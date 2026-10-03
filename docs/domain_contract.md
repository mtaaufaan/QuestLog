# Domain Contract — QuestLog

**Status**: Draft v1.0  
**Dibuat oleh**: Architect (Claude)  
**Scope**: Definisi interface Repository dan UseCase untuk seluruh layar MVP.  
**Arsitektur**: MVVM + Clean Architecture — Presentation → Domain → Data

---

## 1. Layer Overview

```
┌─────────────────────────────────────────────┐
│              PRESENTATION                   │
│  Screen (Composable) ← ViewModel ← UiState │
│  Hanya tahu tentang UseCase, bukan Repo     │
└───────────────────┬─────────────────────────┘
                    │ memanggil
┌───────────────────▼─────────────────────────┐
│                 DOMAIN                      │
│  UseCase (business logic, enforcement)      │
│  Repository Interfaces (kontrak)            │
│  Domain Models (murni Kotlin, tanpa Android)│
└───────────────────┬─────────────────────────┘
                    │ implementasi
┌───────────────────▼─────────────────────────┐
│                  DATA                       │
│  RepositoryImpl → RoomDAO / DataStore /     │
│  WorkManager / BillingClient                │
└─────────────────────────────────────────────┘
```

---

## 2. Domain Models

Model di domain layer adalah plain Kotlin class — tanpa Room annotation, tanpa Android import.

```kotlin
// domain/model/Game.kt
data class Game(
    val id: Long,
    val name: String,
    val createdAt: Long
)

// domain/model/Article.kt
data class Article(
    val id: Long,
    val gameId: Long,
    val gameName: String,
    val title: String,
    val scrapingStatus: ScrapingStatus,
    val isScrapingDone: Boolean,
    val scrapedPageCount: Int,
    val totalPageCount: Int,
    val lastReadAt: Long?,
    val createdAt: Long
)

enum class ScrapingStatus { PENDING, SCRAPING, READY, ERROR }

// domain/model/ContentNode.kt
data class ContentNode(
    val id: Long,
    val articleId: Long,
    val type: ContentType,
    val content: String,
    val displayOrder: Int
)

enum class ContentType { HEADING, PARAGRAPH, LIST_ITEM, IMAGE_REF, TABLE }

// domain/model/Checkpoint.kt
data class Checkpoint(
    val articleId: Long,
    val anchorNodeId: Long,
    val fallbackOrder: Int,
    val lastVisitedNodeId: Long?,
    val updatedAt: Long
)

// domain/model/DisplayPreferences.kt
data class DisplayPreferences(
    val fontSizeSp: Int,          // default: 16
    val darkModeOverride: Boolean? // null = ikuti sistem
)
```

---

## 3. Repository Interfaces

Semua interface berada di domain layer. Implementasi ada di data layer.

### 3.1 GameRepository

```kotlin
// domain/repository/GameRepository.kt

interface GameRepository {

    /** Stream semua game, diurutkan berdasarkan created_at DESC. */
    fun getGames(): Flow<List<Game>>

    /** Jumlah total game yang tersimpan. Digunakan untuk free tier check. */
    fun getGameCount(): Flow<Int>

    /**
     * Insert game baru.
     * @return id game yang baru dibuat
     * @throws GameAlreadyExistsException jika nama sudah ada (case-insensitive)
     */
    suspend fun insertGame(name: String): Long

    /** Hapus game beserta semua artikel dan kontennya (cascade via FK). */
    suspend fun deleteGame(gameId: Long)
}
```

### 3.2 ArticleRepository

```kotlin
// domain/repository/ArticleRepository.kt

interface ArticleRepository {

    /**
     * Stream daftar artikel.
     * @param gameId null = semua game; non-null = filter per game
     */
    fun getArticles(gameId: Long? = null): Flow<List<Article>>

    /** Stream satu artikel by id. */
    fun getArticleById(articleId: Long): Flow<Article?>

    /** Jumlah artikel dalam satu game. Digunakan untuk free tier check. */
    fun getArticleCountByGame(gameId: Long): Flow<Int>

    /**
     * Insert artikel baru dengan status PENDING.
     * @return id artikel yang baru dibuat
     */
    suspend fun insertArticle(gameId: Long, title: String): Long

    /** Insert source_pages (URL-URL yang akan di-scrape) untuk sebuah artikel. */
    suspend fun insertSourcePages(articleId: Long, urls: List<String>)

    /** Update status scraping artikel. */
    suspend fun updateScrapingStatus(articleId: Long, status: ScrapingStatus)

    /** Update jumlah halaman yang sudah di-scrape (untuk notifikasi progress). */
    suspend fun updateScrapedPageCount(articleId: Long, count: Int)

    /** Tandai artikel sebagai selesai di-scrape (is_scraping_done = 1, status = READY). */
    suspend fun markScrapingDone(articleId: Long)

    /** Hapus artikel beserta semua content_nodes dan checkpoint-nya (cascade). */
    suspend fun deleteArticle(articleId: Long)
}
```

