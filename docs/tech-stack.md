# Tech Stack — QuestLog

> Dokumen ini merupakan keputusan arsitektur final untuk aplikasi **QuestLog** — Android offline reader untuk walkthrough game (RPG / retro via emulator). Semua keputusan didasarkan pada diskusi antara product owner dan software architect.

---

## 1. Bahasa & Runtime

| Komponen | Pilihan | Alasan |
|---|---|---|
| Bahasa utama | **Kotlin** | First-class di Android, coroutine native, null-safe |
| Minimum SDK | **API 26 (Android 8.0)** | Mendukung WebP lossy, cakupan >95% device aktif |
| Target SDK | **API 35** | Edge-to-edge, Predictive Back |
| Build system | **Gradle KTS** | Type-safe, IDE-friendly, konsisten dengan Kotlin |

---

## 2. UI Framework

**Jetpack Compose** — deklaratif, lifecycle-aware, tidak perlu XML.

```kotlin
// Contoh struktur layar utama
@Composable
fun ReaderScreen(viewModel: ReaderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(uiState.nodes, key = { it.id }) { node ->
            ContentNodeItem(node)
        }
    }
}
```

- **Navigation Compose** untuk navigasi antar layar (type-safe routes)
- **Material 3** sebagai design system

---

## 3. Arsitektur

### Clean Architecture + MVVM

Tiga layer dengan dependensi satu arah: **Presentation → Domain → Data**

```
app/
├── presentation/       # Compose UI, ViewModel
├── domain/             # Use cases, interface, model (pure Kotlin)
└── data/               # Room, OkHttp, parser (implementasi domain)
```

### Prinsip utama

- **Domain layer adalah pure Kotlin** — tidak mengimpor `android.*`, `room.*`, `okhttp3.*`
- Semua dependensi eksternal dideklarasikan sebagai interface di domain, diimplementasikan di data
- ViewModel hanya meng-observe `Flow` dari use case; tidak menyentuh Room atau OkHttp secara langsung

```kotlin
// Domain Layer — hanya interface dan model
interface ArticleRepository {
    suspend fun save(article: Article, nodes: List<ContentNode>)
    fun observeAll(): Flow<List<Article>>
    fun observeNodes(articleId: Long): Flow<List<ContentNode>>
}

interface ScraperEngine {
    suspend fun scrape(url: String): ScrapingResult
}
```

```kotlin
// Data Layer — implementasi konkret
class ArticleRepositoryImpl(
    private val articleDao: ArticleDao,
    private val contentNodeDao: ContentNodeDao
) : ArticleRepository {
    override fun observeAll() = articleDao.observeAll()
    override suspend fun save(article: Article, nodes: List<ContentNode>) {
        articleDao.insert(article.toEntity())
        contentNodeDao.insertAll(nodes.map { it.toEntity() })
    }
}
```

---

## 4. Database — Room

### Skema Relasional

Skema relasional dipilih karena mendukung checkpoint berbasis FK, append parsial, dan query efisien tanpa parsing JSON. Schema lengkap ada di [`database_schema.md`](database_schema.md).

