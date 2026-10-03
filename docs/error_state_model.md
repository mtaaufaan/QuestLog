# Error State Model — QuestLog

**Status**: Draft v1.0  
**Dibuat oleh**: Architect (Claude)  
**Scope**: Hierarki error class, UiState per screen, dan pola penanganan error dari domain hingga UI.

---

## 1. Error Flow Overview

```
Data Layer                    Domain Layer              Presentation Layer
──────────────────────────    ──────────────────────    ──────────────────────────
RoomDAO throws                UseCase catch &           ViewModel map ke
IOException         ───────►  wrap ke               ───► UiState.error
OkHttp IOException            QuestLogError             Composable render:
BillingClient fail            (sealed class)            • Snackbar (transient)
                                                        • Inline text (form)
                                                        • Dialog (confirmation)
                                                        • Full-screen (fatal)
```

**Aturan dasar:**
- Data layer: boleh throw exception native (IOException, SQLiteException, dll.)
- Domain layer (UseCase): wajib catch dan wrap ke `QuestLogError`
- ViewModel: terima `QuestLogError`, petakan ke string message atau UiState spesifik
- Composable: hanya tahu soal UiState — tidak pernah menerima exception langsung

---

## 2. Error Class Hierarchy

```kotlin
// domain/error/QuestLogError.kt

sealed class QuestLogError(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    // ── Network (terjadi saat scraping) ──────────────────────────────────
    sealed class NetworkError : QuestLogError() {
        /** Tidak bisa reach URL — tidak ada koneksi atau DNS gagal. */
        data class ConnectionFailed(
            val url: String,
            override val cause: Throwable? = null
        ) : NetworkError()

        /** Request timeout. */
        data class Timeout(val url: String) : NetworkError()

        /** Server merespons dengan HTTP error code. */
        data class HttpError(
            val url: String,
            val code: Int          // e.g. 404, 403, 500
        ) : NetworkError()
    }

    // ── Parsing (terjadi saat memproses HTML hasil scrape) ───────────────
    sealed class ParseError : QuestLogError() {
        /** Struktur HTML tidak dikenal — selector tidak menemukan konten. */
        data class StructureUnrecognized(val url: String) : ParseError()

        /** Halaman berhasil di-fetch tapi tidak menghasilkan content_node apapun. */
        data class EmptyContent(val url: String) : ParseError()
    }

    // ── Database (Room operation failure) ────────────────────────────────
    sealed class DatabaseError : QuestLogError() {
        object ReadFailed : DatabaseError()
        object WriteFailed : DatabaseError()
    }

    // ── Validation (input form dari user) ────────────────────────────────
    sealed class ValidationError : QuestLogError() {
        object InvalidUrl : ValidationError()       // bukan format https://
        object EmptyGameName : ValidationError()    // nama game kosong
        object GameNameTooLong : ValidationError()  // > 100 karakter
        object EmptyTitle : ValidationError()       // judul artikel kosong
        object TooManyUrls : ValidationError()      // > 10 URL
        object NoUrlsProvided : ValidationError()   // tidak ada URL sama sekali
    }

    // ── Free tier limit ──────────────────────────────────────────────────
    sealed class TierError : QuestLogError() {
        object GameLimitReached : TierError()       // sudah 2 game (free)
        object ArticleLimitReached : TierError()    // sudah 5 artikel/game (free)
    }

    // ── Google Play Billing ──────────────────────────────────────────────
    sealed class BillingError : QuestLogError() {
        object ServiceUnavailable : BillingError()  // BillingClient tidak terhubung
        object PurchaseNotFound : BillingError()    // tidak ada purchase aktif
    }
}
```

---

## 3. UiState per Screen

### 3.1 ArticleListScreen — `ArticleListUiState`

Screen ini berbasis `Flow<List<Article>>` dari Room — data selalu tersedia meski offline.  
Tidak ada full-screen error state; error ditampilkan sebagai snackbar atau inline per item.

