package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.rds.questlog.presentation.model.ScrapeMode

const val MAX_URLS = 10
const val MAX_GAME_NAME = 100

private const val NO_ID = -1L
private val URL_SEPARATOR = Regex("[\\s,]+")
private val URL_SCHEME = Regex("https?://")
private val HTTPS_URL = Regex("https://[^\\s,]+")

/** Isi form S2 beserta operasinya; semuanya murni (tanpa Compose) agar mudah diuji. */
data class AddArticleForm(
    val mode: ScrapeMode = ScrapeMode.NEW,
    val gameInput: String = "",
    /** Game yang dipilih dari daftar; null bila belum dipilih atau [newGame]. */
    val gameId: Long? = null,
    val newGame: Boolean = false,
    val title: String = "",
    val urls: List<String> = listOf(""),
    val targetId: Long? = null,
    /** Error yang baru tampil setelah user mencoba menyimpan (kosong, game, judul, target). */
    val showErrors: Boolean = false,
    val saving: Boolean = false,
) {
    fun withMode(newMode: ScrapeMode) = copy(mode = newMode, showErrors = false)

    /** Mengetik di field game membatalkan pilihan game sebelumnya. */
    fun withGameInput(text: String) = copy(gameInput = text, gameId = null, newGame = false)

    fun withPickedGame(id: Long, name: String) = copy(gameId = id, gameInput = name, newGame = false)

    fun withNewGame() = copy(newGame = true, gameInput = normalizeGameName(gameInput))

    fun withTitle(text: String) = copy(title = text)

    fun withTarget(id: Long) = copy(targetId = id)

    /**
     * Mengubah baris URL ke-[index]. Teks yang berisi beberapa URL (dipisah spasi, baris, atau koma) dari paste
     * dipecah menjadi beberapa baris, maksimal [MAX_URLS] baris.
     */
    fun withUrlChanged(index: Int, text: String): AddArticleForm {
        val parts = text.split(URL_SEPARATOR).filter { it.isNotEmpty() }
        // Hanya lonjakan teks (paste); mengetik koma/spasi satu per satu tidak memecah baris.
        val pasted = text.length - urls[index].length > 1
        if (pasted && parts.size > 1 && URL_SCHEME.containsMatchIn(text)) {
            return copy(urls = (urls.take(index) + parts + urls.drop(index + 1)).take(MAX_URLS))
        }
        return copy(urls = urls.mapIndexed { i, url -> if (i == index) text else url })
    }

    fun withUrlAdded() = if (urls.size >= MAX_URLS) this else copy(urls = urls + "")

    fun withUrlRemoved(index: Int) = copy(
        urls = if (urls.size > 1) urls.filterIndexed { i, _ -> i != index } else listOf(""),
    )

    /** Menukar baris [index] dengan tetangganya ([delta] = -1 naik, +1 turun); di luar batas diabaikan. */
    fun withUrlMoved(index: Int, delta: Int): AddArticleForm {
        val target = index + delta
        if (index !in urls.indices || target !in urls.indices) return this
        val moved = urls.toMutableList()
        moved[index] = urls[target]
        moved[target] = urls[index]
        return copy(urls = moved)
    }

    val filledUrlCount: Int get() = urls.count { it.isNotBlank() }

    /** URL dari "Bagikan" menggantikan seluruh daftar URL (Hal. 1); game dan judul tidak berubah. */
    fun withSharedUrl(url: String) = copy(urls = listOf(url), showErrors = false)

    /**
     * Menambahkan URL `https://` dari [clipboard] ke akhir daftar (QL-22): URL yang sudah ada dilewati, hasilnya
     * maksimal [MAX_URLS], dan baris kosong diganti. Mengembalikan form baru beserta hasilnya; form tidak berubah
     * bila clipboard tanpa URL valid ([PasteOutcome.NoUrls]) atau tidak ada yang baru ([PasteOutcome.NothingNew]).
     */
    fun withPasted(clipboard: String?): Pair<AddArticleForm, PasteOutcome> {
        val found = HTTPS_URL.findAll(clipboard.orEmpty()).map { it.value }.distinct().toList()
        val current = urls.filter { it.isNotBlank() }
        val merged = (current + found.filter { it !in current }).take(MAX_URLS)
        val added = merged.size - current.size
        return when {
            found.isEmpty() -> this to PasteOutcome.NoUrls
            added == 0 -> this to PasteOutcome.NothingNew
            else -> copy(urls = merged) to PasteOutcome.Added(added)
        }
    }
}

/** Hasil mengetuk Tempel: pesan yang ditampilkan di bawah judul bagian URL. */
sealed interface PasteOutcome {
    /** [count] URL baru ditambahkan. */
    data class Added(val count: Int) : PasteOutcome

    /** Clipboard kosong atau tidak berisi URL `https://` yang valid. */
    data object NoUrls : PasteOutcome

    /** Semua URL di clipboard sudah ada di daftar, atau daftar sudah penuh. */
    data object NothingNew : PasteOutcome
}

/** Menyimpan form saat rotasi layar/proses dimatikan; status `saving` sengaja tidak dipulihkan. */
val AddArticleFormSaver: Saver<AddArticleForm, Any> = listSaver(
    save = {
        listOf(
            it.mode.name,
            it.gameInput,
            it.gameId ?: NO_ID,
            it.newGame,
            it.title,
            ArrayList(it.urls),
            it.targetId ?: NO_ID,
            it.showErrors,
        )
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        AddArticleForm(
            mode = ScrapeMode.valueOf(it[0] as String),
            gameInput = it[1] as String,
            gameId = (it[2] as Long).takeIf { id -> id != NO_ID },
            newGame = it[3] as Boolean,
            title = it[4] as String,
            urls = it[5] as List<String>,
            targetId = (it[6] as Long).takeIf { id -> id != NO_ID },
            showErrors = it[7] as Boolean,
        )
    },
)