### 3.3 ContentNodeRepository

```kotlin
// domain/repository/ContentNodeRepository.kt

interface ContentNodeRepository {

    /** Stream semua konten sebuah artikel, diurutkan berdasarkan display_order ASC. */
    fun getContentNodes(articleId: Long): Flow<List<ContentNode>>
}
```

### 3.4 CheckpointRepository

```kotlin
// domain/repository/CheckpointRepository.kt

interface CheckpointRepository {

    /** Stream checkpoint (posisi terakhir baca) untuk sebuah artikel. */
    fun getCheckpoint(articleId: Long): Flow<Checkpoint?>

    /**
     * Simpan/update posisi terakhir baca (upsert).
     * Jika checkpoint belum ada, buat baru dengan anchorNodeId = nodeId.
     */
    suspend fun saveLastPosition(articleId: Long, nodeId: Long, fallbackOrder: Int)
}
```

### 3.5 AppConfigRepository

```kotlin
// domain/repository/AppConfigRepository.kt

interface AppConfigRepository {

    /** Stream status premium (is_premium dari tabel app_config). */
    fun isPremium(): Flow<Boolean>

    /** Set status premium dan catat timestamp verifikasi. */
    suspend fun setPremiumStatus(
        isPremium: Boolean,
        purchaseToken: String?,
        verifiedAt: Long
    )

    /** Ambil purchase token untuk verifikasi ulang ke Play Billing. */
    suspend fun getPurchaseToken(): String?
}
```

### 3.6 UserPreferencesRepository

```kotlin
// domain/repository/UserPreferencesRepository.kt
// Sumber data: DataStore<Preferences> (bukan Room)

interface UserPreferencesRepository {

    /** Stream preferensi tampilan (font size + dark mode). */
    fun getDisplayPreferences(): Flow<DisplayPreferences>

    /** Set ukuran font. Range valid: 12–24 sp. */
    suspend fun setFontSize(sizeSp: Int)

    /**
     * Set override dark mode.
     * @param enabled null = ikuti sistem Android
     */
    suspend fun setDarkModeOverride(enabled: Boolean?)
}
```

---

## 4. UseCase Definitions

UseCase = satu aksi bisnis. Tidak boleh memanggil ViewModel atau View.
Diinjeksikan ke ViewModel via Hilt constructor injection.

### 4.1 Game UseCases

```kotlin
// domain/usecase/game/GetGamesUseCase.kt
class GetGamesUseCase @Inject constructor(
    private val gameRepository: GameRepository
) {
    operator fun invoke(): Flow<List<Game>> = gameRepository.getGames()
}

// domain/usecase/game/GetGameCountUseCase.kt
class GetGameCountUseCase @Inject constructor(
    private val gameRepository: GameRepository
) {
    operator fun invoke(): Flow<Int> = gameRepository.getGameCount()
}

// domain/usecase/game/GetGameFilterOptionsUseCase.kt
// Derived dari game list — untuk dropdown filter di ArticleListScreen
class GetGameFilterOptionsUseCase @Inject constructor(
    private val gameRepository: GameRepository
) {
    operator fun invoke(): Flow<List<String>> =
        gameRepository.getGames().map { games -> games.map { it.name } }
}

// domain/usecase/game/CreateGameUseCase.kt
class CreateGameUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val appConfigRepository: AppConfigRepository
) {
    sealed class Result {
        data class Success(val gameId: Long) : Result()
        object GameLimitReached : Result()    // maks. 2 game (free tier)
        data class Error(val cause: Throwable) : Result()
    }

    suspend operator fun invoke(name: String): Result {
        val isPremium = appConfigRepository.isPremium().first()

        if (!isPremium) {
            val gameCount = gameRepository.getGameCount().first()
            if (gameCount >= 2) return Result.GameLimitReached
        }

        return try {
            val gameId = gameRepository.insertGame(name)
            Result.Success(gameId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### 4.2 Article UseCases

```kotlin
// domain/usecase/article/GetArticlesUseCase.kt
class GetArticlesUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(gameId: Long? = null): Flow<List<Article>> =
        articleRepository.getArticles(gameId)
}

