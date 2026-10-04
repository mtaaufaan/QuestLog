package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.model.ScrapingStatus
import com.rds.questlog.domain.usecase.article.ScrapeArticleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-2/QL-3/QL-15: multi-URL berurutan, append-only dengan slot dilanjutkan, retry tidak mengulang halaman DONE. */
@RunWith(AndroidJUnit4::class)
class MultiUrlScrapingTest {

    private val server = MockWebServer()
    private val hits = mutableListOf<String>()
    private val missing = mutableSetOf<String>()
    private lateinit var env: TestEnvironment
    private lateinit var scrape: ScrapeArticleUseCase
    private var gameId = 0L

    @Before
    fun setUp() {
        env = TestEnvironment(timeoutSeconds = 2)
        scrape = ScrapeArticleUseCase(env.pages, env.scraper)
        gameId = runBlocking { env.games.findOrCreate("Game") }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                hits += path
                if (path in missing) return MockResponse().setResponseCode(404)
                val body = "<html><body><article><h1>Judul $path</h1><p>Isi halaman $path.</p></article></body></html>"
                return MockResponse().setHeader("Content-Type", "text/html").setBody(body)
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        env.close()
    }

    private fun url(path: String) = server.url(path).toString()

    private fun pageOrders() = env.strings("SELECT page_order FROM source_pages ORDER BY page_order").map(String::toInt)

    private fun article() = runBlocking { env.articles.observeArticles("", null).first().single() }

    @Test(timeout = 60_000)
    fun semuaUrlDigabungBerurutanDenganSlotPerHalaman() = runBlocking {
        val id = env.articles.insertArticle(gameId, "A", listOf(url("/p1"), url("/p2"), url("/p3")))
        scrape(id)

        assertEquals(listOf("/p1", "/p2", "/p3"), hits)
        assertEquals(ScrapingStatus.READY, article().status)
        // Tiap halaman menulis ke slot sendiri: 1.., 1001.., 2001..
        assertEquals(
            listOf(1, 2, 1001, 1002, 2001, 2002),
            env.strings("SELECT display_order FROM content_nodes ORDER BY display_order").map { it.toInt() },
        )
        assertEquals(listOf(1, 2, 3), pageOrders())
    }

    @Test(timeout = 60_000)
    fun lengkapiMelanjutkanUrutanDanTidakMenggeserKontenLama() = runBlocking {
        val id = env.articles.insertArticle(gameId, "A", listOf(url("/p1"), url("/p2")))
        scrape(id)
        val before = env.strings("SELECT id FROM content_nodes ORDER BY display_order")

        env.articles.appendPages(id, listOf(url("/p3"), url("/p4")))
        assertEquals(ScrapingStatus.SCRAPING, article().status)
        scrape(id)

        assertEquals(listOf("/p1", "/p2", "/p3", "/p4"), hits)
        assertEquals(listOf(1, 2, 3, 4), pageOrders())
        assertEquals(
            listOf(1, 2, 1001, 1002, 2001, 2002, 3001, 3002),
            env.strings("SELECT display_order FROM content_nodes ORDER BY display_order").map { it.toInt() },
        )
        // Node lama tetap utuh (checkpoint yang menunjuk id node lama tetap valid).
        assertEquals(
            before,
            env.strings("SELECT id FROM content_nodes WHERE display_order < 2000 ORDER BY display_order"),
        )
        assertEquals(ScrapingStatus.READY, article().status)
    }

    @Test(timeout = 60_000)
    fun sebagianGagalTetapReadyDanRetryHanyaMengulangYangGagal() = runBlocking {
        missing += "/p2"
        val id = env.articles.insertArticle(gameId, "A", listOf(url("/p1"), url("/p2"), url("/p3")))
        scrape(id)

        val partial = article()
        assertEquals(ScrapingStatus.READY, partial.status)
        assertEquals(1, partial.pages.count { it.failure != null })

        missing.clear()
        hits.clear()
        assertEquals(1, env.articles.retryFailedPages(id))
        scrape(id)

        assertEquals(listOf("/p2"), hits)
        assertTrue(article().pages.all { it.failure == null })
    }

    @Test(timeout = 60_000)
    fun urlTersimpanDikenaliDiArtikelManaPun() = runBlocking {
        env.articles.insertArticle(gameId, "A", listOf(url("/p1")))
        assertEquals(setOf(url("/p1")), env.articles.findStoredUrls(listOf(url("/p1"), url("/zzz"))))
    }
}