```kotlin
// presentation/articlelist/ArticleListUiState.kt

data class ArticleListUiState(
    val articles: List<ArticleUiModel> = emptyList(),
    val gameFilterOptions: List<String> = emptyList(),  // ["Semua Game", "Elden Ring", …]
    val selectedGameFilter: String? = null,             // null = "Semua Game"
    val isLoading: Boolean = true,
    val snackbar: SnackbarMessage? = null               // transient message
)

data class ArticleUiModel(
    val id: Long,
    val title: String,
    val gameName: String,
    val status: ArticleStatus,
    val scrapedPageCount: Int,
    val totalPageCount: Int,
    val lastReadAt: Long?
)

enum class ArticleStatus {
    SCRAPING,  // → dot animasi + "Memproses… X/Y"
    READY,     // → normal, bisa di-tap
    ERROR      // → dot merah + "Gagal — tap untuk retry"
}

data class SnackbarMessage(
    val text: String,
    val actionLabel: String? = null,
    val id: Long = System.currentTimeMillis()  // untuk deduplikasi
)
```

#### Error → UI mapping di S1

| Error / event | Tampilan UI |
|---------------|-------------|
| Artikel status `ERROR` | Dot merah inline + teks "Gagal — tap untuk retry" |
| `DeleteArticleUseCase` gagal | Snackbar: "Gagal menghapus artikel. Coba lagi." |
| Room Flow emit exception | Snackbar: "Terjadi kesalahan membaca data." (jarang terjadi) |

---

### 3.2 AddArticleScreen — `AddArticleUiState`

Screen ini punya dua jenis error: **validasi form** (inline, sync) dan **hasil save** (transient/blocking).

```kotlin
// presentation/addarticle/AddArticleUiState.kt

data class AddArticleUiState(
    // Form state
    val games: List<GameUiModel> = emptyList(),
    val selectedGameId: Long? = null,
    val isCreatingNewGame: Boolean = false,
    val newGameName: String = "",
    val urls: List<String> = listOf(""),        // min 1, max 10

    // Capability flags (reaktif dari CanAdd*UseCase)
    val canAddGame: Boolean = true,
    val canAddArticle: Boolean = true,          // false jika limit artikel tercapai

    // Validation errors (inline, tampil di bawah field)
    val formErrors: AddArticleFormErrors = AddArticleFormErrors(),

    // Save state
    val isSaving: Boolean = false,
    val saveEvent: SaveEvent? = null            // one-shot event ke ViewModel
)

data class AddArticleFormErrors(
    val gameNameError: String? = null,          // tampil di bawah field nama game
    val urlErrors: Map<Int, String> = emptyMap(), // index → pesan error per URL
    val generalError: String? = null
)

sealed class SaveEvent {
    /** Artikel berhasil disimpan dan WorkManager job sudah di-enqueue. */
    data class Success(val articleId: Long) : SaveEvent()

    /** Snackbar — error sementara, user bisa retry. */
    data class Failure(val message: String) : SaveEvent()
}

data class GameUiModel(
    val id: Long,
    val name: String
)
```

#### Error → UI mapping di S2

| `QuestLogError` | Tampilan UI | Lokasi |
|-----------------|-------------|--------|
| `ValidationError.InvalidUrl` | "URL tidak valid — harus diawali https://" | Di bawah field URL |
| `ValidationError.NoUrlsProvided` | "Tambahkan minimal 1 URL" | Di bawah field URL pertama |
| `ValidationError.TooManyUrls` | "Maksimal 10 URL per artikel" | Di bawah field URL terakhir |
| `ValidationError.EmptyGameName` | "Nama game tidak boleh kosong" | Di bawah field nama game |
| `ValidationError.GameNameTooLong` | "Nama game maksimal 100 karakter" | Di bawah field nama game |
| `TierError.GameLimitReached` | Tombol "Buat Game Baru" disabled + "Limit gratis: maks. 2 game" | Inline di dropdown |
| `TierError.ArticleLimitReached` | Tombol "Simpan" disabled + "Limit gratis: maks. 5 artikel per game" | Di atas tombol Simpan |
| `DatabaseError.WriteFailed` | Snackbar: "Gagal menyimpan. Coba lagi." | Transient |

#### Validasi form — aturan

Validasi dilakukan di ViewModel **sebelum** memanggil UseCase (client-side validation):