// domain/usecase/article/GetArticleDetailUseCase.kt
class GetArticleDetailUseCase @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val contentNodeRepository: ContentNodeRepository
) {
    data class ArticleDetail(
        val article: Article,
        val contentNodes: List<ContentNode>
    )

    operator fun invoke(articleId: Long): Flow<ArticleDetail> =
        combine(
            articleRepository.getArticleById(articleId).filterNotNull(),
            contentNodeRepository.getContentNodes(articleId)
        ) { article, nodes ->
            ArticleDetail(article, nodes)
        }
}

// domain/usecase/article/GetArticleCountForGameUseCase.kt
class GetArticleCountForGameUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(gameId: Long): Flow<Int> =
        articleRepository.getArticleCountByGame(gameId)
}

// domain/usecase/article/SaveArticleUseCase.kt
// Free tier enforcement ada di sini — bukan di DB constraint
class SaveArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val appConfigRepository: AppConfigRepository
) {
    sealed class Result {
        data class Success(val articleId: Long) : Result()
        object ArticleLimitReached : Result()  // maks. 5 artikel/game (free tier)
        data class Error(val cause: Throwable) : Result()
    }

    suspend operator fun invoke(
        gameId: Long,
        title: String,
        urls: List<String>   // 1–10 URL source pages
    ): Result {
        val isPremium = appConfigRepository.isPremium().first()

        if (!isPremium) {
            val articleCount = articleRepository.getArticleCountByGame(gameId).first()
            if (articleCount >= 5) return Result.ArticleLimitReached
        }

        return try {
            val articleId = articleRepository.insertArticle(gameId, title)
            articleRepository.insertSourcePages(articleId, urls)
            Result.Success(articleId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// domain/usecase/article/DeleteArticleUseCase.kt
class DeleteArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    suspend operator fun invoke(articleId: Long) =
        articleRepository.deleteArticle(articleId)
}
```

### 4.3 Checkpoint UseCases (auto-resume)

```kotlin
// domain/usecase/checkpoint/GetLastPositionUseCase.kt
class GetLastPositionUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository
) {
    /** Mengembalikan lastVisitedNodeId, atau null jika belum ada checkpoint. */
    operator fun invoke(articleId: Long): Flow<Long?> =
        checkpointRepository.getCheckpoint(articleId)
            .map { it?.lastVisitedNodeId }
}

// domain/usecase/checkpoint/SaveLastPositionUseCase.kt
class SaveLastPositionUseCase @Inject constructor(
    private val checkpointRepository: CheckpointRepository
) {
    suspend operator fun invoke(
        articleId: Long,
        nodeId: Long,
        fallbackOrder: Int
    ) = checkpointRepository.saveLastPosition(articleId, nodeId, fallbackOrder)
}
```

### 4.4 Premium UseCases

```kotlin
// domain/usecase/premium/CheckPremiumStatusUseCase.kt
class CheckPremiumStatusUseCase @Inject constructor(
    private val appConfigRepository: AppConfigRepository
) {
    operator fun invoke(): Flow<Boolean> = appConfigRepository.isPremium()
}

// domain/usecase/premium/VerifyAndCachePurchaseUseCase.kt
// Dipanggil saat app launch oleh AppViewModel (bukan per-screen)
class VerifyAndCachePurchaseUseCase @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    private val billingManager: BillingManager
) {
    sealed class Result {
        object Premium : Result()
        object Free : Result()
        data class Error(val cause: Throwable) : Result()
    }

    suspend operator fun invoke(): Result {
        return try {
            val token = appConfigRepository.getPurchaseToken()
            val isPremium = billingManager.queryActivePurchases(token)
            appConfigRepository.setPremiumStatus(
                isPremium = isPremium,
                purchaseToken = if (isPremium) token else null,
                verifiedAt = System.currentTimeMillis()
            )
            if (isPremium) Result.Premium else Result.Free
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### 4.5 Display Preferences UseCases

```kotlin
// domain/usecase/prefs/GetDisplayPreferencesUseCase.kt
class GetDisplayPreferencesUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    operator fun invoke(): Flow<DisplayPreferences> =
        userPreferencesRepository.getDisplayPreferences()
}

// domain/usecase/prefs/SetFontSizeUseCase.kt
class SetFontSizeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /** Size valid: 12–24 sp. Nilai di luar range di-clamp otomatis. */
    suspend operator fun invoke(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(12, 24)
        userPreferencesRepository.setFontSize(clamped)
    }
}

// domain/usecase/prefs/SetDarkModeUseCase.kt
class SetDarkModeUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /** enabled = null berarti ikuti sistem Android. */
    suspend operator fun invoke(enabled: Boolean?) =
        userPreferencesRepository.setDarkModeOverride(enabled)
}
```

### 4.6 Free Tier Guard (reactive — untuk disable button di UI)

```kotlin
// domain/usecase/tier/CanAddGameUseCase.kt
class CanAddGameUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val appConfigRepository: AppConfigRepository
) {
    operator fun invoke(): Flow<Boolean> =
        combine(
            appConfigRepository.isPremium(),
            gameRepository.getGameCount()
        ) { isPremium, count -> isPremium || count < 2 }
}

// domain/usecase/tier/CanAddArticleUseCase.kt
class CanAddArticleUseCase @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val appConfigRepository: AppConfigRepository
) {
    operator fun invoke(gameId: Long): Flow<Boolean> =
        combine(
            appConfigRepository.isPremium(),
            articleRepository.getArticleCountByGame(gameId)
        ) { isPremium, count -> isPremium || count < 5 }
}
```

---

## 5. BillingManager Interface

Abstraksi di domain layer. Implementasi menggunakan Google Play Billing di data layer.

```kotlin
// domain/billing/BillingManager.kt

interface BillingManager {
    /**
     * Verifikasi ke Google Play apakah ada purchase aktif.
     * Wrapper atas BillingClient.queryPurchasesAsync().
     * @param cachedToken token yang tersimpan di app_config (bisa null)
     * @return true jika ada purchase aktif dengan status PURCHASED
     */
    suspend fun queryActivePurchases(cachedToken: String?): Boolean
}
```

---

## 6. UseCase → ViewModel Mapping

| ViewModel | UseCase yang digunakan |
|-----------|------------------------|
| `AppViewModel` | `VerifyAndCachePurchaseUseCase` |
| `ArticleListViewModel` | `GetArticlesUseCase`, `GetGameFilterOptionsUseCase`, `DeleteArticleUseCase`, `CheckPremiumStatusUseCase` |
| `AddArticleViewModel` | `GetGamesUseCase`, `CreateGameUseCase`, `SaveArticleUseCase`, `CanAddGameUseCase`, `CanAddArticleUseCase` |
| `ReaderViewModel` | `GetArticleDetailUseCase`, `GetLastPositionUseCase`, `SaveLastPositionUseCase`, `GetDisplayPreferencesUseCase`, `SetFontSizeUseCase`, `SetDarkModeUseCase` |

---

## 7. Dependency Injection — Hilt Module Outline

```kotlin
// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindGameRepo(impl: GameRepositoryImpl): GameRepository
    @Binds abstract fun bindArticleRepo(impl: ArticleRepositoryImpl): ArticleRepository
    @Binds abstract fun bindContentNodeRepo(impl: ContentNodeRepositoryImpl): ContentNodeRepository
    @Binds abstract fun bindCheckpointRepo(impl: CheckpointRepositoryImpl): CheckpointRepository
    @Binds abstract fun bindAppConfigRepo(impl: AppConfigRepositoryImpl): AppConfigRepository
    @Binds abstract fun bindUserPrefsRepo(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
    @Binds abstract fun bindBillingManager(impl: BillingManagerImpl): BillingManager
}
```

---

## 8. Business Rules Summary

| Rule | Enforcement | Lokasi |
|------|-------------|--------|
| Maks. 2 game (free tier) | `CreateGameUseCase` | Domain |
| Maks. 5 artikel/game (free tier) | `SaveArticleUseCase` | Domain |
| Button add disabled jika limit | `CanAddGameUseCase`, `CanAddArticleUseCase` | Domain |
| Premium verify via Play Billing | `VerifyAndCachePurchaseUseCase` | Domain + Data |
| Premium cache di DB | `AppConfigRepository.setPremiumStatus()` | Data |
| Font size range 12–24 sp | `SetFontSizeUseCase` (`coerceIn`) | Domain |
| Auto-resume posisi baca | `SaveLastPositionUseCase` + `GetLastPositionUseCase` | Domain |

---

## 9. Referensi Dokumen Terkait

| Dokumen | Path |
|---------|------|
| Screen Flow | `docs/screen_flow.md` |
| Database Schema | `docs/database_schema.md` |
| Tech Stack | `docs/tech-stack.md` |
| Error State Model | `docs/error_state_model.md` *(akan dibuat)* |
