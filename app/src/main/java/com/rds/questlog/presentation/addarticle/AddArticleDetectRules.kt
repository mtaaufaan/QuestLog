package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Status "Cari halaman lanjutan" di S2 (QL-34, premium). */
sealed interface DetectState {
    data object Idle : DetectState

    /** Sedang memeriksa dari URL pertama. */
    data object Loading : DetectState

    /** Hasil berupa daftar URL bercentang; [selected] ≤ sisa slot. */
    data class Found(val urls: List<String>, val selected: Set<String>) : DetectState

    /** Selesai tetapi tidak ada halaman lanjutan. */
    data object NoResults : DetectState

    /** Pemeriksaan gagal (mis. jaringan); URL yang sudah ada tidak berubah. */
    data object Failed : DetectState

    /** [count] halaman lanjutan baru saja ditambahkan ke daftar URL. */
    data class Added(val count: Int) : DetectState
}

/** Permintaan pemeriksaan: [base] = URL pertama, [slots] = sisa slot, [taken] = URL yang sudah ada (form, artikel). */
class DetectRequest(val base: String, val slots: Int, val taken: Set<String>)

/** Hasil pemeriksaan dari sumber: usulan URL, gagal (tidak menghalangi simpan manual), atau fitur terkunci. */
sealed interface DetectOutcome {
    data class Urls(val urls: List<String>) : DetectOutcome
    data object Failed : DetectOutcome

    /** Gratis atau premium hilang; UI membuka Unlock Sheet. */
    data object Locked : DetectOutcome
}

private val VALID_BASE = Regex("^https://\\S+\\.\\S+")
private val NUMBERED = Regex("^(.*?[?&/](?:page[=/])?)(\\d+)(/?)$", RegexOption.IGNORE_CASE)
private val TRAILING_NUMBER = Regex("^(.*?)(\\d+)(/?)$")
private const val DUMMY_RESULTS = 6
private const val DUMMY_DELAY_MS = 900L

/** URL pertama yang valid (`https://…`) di [urls]; sumber pemeriksaan halaman lanjutan. */
fun detectBaseUrl(urls: List<String>): String? = urls.map { it.trim() }.firstOrNull { VALID_BASE.matches(it) }

/** Sisa slot dari [MAX_URLS] setelah URL yang sudah terisi di [form]. */
fun detectSlots(form: AddArticleForm): Int = (MAX_URLS - form.filledUrlCount).coerceAtLeast(0)

/** Tombol aktif bila ada URL valid, masih ada slot, dan tidak sedang memeriksa (gratis tetap bisa diketuk: Unlock). */
fun canDetect(premium: Boolean, base: String?, slots: Int, state: DetectState): Boolean =
    !premium || (base != null && slots > 0 && state != DetectState.Loading)

/** Hasil pencarian awal: semua URL baru tercentang sampai sisa slot habis. */
fun foundState(found: List<String>, known: Set<String>, slots: Int): DetectState {
    val fresh = found.filter { it !in known }.distinct()
    return if (fresh.isEmpty()) DetectState.NoResults else DetectState.Found(fresh, fresh.take(slots).toSet())
}

/** Mencentang/menghapus centang [url]; mencentang melebihi [slots] diabaikan. */
fun toggleDetected(state: DetectState.Found, url: String, slots: Int): DetectState.Found = when {
    url in state.selected -> state.copy(selected = state.selected - url)
    state.selected.size >= slots -> state
    else -> state.copy(selected = state.selected + url)
}

/** Menambahkan URL tercentang (urutan hasil) ke akhir daftar form; baris kosong diganti, maksimal [MAX_URLS]. */
fun applyDetected(form: AddArticleForm, state: DetectState.Found): AddArticleForm {
    val current = form.urls.filter { it.isNotBlank() }
    val added = state.urls.filter { it in state.selected && it !in current }
    return form.copy(urls = (current + added).take(MAX_URLS).ifEmpty { listOf("") })
}

/**
 * Sumber dummy Tahap 1 (tanpa jaringan): menaikkan angka di ujung [base] (mis. `?page=1` → `?page=2…`), maksimal
 * [DUMMY_RESULTS]; URL tanpa angka di ujung tidak menghasilkan apa pun.
 */
fun dummyContinuationUrls(base: String): List<String> {
    val match = NUMBERED.find(base) ?: TRAILING_NUMBER.find(base) ?: return emptyList()
    val (prefix, number, suffix) = match.destructured
    val start = number.toLong()
    return (1..DUMMY_RESULTS).map { prefix + (start + it) + suffix }
}

/** Sumber dummy untuk pratinjau: menunggu sebentar lalu memberi hasil dummy (aplikasi memakai `DetectViewModel`). */
suspend fun dummyDetect(request: DetectRequest): DetectOutcome {
    delay(DUMMY_DELAY_MS)
    return DetectOutcome.Urls(dummyContinuationUrls(request.base))
}

/** Pemegang state pencarian: menjalankan [source] di [scope] dan menyimpan hasilnya sebagai [DetectState]. */
@Stable
class Detector(
    private val scope: CoroutineScope,
    private val source: suspend (DetectRequest) -> DetectOutcome,
    private val onLocked: () -> Unit,
) {
    var state by mutableStateOf<DetectState>(DetectState.Idle)
        private set

    fun search(base: String, slots: Int, known: Set<String>) {
        if (state == DetectState.Loading) return
        state = DetectState.Loading
        scope.launch {
            state = when (val outcome = source(DetectRequest(base, slots, known))) {
                is DetectOutcome.Urls -> foundState(outcome.urls, known, slots)
                DetectOutcome.Failed -> DetectState.Failed
                DetectOutcome.Locked -> {
                    onLocked()
                    DetectState.Idle
                }
            }
        }
    }

    fun toggle(url: String, slots: Int) {
        (state as? DetectState.Found)?.let { state = toggleDetected(it, url, slots) }
    }

    fun dismiss() {
        state = DetectState.Idle
    }

    /** Menandai hasil sudah ditambahkan ([count] halaman). */
    fun applied(count: Int) {
        state = DetectState.Added(count)
    }
}

@Composable
fun rememberDetector(source: suspend (DetectRequest) -> DetectOutcome, onLocked: () -> Unit): Detector {
    val scope = rememberCoroutineScope()
    val currentOnLocked by rememberUpdatedState(onLocked)
    return remember(scope, source) { Detector(scope, source) { currentOnLocked() } }
}

/** Aksi pengguna di bagian "Halaman lanjutan". */
class DetectActions(
    val onDetect: () -> Unit,
    val onToggle: (String) -> Unit,
    val onApply: () -> Unit,
    val onDismiss: () -> Unit,
    val onOpenUnlock: () -> Unit,
)