```kotlin
// AddArticleViewModel.kt (contoh logika validasi)
fun validateAndSave() {
    val errors = mutableMapOf<String, String?>()

    if (uiState.isCreatingNewGame && uiState.newGameName.isBlank()) {
        errors["gameName"] = "Nama game tidak boleh kosong"
    }
    if (uiState.newGameName.length > 100) {
        errors["gameName"] = "Nama game maksimal 100 karakter"
    }

    val urlErrors = uiState.urls.mapIndexed { i, url ->
        i to when {
            url.isBlank() -> "URL tidak boleh kosong"
            !url.startsWith("https://") -> "URL harus diawali https://"
            else -> null
        }
    }.filter { it.second != null }.toMap()

    if (errors.isEmpty() && urlErrors.isEmpty()) {
        // Lanjut panggil UseCase
        save()
    } else {
        _uiState.update { it.copy(formErrors = AddArticleFormErrors(
            gameNameError = errors["gameName"],
            urlErrors = urlErrors
        ))}
    }
}
```

---

### 3.3 ReaderScreen — `ReaderUiState`

Screen ini memiliki **full-screen error state** karena tidak ada fallback konten jika artikel gagal dimuat.

```kotlin
// presentation/reader/ReaderUiState.kt

sealed class ReaderUiState {

    /** Data sedang dimuat dari Room. */
    object Loading : ReaderUiState()

    /** Konten siap ditampilkan. */
    data class Success(
        val article: ArticleUiModel,
        val contentNodes: List<ContentNodeUiModel>,
        val lastVisitedNodeId: Long?,
        val displayPrefs: DisplayPreferences,
        val snackbar: SnackbarMessage? = null     // untuk error non-fatal
    ) : ReaderUiState()

    /** Artikel tidak bisa dimuat sama sekali. */
    data class Error(
        val error: ReaderError,
        val articleId: Long                       // untuk tombol retry
    ) : ReaderUiState()
}

sealed class ReaderError {
    object ArticleNotFound : ReaderError()        // artikel dihapus dari DB
    object ContentEmpty : ReaderError()           // scraping selesai tapi 0 node
    object DatabaseReadFailed : ReaderError()     // Room exception
}

data class ContentNodeUiModel(
    val id: Long,
    val type: ContentType,
    val content: String,
    val displayOrder: Int
)
```

#### Error → UI mapping di S3

| `ReaderError` | Tampilan UI | Tindakan user |
|---------------|-------------|---------------|
| `ArticleNotFound` | Full-screen: ikon 📄 + "Artikel tidak ditemukan" | Tombol "Kembali" |
| `ContentEmpty` | Full-screen: ikon 📭 + "Konten tidak tersedia. Coba scraping ulang dari daftar artikel." | Tombol "Kembali" |
| `DatabaseReadFailed` | Full-screen: ikon ⚠️ + "Gagal membaca data. Coba lagi." | Tombol "Coba Lagi" → retry |
| Save position gagal | Snackbar singkat: "Gagal menyimpan posisi baca." (tidak fatal) | — |
| Set font size gagal | Snackbar: "Gagal menyimpan preferensi." | — |

---

## 4. SnackbarMessage — Pola One-Shot

Snackbar adalah event, bukan state permanen. Harus di-consume setelah ditampilkan.

```kotlin
// Pola di ViewModel:
fun onSnackbarShown() {
    _uiState.update { it.copy(snackbar = null) }
}

// Di Composable:
val snackbarHostState = remember { SnackbarHostState() }
val snackbar = uiState.snackbar

LaunchedEffect(snackbar) {
    snackbar?.let {
        snackbarHostState.showSnackbar(
            message = it.text,
            actionLabel = it.actionLabel
        )
        viewModel.onSnackbarShown()
    }
}
```

---

## 5. Dialog Errors (Confirmation Required)

Dialog digunakan untuk aksi destruktif yang butuh konfirmasi user.

```kotlin
// Contoh: dialog hapus artikel di S1
data class DialogState(
    val title: String,
    val message: String,
    val confirmLabel: String = "Hapus",
    val dismissLabel: String = "Batal",
    val onConfirm: () -> Unit,
    val isDangerous: Boolean = false
)
```

