package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.scraper.ScraperEngine
import com.rds.questlog.domain.scraper.ScrapingResult
import com.rds.questlog.domain.usecase.article.ScrapeArticleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-20 di Room nyata: isi lama bertahan sampai yang baru sukses dan checkpoint dipulihkan ke node baru. */
@RunWith(AndroidJUnit4::class)
class RefreshDataTest {

    private lateinit var env: TestEnvironment
    private var articleId = 0L
    private lateinit var pageIds: List<Long>

    @Before
    fun setUp() = runBlocking {
        env = TestEnvironment()
        val game = env.games.findOrCreate("Game")
        articleId = env.articles.insertArticle(game, "A", listOf("https://a/1", "https://a/2"))
        pageIds = env.db.pageLayoutDao().getByArticle(articleId).map { it.id }
        pageIds.forEachIndexed { i, id -> env.pages.saveScrapedPage(articleId, id, nodes(i + 1, "lama"), emptyList()) }
    }

    @After
    fun tearDown() = env.close()

    private fun nodes(page: Int, tag: String) = listOf(
        ScrapedNode(NodeType.P, "h$page-awal"),
        ScrapedNode(NodeType.P, "h$page-tengah-$tag"),
        ScrapedNode(NodeType.P, "h$page-akhir"),
    )

    private fun texts() = runBlocking { env.articles.observeContent(articleId).first() }.map { it.text }

    private fun nodeId(text: String) =
        runBlocking { env.articles.observeContent(articleId).first() }.first { it.text == text }.id

    private fun checkpoint() = runBlocking { env.db.checkpointDao().get(articleId) }

    private fun statuses() = runBlocking { env.articles.observeArticle(articleId).first()!! }.pages.map { it.status }

    private fun failureOf(block: suspend () -> Unit): Throwable? =
        runBlocking { runCatching { block() }.exceptionOrNull() }

    /** Scraper palsu: tiap URL punya hasilnya sendiri. */
    private class FakeScraper(private val results: Map<String, ScrapingResult>) : ScraperEngine {
        override suspend fun scrape(url: String): ScrapingResult = results.getValue(url)
    }

    private fun scrape(vararg results: Pair<String, ScrapingResult>) =
        runBlocking { ScrapeArticleUseCase(env.pages, FakeScraper(results.toMap()))(articleId) }

    @Test
    fun menandaiUnduhUlangTidakMenghapusIsiLama() = runBlocking {
        val before = texts()

        env.management.markPagesForRefresh(articleId)

        assertEquals(listOf(SourcePageStatus.PENDING, SourcePageStatus.PENDING), statuses())
        assertEquals(before, texts())
    }

    @Test
    fun menandaiSatuHalamanTidakMenyentuhHalamanLain() = runBlocking {
        env.management.markPagesForRefresh(articleId, pageIds[1])

        assertEquals(listOf(SourcePageStatus.COMPLETED, SourcePageStatus.PENDING), statuses())
        assertEquals(PageNotFoundExpected, failureOf { env.management.markPagesForRefresh(articleId, 9_999) })
    }

    @Test
    fun unduhUlangSuksesMenggantiIsiDanMemulihkanCheckpointByTeksSama() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h1-akhir"))
        env.checkpoints.saveLastPosition(articleId, nodeId("h1-tengah-lama"))
        env.management.markPagesForRefresh(articleId, pageIds[0])
        // Halaman baru punya paragraf tambahan di depan: posisi bergeser tetapi teksnya sama.
        val fresh = listOf(ScrapedNode(NodeType.P, "sisipan")) + nodes(1, "baru")

        scrape("https://a/1" to ScrapingResult.Success(fresh, emptyList()))

        assertEquals(
            listOf("sisipan", "h1-awal", "h1-tengah-baru", "h1-akhir", "h2-awal", "h2-tengah-lama", "h2-akhir"),
            texts(),
        )
        assertEquals(nodeId("h1-akhir"), checkpoint()!!.anchorNodeId)
        assertEquals(4, checkpoint()!!.fallbackOrder)
        // "h1-tengah-lama" sudah berubah teksnya: jatuh ke node terdekat di halaman yang sama (posisi 2).
        assertEquals(nodeId("h1-awal"), checkpoint()!!.lastVisitedNodeId)
        assertEquals(listOf(SourcePageStatus.COMPLETED, SourcePageStatus.COMPLETED), statuses())
    }

    @Test
    fun unduhUlangGagalTotalMempertahankanIsiLamaDanCheckpoint() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h1-awal"))
        env.checkpoints.saveLastPosition(articleId, nodeId("h2-akhir"))
        val before = texts()
        val anchor = nodeId("h1-awal")
        env.management.markPagesForRefresh(articleId)

        scrape(
            "https://a/1" to ScrapingResult.Failure(PageFailure.Timeout),
            "https://a/2" to ScrapingResult.Success(emptyList(), emptyList()),
        )

        assertEquals(listOf(SourcePageStatus.FAILED, SourcePageStatus.FAILED), statuses())
        assertEquals(before, texts())
        assertEquals(anchor, checkpoint()!!.anchorNodeId)
        assertEquals(nodeId("h2-akhir"), checkpoint()!!.lastVisitedNodeId)
    }

    @Test
    fun unduhUlangSebagianGantiYangSuksesSajaDanYangGagalTetapUtuh() = runBlocking {
        env.management.markPagesForRefresh(articleId)

        scrape(
            "https://a/1" to ScrapingResult.Success(nodes(1, "baru"), emptyList()),
            "https://a/2" to ScrapingResult.Failure(PageFailure.Timeout),
        )

        assertEquals(listOf(SourcePageStatus.COMPLETED, SourcePageStatus.FAILED), statuses())
        assertEquals(
            listOf("h1-awal", "h1-tengah-baru", "h1-akhir", "h2-awal", "h2-tengah-lama", "h2-akhir"),
            texts(),
        )
    }

    @Test
    fun checkpointDiHalamanLainTidakTersentuh() = runBlocking {
        env.checkpoints.setCheckpoint(articleId, nodeId("h2-tengah-lama"))
        val anchor = nodeId("h2-tengah-lama")
        env.management.markPagesForRefresh(articleId, pageIds[0])

        scrape("https://a/1" to ScrapingResult.Success(nodes(1, "baru"), emptyList()))

        assertEquals(anchor, checkpoint()!!.anchorNodeId)
    }

    @Test
    fun gambarYangSamaTidakDiduplikasiSaatUnduhUlang() = runBlocking {
        val image = ScrapedImage("https://a/i.png", "abc.webp", "/x/abc.webp", 10)
        env.pages.saveScrapedPage(articleId, pageIds[0], nodes(1, "lama"), listOf(image))
        env.management.markPagesForRefresh(articleId, pageIds[0])

        scrape("https://a/1" to ScrapingResult.Success(nodes(1, "baru"), listOf(image)))

        assertEquals(1, env.count("SELECT COUNT(*) FROM images"))
    }

    @Test
    fun unduhUlangHalamanTanpaCheckpointTidakMembuatBarisCheckpoint() = runBlocking {
        env.management.markPagesForRefresh(articleId, pageIds[0])

        scrape("https://a/1" to ScrapingResult.Success(nodes(1, "baru"), emptyList()))

        assertNull(checkpoint())
        assertTrue(texts().contains("h1-tengah-baru"))
    }

    private companion object {
        val PageNotFoundExpected = ArticleError.PageNotFound
    }
}