```sql
-- Entitas game
CREATE TABLE games (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    created_at  INTEGER NOT NULL
);

-- Artikel / walkthrough
CREATE TABLE articles (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    game_id          INTEGER NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    title            TEXT    NOT NULL,
    is_scraping_done INTEGER NOT NULL DEFAULT 0,
    -- 0 = WorkManager masih berjalan; 1 = semua source_pages COMPLETED atau FAILED
    created_at       INTEGER NOT NULL
);

-- Halaman sumber (multi-URL per artikel)
CREATE TABLE source_pages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id  INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    source_url  TEXT    NOT NULL,
    page_order  INTEGER NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'PENDING',
    -- PENDING | IN_PROGRESS | COMPLETED | FAILED
    order_start INTEGER,  -- reserved range start (mis. 1, 1001, 2001...)
    order_end   INTEGER   -- reserved range end   (mis. 1000, 2000, 3000...)
);

-- Node konten — unit terkecil untuk checkpoint
CREATE TABLE content_nodes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id      INTEGER NOT NULL REFERENCES articles(id)      ON DELETE CASCADE,
    source_page_id  INTEGER NOT NULL REFERENCES source_pages(id),
    node_type       TEXT    NOT NULL,  -- h1, h2, h3, p, p_cont, img, pre, table, li
    display_order   INTEGER NOT NULL,
    text_content    TEXT,
    metadata_json   TEXT
);

CREATE INDEX idx_content_nodes_article_order ON content_nodes (article_id, display_order);
CREATE INDEX idx_content_nodes_source_page   ON content_nodes (source_page_id);

-- Checkpoint: 1 per artikel
CREATE TABLE checkpoints (
    article_id           INTEGER PRIMARY KEY REFERENCES articles(id) ON DELETE CASCADE,
    anchor_node_id       INTEGER NOT NULL REFERENCES content_nodes(id),
    -- checkpoint MANUAL yang disengaja user
    fallback_order       INTEGER NOT NULL,
    last_visited_node_id INTEGER REFERENCES content_nodes(id),
    -- posisi terakhir AUTO-SAVED (MVP)
    last_read_at         INTEGER,  -- timestamp (schema ready, UI v2)
    read_progress        REAL,     -- 0.0-1.0  (schema ready, UI v2)
    updated_at           INTEGER NOT NULL
);

-- Tracking gambar tersimpan (untuk dedup & cleanup)
CREATE TABLE images (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id  INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    filename    TEXT    NOT NULL,  -- MD5(source_url) + ".webp"
    file_path   TEXT    NOT NULL,
    file_size   INTEGER,
    source_url  TEXT    NOT NULL,
    created_at  INTEGER NOT NULL
);

-- Konfigurasi aplikasi & status premium
CREATE TABLE app_config (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
    -- Rows: is_premium, purchase_token, purchase_verified_at
);
```

### Tipe node (`node_type`)

| Nilai | Deskripsi |
|---|---|
| `h1`, `h2`, `h3` | Heading |
| `p` | Paragraf pendek (≤400 karakter atau ≤3 kalimat) |
| `p_cont` | Kelanjutan paragraf panjang (hasil split otomatis) |
| `img` | Gambar — path WebP lokal di `metadata_json` |
| `pre` | Blok preformatted / ASCII art |
| `table` | Tabel — HTML mentah di `metadata_json` |
| `li` | Item list |

### Granularitas Checkpoint — Paragraph Splitting

Paragraf dengan **>400 karakter DAN >3 kalimat** dipecah saat scraping menjadi node `p_cont` pada batas antar kalimat.

```kotlin
fun splitParagraph(text: String): List<String> {
    val sentences = text.split(Regex("(?<=[.!?])\s+"))
    if (text.length <= 400 || sentences.size <= 3) return listOf(text)

    val chunks = mutableListOf<String>()
    var current = StringBuilder()
    sentences.forEach { sentence ->
        if (current.length + sentence.length > 400 && current.isNotEmpty()) {
            chunks.add(current.toString().trim())
            current = StringBuilder()
        }
        current.append(sentence).append(" ")
    }
    if (current.isNotEmpty()) chunks.add(current.toString().trim())
    return chunks
}
```

### Auto-resume posisi baca

`last_visited_node_id` disimpan otomatis saat user keluar dari layar baca, terpisah dari checkpoint manual (`anchor_node_id`):

```kotlin
// Simpan saat keluar layar
DisposableEffect(Unit) {
    onDispose {
        val nodeId = contentNodes.getOrNull(listState.firstVisibleItemIndex)?.id
        if (nodeId != null) viewModel.saveLastPosition(articleId, nodeId)
    }
}

// Resume saat buka artikel
LaunchedEffect(lastVisitedNodeId) {
    lastVisitedNodeId?.let { nodeId ->
        val index = contentNodes.indexOfFirst { it.id == nodeId }
        if (index >= 0) listState.scrollToItem(index)
    }
}
```

### Checkpoint Fallback

```sql
SELECT id FROM content_nodes
WHERE article_id = :article_id
  AND display_order >= :last_known_order
ORDER BY display_order ASC
LIMIT 1;
```

## 5. Parser Konten — Dual-Engine

Dua engine scraping dipilih berdasarkan jenis sumber:

| Engine | Target | Library |
|---|---|---|
| ASCII / GameFAQs Engine | Plain-text, `<pre>` blocks, GameFAQs | **Ksoup** (Kotlin-native Jsoup port) |
| Rich Wiki Engine | HTML rich (Fandom, IGN, wiki) | **Ksoup** + Readability-style extraction |

Engine dideteksi otomatis dari URL atau struktur halaman.

