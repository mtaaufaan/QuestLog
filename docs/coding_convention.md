# QuestLog — Coding Convention

> Dokumen ini berlaku untuk semua kontributor QuestLog.  
> Tujuannya bukan menggantikan [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html),
> melainkan menetapkan keputusan **QuestLog-specific** yang tidak dicakup standar umum.  
> Jika ada konflik, dokumen ini menang atas preferensi personal.

---

## 1. Package Structure

Gunakan **feature-based** package, bukan layer-based.

```
com.rds.questlog/
├── di/                         # Hilt modules saja
│   ├── RepositoryModule.kt
│   └── UseCaseModule.kt
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── QuestLogDatabase.kt
│   ├── repository/             # implementasi interface domain
│   └── worker/
│       └── ScrapeArticleWorker.kt
├── domain/
│   ├── model/                  # pure Kotlin data class
│   ├── repository/             # interface saja
│   ├── usecase/
│   │   ├── article/
│   │   ├── game/
│   │   └── preferences/
│   └── error/
│       └── QuestLogError.kt
├── presentation/
│   ├── articlelist/
│   │   ├── ArticleListScreen.kt
│   │   ├── ArticleListViewModel.kt
│   │   └── ArticleListUiState.kt
│   ├── addarticle/
│   │   ├── AddArticleScreen.kt
│   │   ├── AddArticleViewModel.kt
│   │   └── AddArticleUiState.kt
│   ├── reader/
│   │   ├── ReaderScreen.kt
│   │   ├── ReaderViewModel.kt
│   │   ├── ReaderUiState.kt
│   │   └── DisplaySettingsSheet.kt
│   ├── navigation/
│   │   └── Routes.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── MainActivity.kt
```

**Aturan:**
- Setiap "feature" folder = satu screen (atau satu sheet yang kompleks).
- `domain/` tidak boleh import apapun dari `data/` atau `presentation/`.
- `data/` hanya boleh import dari `domain/`.

---

## 2. File Organization

### Satu class utama per file

| Boleh digabung dalam 1 file | Harus file terpisah |
|-----------------------------|---------------------|
| Sealed class + subclass-nya | Screen + ViewModel |
| Data class kecil + companion | UseCase yang berbeda |
| Extension functions tematik | Entity + DAO |

**Contoh benar:**
```kotlin
// ReaderUiState.kt — sealed class + semua subclassnya boleh 1 file
sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(...) : ReaderUiState()
    data class Error(...) : ReaderUiState()
}
```

```kotlin
// ReaderScreen.kt — screen composable saja
// ReaderViewModel.kt — ViewModel saja (file terpisah)
```

---

## 3. Naming Convention

### 3.1 UseCase

Format: **`[Verb][Noun]UseCase`**

```kotlin
GetArticleListUseCase   // ✅ — ambil data, return Flow
SaveArticleUseCase      // ✅ — write operation
DeleteArticleUseCase    // ✅
CanAddArticleUseCase    // ✅ — boolean guard, return Flow<Boolean>

ArticleUseCase          // ❌ — terlalu umum
FetchArticles           // ❌ — hilangkan "UseCase" suffix
```

Setiap UseCase = 1 file, 1 class, 1 public function (`operator fun invoke`).

### 3.2 Repository Interface vs Implementation

```kotlin
// domain/repository/ArticleRepository.kt
interface ArticleRepository { ... }

// data/repository/ArticleRepositoryImpl.kt
class ArticleRepositoryImpl @Inject constructor(...) : ArticleRepository { ... }
```

Selalu tambah `Impl` suffix di implementation. Jangan rename interface.

### 3.3 ViewModel

Format: **`[FeatureName]ViewModel`** — sama persis dengan screen-nya.

```kotlin
ArticleListViewModel    // untuk ArticleListScreen
AddArticleViewModel     // untuk AddArticleScreen
ReaderViewModel         // untuk ReaderScreen
```

### 3.4 Composable

- **Screen-level**: `[FeatureName]Screen` — menerima ViewModel, tidak boleh dipakai di tempat lain
- **Reusable component**: nama deskriptif tanpa "Screen" suffix

```kotlin
@Composable
fun ReaderScreen(viewModel: ReaderViewModel = hiltViewModel()) { ... }  // ✅ screen

@Composable
fun ContentNodeItem(node: ContentNodeUiModel, modifier: Modifier = Modifier) { ... }  // ✅ component

@Composable
fun ReaderScreenItem(...) { ... }  // ❌ — "Screen" di component membingungkan
```

### 3.5 Room Entity vs Domain Model

```kotlin
// data/local/entity/ArticleEntity.kt  — suffix "Entity"
@Entity(tableName = "articles")
data class ArticleEntity(...)

// domain/model/Article.kt  — tanpa suffix
data class Article(...)
```

Jangan pernah expose `Entity` ke domain atau presentation layer.

### 3.6 UiState

Format: **`[FeatureName]UiState`**

```kotlin
data class ArticleListUiState(...)   // flat data class — untuk screen yang jarang error
sealed class ReaderUiState { ... }   // sealed — untuk screen dengan Loading/Error state berbeda
```

Lihat `error_state_model.md` untuk pattern mana yang dipakai per screen.

---

## 4. StateFlow & Compose Pattern

### Collect di screen, bukan di composable child

