package com.rds.questlog.domain

import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.repository.SourcePageRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.scraper.ScraperEngine
import com.rds.questlog.domain.scraper.ScrapingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun page(id: Long, url: String = "https://example.com/$id", status: SourcePageStatus = SourcePageStatus.PENDING) =
    SourcePage(id = id, url = url, order = id.toInt(), status = status)

class FakeScheduler : ScrapeScheduler {
    val scheduled = mutableListOf<Long>()
    override fun schedule(articleId: Long) {
        scheduled += articleId
    }
}

class FakeGameRepository(private val failWith: Throwable? = null) : GameRepository {
    val created = mutableListOf<String>()
    override fun observeGames(): Flow<List<Game>> = emptyFlow()
    override suspend fun findOrCreate(name: String): Long {
        failWith?.let { throw it }
        created += name
        return 7L
    }
}

class FakeArticleRepository(
    private val failWith: Throwable? = null,
    private val resetCount: Int = 0,
) : ArticleRepository {
    val inserted = mutableListOf<Triple<Long, String, List<String>>>()
    val deleted = mutableListOf<Long>()
    override fun observeArticles(query: String, gameId: Long?): Flow<List<Article>> = emptyFlow()
    override suspend fun insertArticle(gameId: Long, title: String, urls: List<String>): Long {
        failWith?.let { throw it }
        inserted += Triple(gameId, title, urls)
        return 42L
    }
    override suspend fun deleteArticle(articleId: Long) {
        failWith?.let { throw it }
        deleted += articleId
    }
    override suspend fun retryFailedPages(articleId: Long): Int {
        failWith?.let { throw it }
        return resetCount
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

class FakeScraperEngine(private val results: Map<String, ScrapingResult>) : ScraperEngine {
    val requested = mutableListOf<String>()
    override suspend fun scrape(url: String): ScrapingResult {
        requested += url
        return results.getValue(url)
    }
}