| Trigger | Dialog |
|---------|--------|
| Long-tap artikel → Hapus | "Hapus artikel ini? Semua konten yang sudah diunduh akan ikut terhapus." |
| Hapus game (jika ada) | "Hapus game ini? Semua artikel di dalamnya akan ikut terhapus." |

---

## 6. ScrapeArticleWorker — Error Handling

Worker berjalan di background; error-nya tidak bisa ditampilkan langsung di UI.  
Error worker dicatat di DB dan tampil sebagai status `ERROR` di daftar artikel.

```kotlin
// Mapping error di ScrapeArticleWorker:

override suspend fun doWork(): Result {
    return try {
        // ... scraping logic ...
        articleRepository.markScrapingDone(articleId)
        Result.success()
    } catch (e: QuestLogError.NetworkError.ConnectionFailed) {
        articleRepository.updateScrapingStatus(articleId, ScrapingStatus.ERROR)
        // Simpan pesan error ke DB untuk ditampilkan di UI (opsional)
        Result.failure(workDataOf("error" to "Tidak bisa menjangkau ${e.url}"))
    } catch (e: QuestLogError.NetworkError.Timeout) {
        articleRepository.updateScrapingStatus(articleId, ScrapingStatus.ERROR)
        Result.retry()   // WorkManager akan coba ulang
    } catch (e: QuestLogError.ParseError) {
        articleRepository.updateScrapingStatus(articleId, ScrapingStatus.ERROR)
        Result.failure(workDataOf("error" to "Konten tidak dikenali di ${e.url}"))
    } catch (e: Exception) {
        articleRepository.updateScrapingStatus(articleId, ScrapingStatus.ERROR)
        Result.failure()
    }
}
```

**Retry policy untuk WorkManager:**

```kotlin
val workRequest = OneTimeWorkRequestBuilder<ScrapeArticleWorker>()
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()
```

---

## 7. Error Message Strings

Semua string error disimpan di `res/values/strings.xml` (bukan hardcoded di Kotlin):

```xml
<!-- res/values/strings.xml -->

<!-- Validasi form -->
<string name="error_url_invalid">URL tidak valid — harus diawali https://</string>
<string name="error_url_empty">URL tidak boleh kosong</string>
<string name="error_urls_too_many">Maksimal 10 URL per artikel</string>
<string name="error_game_name_empty">Nama game tidak boleh kosong</string>
<string name="error_game_name_too_long">Nama game maksimal 100 karakter</string>

<!-- Free tier -->
<string name="error_game_limit">Limit gratis tercapai (maks. 2 game). Upgrade untuk lebih.</string>
<string name="error_article_limit">Limit gratis tercapai (maks. 5 artikel per game). Upgrade untuk lebih.</string>

<!-- Database -->
<string name="error_save_failed">Gagal menyimpan. Coba lagi.</string>
<string name="error_delete_failed">Gagal menghapus artikel. Coba lagi.</string>
<string name="error_read_failed">Gagal membaca data. Coba lagi.</string>

<!-- Reader -->
<string name="error_article_not_found">Artikel tidak ditemukan.</string>
<string name="error_content_empty">Konten tidak tersedia. Coba scraping ulang dari daftar artikel.</string>
<string name="error_position_save_failed">Gagal menyimpan posisi baca.</string>

<!-- Scraping (via notifikasi) -->
<string name="notif_scraping_error">Gagal mengunduh %1$s. Tap untuk retry.</string>
```

---

## 8. Error Handling — Ringkasan per Layer

| Layer | Tanggung jawab | Boleh throw? |
|-------|---------------|--------------|
| **Data** | Operasi Room, network, billing | ✅ Throw exception native |
| **Domain (UseCase)** | Wrap ke `QuestLogError`, enforce business rules | ✅ Throw `QuestLogError` |
| **ViewModel** | Catch semua error, petakan ke UiState | ❌ Tidak boleh throw |
| **Composable** | Render UiState, tidak tahu soal exception | ❌ Tidak boleh catch |

---

## 9. Referensi Dokumen Terkait

| Dokumen | Path |
|---------|------|
| Screen Flow | `docs/screen_flow.md` |
| Domain Contract | `docs/domain_contract.md` |
| Database Schema | `docs/database_schema.md` |
| Tech Stack | `docs/tech-stack.md` |