```kotlin
// ✅ Benar — collect sekali di screen level
@Composable
fun ArticleListScreen(viewModel: ArticleListViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArticleListContent(uiState = uiState, onArticleClick = viewModel::onArticleClick)
}

// ❌ Salah — jangan collect di dalam child composable
@Composable
fun ArticleListContent(viewModel: ArticleListViewModel) {   // ← jangan pass ViewModel ke sini
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

### StateFlow untuk state, SharedFlow untuk event

```kotlin
// ViewModel
private val _uiState = MutableStateFlow(ArticleListUiState())
val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

// Gunakan StateFlow — bukan LiveData, bukan SharedFlow untuk UI state
```

### Snackbar: pakai one-shot pattern

Selalu gunakan `SnackbarMessage` dengan `id` untuk dedupe. Lihat `error_state_model.md`.  
**Jangan** update state berulang dari `LaunchedEffect` tanpa consume dulu.

---

## 5. Dependency Injection

### Selalu inject interface, bukan implementation

```kotlin
// ✅
class GetArticleListUseCase @Inject constructor(
    private val repository: ArticleRepository   // interface
)

// ❌
class GetArticleListUseCase @Inject constructor(
    private val repository: ArticleRepositoryImpl   // konkret
)
```

### Modul Hilt: `@Binds` bukan `@Provides` untuk interface binding

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindArticleRepository(
        impl: ArticleRepositoryImpl
    ): ArticleRepository
}
```

Gunakan `@Provides` hanya untuk third-party class (OkHttpClient, Room, DataStore) yang tidak bisa di-annotate `@Inject constructor`.

---

## 6. Error Handling

Semua error handling mengikuti hierarki di `error_state_model.md`.

**Layer responsibility — tidak boleh dilanggar:**

| Layer | Boleh | Tidak boleh |
|---|---|---|
| `data/` | throw `IOException`, `SQLiteException`, dll | catch error domain |
| `domain/UseCase` | wrap ke `QuestLogError`, return `Result<T>` atau sealed | throw ke atas |
| `presentation/ViewModel` | catch, update `uiState` | show Toast/Snackbar langsung |
| `presentation/Screen` | render error dari `uiState` | catch exception sendiri |

**Jangan gunakan `try-catch` di Composable.**

---

## 7. Komentar Wajib

Tulis KDoc comment (`/** ... */`) di:

```kotlin
/**
 * [WAJIB] Setiap UseCase public function (invoke)
 * Jelaskan: pre-condition, apa yang dikembalikan, kapan error terjadi.
 */
suspend operator fun invoke(gameId: Long, urls: List<String>): Result<Long>

/**
 * [WAJIB] Setiap Repository interface — setiap function
 */
fun getArticlesByGame(gameId: Long): Flow<List<Article>>
```

Tidak wajib (boleh tapi tidak diwajibkan):
- Private function yang namanya sudah self-explanatory
- Composable screen-level (screen sudah jelas dari nama)
- Data class property

**Jangan tulis komentar yang hanya mengulang nama:**
```kotlin
// Mendapatkan artikel ← ❌ tidak berguna
fun getArticle(id: Long): Article
```

---

## 8. Git Convention

### Branch naming

```
feature/article-list-screen
feature/scraping-worker
fix/snackbar-dedupe-bug
chore/upgrade-room-2.7
refactor/reader-viewmodel
```

Format: `[type]/[kebab-case-deskripsi]`

Types: `feature`, `fix`, `chore`, `refactor`, `docs`

### Commit message

```
feat: tambah ArticleListScreen dengan pull-to-refresh
fix: snackbar muncul dua kali saat rotasi layar
refactor: pisahkan ScrapeWorker dari RepositoryImpl
chore: upgrade Navigation Compose ke 2.8.9
```

Format: `[type]: [kalimat aktif, lowercase, tanpa titik]`

Types sama dengan branch. Bahasa: **Indonesia atau Inggris, pilih satu dan konsisten per PR.**

### PR Rules

- Minimum 1 reviewer sebelum merge ke `main`
- PR yang menyentuh `domain/` wajib di-review bersama (karena impact ke semua layer)
- Jangan merge PR dengan konflik unresolved
- Squash merge untuk feature branch; merge commit untuk hotfix

---

## 9. Resource Naming

### String

```xml
<!-- Format: [screen]_[elemen]_[state] -->
<string name="article_list_empty_title">Belum ada artikel</string>
<string name="article_list_empty_body">Tambah artikel pertamamu</string>
<string name="reader_error_load_failed">Gagal memuat artikel</string>
<string name="add_article_error_invalid_url">URL tidak valid</string>

<!-- Error string dari QuestLogError — lihat error_state_model.md -->
<string name="error_network_connection_failed">Tidak dapat terhubung ke internet</string>
<string name="error_tier_article_limit">Batas 5 artikel per game tercapai. Upgrade ke Premium.</string>
```

**Jangan hardcode string UI di Kotlin/Composable.** Semua teks user-facing wajib di `strings.xml`.

### Drawable / Icon

```
ic_[nama].xml          — icon (vector drawable)
bg_[nama].xml          — background shape
img_[nama].png         — raster image (hindari kalau bisa)
```

---

*Terakhir diperbarui: September 2026*  
*Reviewer: diskusikan perubahan convention di PR tersendiri dengan label `convention`*
