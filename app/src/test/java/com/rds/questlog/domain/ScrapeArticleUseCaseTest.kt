package com.rds.questlog.domain

import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapeLimits
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.scraper.ScrapingResult
import com.rds.questlog.domain.usecase.article.ScrapeArticleUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrapeArticleUseCaseTest {

    private val ok = ScrapingResult.Success(listOf(ScrapedNode(NodeType.P, "isi")))

    private fun run(
        results: Map<String, ScrapingResult>,
        pages: List<com.rds.questlog.domain.model.SourcePage>,
    ): Pair<FakeSourcePageRepository, FakeScraperEngine> {
        val repository = FakeSourcePageRepository(pages)
        val scraper = FakeScraperEngine(results)
        runBlocking { ScrapeArticleUseCase(repository, scraper)(articleId = 1) }
        return repository to scraper
    }

    @Test
    fun `halaman sukses disimpan, dipulihkan lebih dulu, dan scraping ditandai selesai`() {
        val pages = listOf(page(1), page(2))
        val (repository, scraper) = run(pages.associate { it.url to ok }, pages)

        assertEquals(listOf("https://example.com/1", "https://example.com/2"), scraper.requested)
        assertEquals(setOf(1L, 2L), repository.savedNodes.keys)
        assertTrue(repository.failures.isEmpty())
        assertEquals(listOf("recover", "progress:1", "progress:2", "done"), repository.events)
    }

    @Test
    fun `kegagalan satu halaman dicatat dan tidak menghentikan halaman lain`() {
        val pages = listOf(page(1), page(2), page(3))
        val results = mapOf(
            pages[0].url to ScrapingResult.Failure(PageFailure.Http(404)),
            pages[1].url to ok,
            pages[2].url to ScrapingResult.Failure(PageFailure.Timeout),
        )
        val (repository, _) = run(results, pages)

        assertEquals(PageFailure.Http(404), repository.failures[1L])
        assertEquals(PageFailure.Timeout, repository.failures[3L])
        assertEquals(setOf(2L), repository.savedNodes.keys)
    }

    @Test
    fun `hasil tanpa node menjadi EmptyContent`() {
        val pages = listOf(page(1))
        val (repository, _) = run(mapOf(pages[0].url to ScrapingResult.Success(emptyList())), pages)
        assertEquals(PageFailure.EmptyContent, repository.failures[1L])
        assertTrue(repository.savedNodes.isEmpty())
    }

    @Test
    fun `halaman dengan node melebihi slot 1000 menjadi TooLong dan tidak disimpan`() {
        val pages = listOf(page(1))
        val tooMany = ScrapingResult.Success(
            List(ScrapeLimits.MAX_NODES_PER_PAGE + 1) { ScrapedNode(NodeType.P, "x$it") },
        )
        val (repository, _) = run(mapOf(pages[0].url to tooMany), pages)
        assertEquals(PageFailure.TooLong, repository.failures[1L])
        assertTrue(repository.savedNodes.isEmpty())
    }

    @Test
    fun `tepat 1000 node masih diterima`() {
        val pages = listOf(page(1))
        val exactly = ScrapingResult.Success(List(ScrapeLimits.MAX_NODES_PER_PAGE) { ScrapedNode(NodeType.P, "x$it") })
        val (repository, _) = run(mapOf(pages[0].url to exactly), pages)
        assertEquals(ScrapeLimits.MAX_NODES_PER_PAGE, repository.savedNodes.getValue(1L).size)
    }
}
