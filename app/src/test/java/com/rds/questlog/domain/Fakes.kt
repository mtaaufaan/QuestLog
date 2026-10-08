package com.rds.questlog.domain

import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.ContentNode
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.model.ReadMode
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.repository.AppConfigRepository
import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.BillingService
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.repository.SourcePageRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.scraper.ScraperEngine
import com.rds.questlog.domain.scraper.ScrapingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

fun page(id: Long, url: String = "https://example.com/$id", status: SourcePageStatus = SourcePageStatus.PENDING) =
    SourcePage(id = id, url = url, order = id.toInt(), status = status)

class FakeScheduler : ScrapeScheduler {
    val scheduled = mutableListOf<Long>()
    override fun schedule(articleId: Long) {
        scheduled += articleId
    }
}

class FakeGameRepository(
    private val failWith: Throwable? = null,
    private val existing: Map<String, Long> = emptyMap(),
    private val gameCount: Int = 0,
    private val games: List<Game> = emptyList(),
) : GameRepository {
    val created = mutableListOf<String>()
    val emptyChecked = mutableListOf<Long>()
    override fun observeGames(): Flow<List<Game>> = flowOf(games)
    override suspend fun findIdByName(name: String): Long? = existing[name]
    override suspend fun count(): Int = gameCount
    override suspend fun findOrCreate(name: String): Long {
        failWith?.let { throw it }
        created += name
        return 7L
    }
    override suspend fun deleteIfEmpty(gameId: Long) {
        emptyChecked += gameId
    }
}

class FakeArticleRepository(
    private val failWith: Throwable? = null,
    private val resetCount: Int = 0,
    private val articleCount: Int = 0,
    private val stored: Set<String> = emptySet(),
    private val article: Article? = null,
) : ArticleRepository {
    val appended = mutableListOf<Pair<Long, List<String>>>()
    val inserted = mutableListOf<Triple<Long, String, List<String>>>()
    val deleted = mutableListOf<Long>()
    override fun observeArticles(query: String, gameId: Long?): Flow<List<Article>> = emptyFlow()
    override suspend fun insertArticle(gameId: Long, title: String, urls: List<String>): Long {
        failWith?.let { throw it }
        inserted += Triple(gameId, title, urls)
        return 42L
    }
    override fun observeArticle(articleId: Long): Flow<Article?> = flowOf(article)
    override fun observeContent(articleId: Long): Flow<List<ContentNode>> = emptyFlow()
    val readModes = mutableMapOf<Long, ReadMode>()
    override suspend fun setReadMode(articleId: Long, mode: ReadMode) {
        failWith?.let { throw it }
        readModes[articleId] = mode
    }
    override suspend fun appendPages(articleId: Long, urls: List<String>) {
        failWith?.let { throw it }
        appended += articleId to urls
    }
    override suspend fun countForGame(gameId: Long): Int = articleCount
    override suspend fun findStoredUrls(urls: List<String>): Set<String> = urls.filter { it in stored }.toSet()
    override suspend fun deleteArticle(articleId: Long) {
        failWith?.let { throw it }
        deleted += articleId
    }
    override suspend fun retryFailedPages(articleId: Long): Int {
        failWith?.let { throw it }
        return resetCount
    }
}

class FakeArticleManagementRepository(private val failWith: Throwable? = null) : ArticleManagementRepository {
    val updates = mutableListOf<Triple<Long, String, GameTarget>>()
    val deletedPages = mutableListOf<Pair<Long, Long>>()
    val reordered = mutableListOf<Pair<Long, List<Long>>>()
    val refreshed = mutableListOf<Pair<Long, Long?>>()
    override suspend fun markPagesForRefresh(articleId: Long, pageId: Long?) {
        failWith?.let { throw it }
        refreshed += articleId to pageId
    }
    override suspend fun deletePage(articleId: Long, pageId: Long) {
        failWith?.let { throw it }
        deletedPages += articleId to pageId
    }
    override suspend fun reorderPages(articleId: Long, orderedPageIds: List<Long>) {
        failWith?.let { throw it }
        reordered += articleId to orderedPageIds
    }
    override suspend fun updateDetails(articleId: Long, title: String, target: GameTarget) {
        failWith?.let { throw it }
        updates += Triple(articleId, title, target)
    }
}

class FakeSourcePageRepository(private val pending: List<SourcePage>) : SourcePageRepository {
    val events = mutableListOf<String>()
    val savedNodes = mutableMapOf<Long, List<ScrapedNode>>()
    val failures = mutableMapOf<Long, PageFailure>()

    override suspend fun recoverInterrupted(articleId: Long) {
        events += "recover"
    }
    override suspend fun getPendingPages(articleId: Long): List<SourcePage> = pending
    override suspend fun markInProgress(pageId: Long) {
        events += "progress:$pageId"
    }
    override suspend fun saveScrapedPage(
        articleId: Long,
        pageId: Long,
        nodes: List<ScrapedNode>,
        images: List<ScrapedImage>,
    ) {
        savedNodes[pageId] = nodes
    }
    override suspend fun markFailed(pageId: Long, failure: PageFailure) {
        failures[pageId] = failure
    }
    override suspend fun updateScrapingDone(articleId: Long) {
        events += "done"
    }
}

class FakeBillingService(
    private val query: PurchaseQuery = PurchaseQuery.NotOwned,
    private val purchase: PurchaseResult = PurchaseResult.Cancelled,
) : BillingService {
    var queries = 0
    override suspend fun queryUnlimitedPurchase(): PurchaseQuery {
        queries++
        return query
    }
    override suspend fun purchaseUnlimited(): PurchaseResult = purchase
}

/** app_config di memori: [premium] bisa diubah dan Flow-nya ikut berubah, seperti tabel Room. */
class FakeAppConfigRepository(premium: Boolean = false, private val failWith: Throwable? = null) : AppConfigRepository {
    private val state = MutableStateFlow(premium)
    var token: String? = null
    var verifiedAt: Long? = null
    var writes = 0
    override fun observeIsPremium(): Flow<Boolean> = state
    override suspend fun setPremium(isPremium: Boolean, purchaseToken: String?, verifiedAt: Long) {
        failWith?.let { throw it }
        writes++
        state.value = isPremium
        token = purchaseToken
        this.verifiedAt = verifiedAt
    }
}

class FakeScraperEngine(private val results: Map<String, ScrapingResult>) : ScraperEngine {
    val requested = mutableListOf<String>()
    override suspend fun scrape(url: String): ScrapingResult {
        requested += url
        return results.getValue(url)
    }
}