```kotlin
interface ScraperEngine {
    suspend fun scrape(url: String): ScrapingResult
}

class DualEngineParser(
    private val asciiEngine: AsciiGameFaqsEngine,
    private val richWikiEngine: RichWikiEngine
) {
    fun parse(html: String, url: String): List<ContentNode> {
        return if (isAsciiSource(url, html)) {
            asciiEngine.parse(html)
        } else {
            richWikiEngine.parse(html)
        }
    }
}
```

- **ASCII Engine** mempertahankan `<pre>` tag untuk tabel ASCII dan diagram karakter
- **Rich Engine** mengekstrak konten artikel utama, mengabaikan sidebar/nav/iklan

---

## 6. Jaringan — OkHttp

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .addInterceptor(UserAgentInterceptor())  // menyamar sebagai browser biasa
    .build()
```

**Tidak menggunakan Retrofit** — scraping tidak memerlukan REST API typed; raw HTML response cukup.

---

## 7. Gambar — Kompresi WebP + Coil

### Strategi dua tahap

| Tahap | Library | Fungsi |
|---|---|---|
| **Scraping** | Android `Bitmap` API | Download, resize, compress → WebP, simpan ke `filesDir` |
| **Display** | **Coil** (`AsyncImage`) | Load dari path lokal, cache in-memory |

### Kompresi Adaptif

```kotlin
fun compressAndSave(bitmap: Bitmap, context: Context, hash: String): String {
    val maxDim = 1200
    val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
        val ratio = minOf(maxDim.toFloat() / bitmap.width,
                         maxDim.toFloat() / bitmap.height)
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    } else bitmap

    // Kualitas adaptif: peta/diagram besar butuh detail lebih
    val quality = if (scaled.width > 1000 && scaled.height > 1000) 90 else 75

    val file = File(context.filesDir, "images/$hash.webp")
    file.parentFile?.mkdirs()
    FileOutputStream(file).use { out ->
        scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out)
    }
    return file.absolutePath
}
```

### Penamaan File — MD5 Hash

```kotlin
fun urlToFilename(url: String): String {
    val md5 = MessageDigest.getInstance("MD5")
    val hash = md5.digest(url.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return "$hash.webp"
}
```

MD5 hash menjamin:
- Nama file aman (tanpa karakter spesial)
- Deduplikasi otomatis (URL sama → file sama)
- Tidak perlu tabel pemetaan tambahan

### Display dengan Coil

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(node.localImagePath)  // path absolut dari filesDir
        .crossfade(true)
        .build(),
    contentDescription = node.altText,
    modifier = Modifier.fillMaxWidth()
)
```

---

## 8. Background Scraping — WorkManager

WorkManager dipilih agar proses scraping **berlanjut walau aplikasi di-minimize**.

```kotlin
class ScrapingWorker(
    context: Context,
    params: WorkerParameters,
    private val scrapeArticleUseCase: ScrapeArticleUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val urls = inputData.getStringArray(KEY_URLS) ?: return Result.failure()
        val gameId = inputData.getLong(KEY_GAME_ID, -1)

        return try {
            scrapeArticleUseCase.execute(gameId, urls.toList())
                .collect { progress ->
                    setProgress(workDataOf(KEY_PROGRESS to progress.current))
                }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

### Scraping Sequential (bukan parallel)

URL di-scrape satu per satu untuk menghindari rate-limiting dari situs target.

```kotlin
suspend fun execute(gameId: Long, urls: List<String>): Flow<ScrapingProgress> = flow {
    val allNodes = mutableListOf<ContentNode>()

    urls.forEachIndexed { index, url ->
        emit(ScrapingProgress(current = index + 1, total = urls.size, url = url))
        val result = scraperEngine.scrape(url)
        allNodes.addAll(result.nodes)
    }

    articleRepository.save(article, allNodes)
    emit(ScrapingProgress.Done)
}
```

---

## 9. Reactive State — Kotlin Flow

```
Room DAO → Repository → Use Case → ViewModel (StateFlow) → Compose UI
```

```kotlin
class ReaderViewModel(
    private val observeNodesUseCase: ObserveNodesUseCase,
    private val saveCheckpointUseCase: SaveCheckpointUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun loadArticle(articleId: Long) {
        viewModelScope.launch {
            observeNodesUseCase(articleId).collect { nodes ->
                _uiState.update { it.copy(nodes = nodes) }
            }
        }
    }

    fun saveCheckpoint(nodeId: Long, displayOrder: Int) {
        viewModelScope.launch {
            saveCheckpointUseCase(nodeId, displayOrder)
        }
    }
}
```

---

## 10. Dependency Injection — Hilt

Hilt mengelola semua dependensi dan menyuntikkan implementasi yang tepat per build variant.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "questlog.db").build()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideBillingService(
        @ApplicationContext context: Context,
        prefs: SharedPreferences
    ): BillingService = when {
        BuildConfig.FULL_VERSION -> FullVersionBillingService()
        BuildConfig.DEBUG        -> FakeBillingService(prefs)
        else                     -> GooglePlayBillingService(context, prefs)
    }
}
```

---

## 11. Monetisasi — Build Variants

Tiga build variant dengan implementasi `BillingService` yang berbeda:

```kotlin
// Domain Layer — interface
interface BillingService {
    fun observePremiumStatus(): Flow<PremiumStatus>
    suspend fun purchaseUnlimited(): PurchaseResult
    suspend fun restorePurchases(): PurchaseResult
}
```

| Variant | `BillingService` | Deskripsi |
|---|---|---|
| `debug` | `FakeBillingService` | Toggle premium via SharedPreferences; untuk dev/testing |
| `full` | `FullVersionBillingService` | Selalu `PremiumStatus.Unlimited`; untuk APK sideload tanpa Play Store |
| `release` | `GooglePlayBillingService` | Google Play Billing resmi; untuk distribusi Play Store |

```kotlin
// Sideload APK — selalu premium, tanpa Play Store
class FullVersionBillingService : BillingService {
    override fun observePremiumStatus(): Flow<PremiumStatus> =
        flowOf(PremiumStatus.Unlimited)
    override suspend fun purchaseUnlimited() = PurchaseResult.Success
    override suspend fun restorePurchases() = PurchaseResult.Success
}
```

```kotlin
// Dev/testing — toggle manual
class FakeBillingService(private val prefs: SharedPreferences) : BillingService {
    override fun observePremiumStatus(): Flow<PremiumStatus> = flow {
        emit(if (prefs.getBoolean("fake_premium", false))
            PremiumStatus.Unlimited else PremiumStatus.Free)
    }
    override suspend fun purchaseUnlimited(): PurchaseResult {
        prefs.edit().putBoolean("fake_premium", true).apply()
        return PurchaseResult.Success
    }
    override suspend fun restorePurchases() = purchaseUnlimited()
}
```

### Konfigurasi Gradle

```kotlin
// build.gradle.kts (app)
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("Boolean", "FULL_VERSION", "false")
        }
        create("full") {
            initWith(getByName("release"))
            applicationIdSuffix = ".full"
            buildConfigField("Boolean", "FULL_VERSION", "true")
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            buildConfigField("Boolean", "FULL_VERSION", "false")
        }
    }
}
```

**Status premium** disimpan di DataStore / SharedPreferences — bukan Room — karena lebih ringan dan cukup untuk satu nilai boolean.

---

## 12. Ringkasan Library

| Kategori | Library | Versi (stabil) |
|---|---|---|
| UI | Jetpack Compose BOM | 2024.xx |
| Navigation | Navigation Compose | 2.8.x |
| DI | Hilt | 2.51.x |
| Database | Room | 2.6.x |
| Network | OkHttp | 4.12.x |
| HTML Parser | Ksoup | 0.1.x |
| Image Display | Coil 3 | 3.x |
| Background | WorkManager | 2.9.x |
| Reactive | Kotlin Coroutines + Flow | 1.8.x |
| Preferences | DataStore Preferences | 1.1.x |
| Billing | Google Play Billing | 7.x (release only) |
| Build | Gradle KTS | — |

---

## 13. Keputusan yang Tidak Dipilih

| Alternatif | Alasan tidak dipilih |
|---|---|
| Retrofit | Tidak diperlukan; scraping butuh raw HTML, bukan typed REST API |
| Glide | Coil lebih idiomatic untuk Compose dan Kotlin-first |
| JSON blob storage | Tidak mendukung FK checkpoint; query lambat; sulit append parsial |
| Parallel scraping | Berisiko rate-limit / IP ban dari situs target |
| Pixel-based checkpoint | Tidak reproducible setelah re-scrape; lebih rapuh |
| SQLite langsung | Room memberikan type-safety, Flow support, dan migration tools |
| Single build variant | Tidak bisa sideload tanpa Play Store; tidak efisien untuk dev testing |

---

*Dokumen diperbarui: September 2026*  
*Dibuat bersama: Taufan (Product Owner) × Claude (Software Architect)*
