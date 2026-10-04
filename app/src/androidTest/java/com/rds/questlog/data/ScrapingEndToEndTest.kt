package com.rds.questlog.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapingStatus
import com.rds.questlog.domain.usecase.article.ScrapeArticleUseCase
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** QL-1: satu URL â†’ konten tersimpan lokal dan terbaca tanpa koneksi; URL gagal â†’ pesan jelas, tidak crash/hang. */
@RunWith(AndroidJUnit4::class)
class ScrapingEndToEndTest {

    private val pagesByPath = mutableMapOf<String, MockResponse>()
    private val server = MockWebServer()
    private lateinit var env: TestEnvironment
    private lateinit var scrape: ScrapeArticleUseCase
    private var gameId = 0L

    // PNG valid dibuat lewat Bitmap (bukan string base64 tulisan tangan).
    private val pixelPng: ByteArray = ByteArrayOutputStream().also {
        Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
    }.toByteArray()

    private val articleHtml = """
        <html><body>
          <nav>Menu Utama</nav>
          <div class="ad">BELI SEKARANG</div>
          <article>
            <h1>Walkthrough Disc 1</h1>
            <p>Mulai dari desa Estard dan bicara dengan semua orang.</p>
            <h2>Item penting</h2>
            <ul><li>Herb</li><li>Antidote</li></ul>
            <table><tr><td>HP</td><td>120</td></tr></table>
            <img src="/peta.png" alt="Peta Estard">
            <div id="comments"><p>komentar spam</p></div>
          </article>
          <footer>hak cipta</footer>
        </body></html>
    """.trimIndent()

    @Before
    fun setUp() {
        env = TestEnvironment(timeoutSeconds = 2)
        scrape = ScrapeArticleUseCase(env.pages, env.scraper)
        gameId = runBlocking { env.games.findOrCreate("Dragon Quest VII") }
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                pagesByPath[request.path] ?: MockResponse().setResponseCode(404)
        }
        server.start()
        pagesByPath["/walkthrough"] = MockResponse().setBody(articleHtml).setHeader("Content-Type", "text/html")
        pagesByPath["/peta.png"] = MockResponse()
            .setBody(Buffer().write(pixelPng))
            .setHeader("Content-Type", "image/png")
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        env.strings("SELECT filename FROM images").forEach(env.imageStore::delete)
        env.close()
    }

    private fun url(path: String) = server.url(path).toString()

    private fun article(title: String) = runBlocking { env.articles.observeArticles(title, null).first().single() }

    @Test(timeout = 60_000)
    fun satuUrlTersimpanLokalDanBisaDibacaTanpaKoneksi() = runBlocking {
        val articleId = env.articles.insertArticle(gameId, "Walkthrough", listOf(url("/walkthrough")))

        scrape(articleId)

        // Konten utama tersimpan berurutan; iklan, navigasi, komentar, dan footer tidak ikut.
        assertEquals(
            listOf("h1", "p", "h2", "li", "li", "table", "img"),
            env.strings("SELECT node_type FROM content_nodes ORDER BY display_order"),
        )
        assertEquals(
            listOf(
                "Walkthrough Disc 1",
                "Mulai dari desa Estard dan bicara dengan semua orang.",
                "Item penting",
                "Herb",
                "Antidote",
                // teks alt gambar
                "Peta Estard",
            ),
            env.strings("SELECT text_content FROM content_nodes WHERE text_content IS NOT NULL ORDER BY display_order"),
        )
        // JSON meng-escape "/" ("<\/td>"), jadi di-parse dulu sebelum dicocokkan.
        val tableJson = env.strings("SELECT metadata_json FROM content_nodes WHERE node_type = 'table'").single()
        assertTrue(JSONObject(tableJson).getString("html").contains("<td>HP</td>"))
        assertEquals(1, env.count("SELECT COUNT(*) FROM images WHERE article_id = $articleId"))
        val imagePath = env.strings("SELECT file_path FROM images").single()
        assertTrue(java.io.File(imagePath).length() > 0)
        assertEquals(1, env.count("SELECT is_scraping_done FROM articles WHERE id = $articleId"))
        assertEquals(ScrapingStatus.READY, article("Walkthrough").status)

        // Server mati: artikel, konten, dan gambar tetap terbaca dari penyimpanan lokal.
        server.shutdown()
        assertEquals(ScrapingStatus.READY, article("Walkthrough").status)
        assertEquals(7, env.count("SELECT COUNT(*) FROM content_nodes WHERE article_id = $articleId"))
        assertTrue(java.io.File(imagePath).exists())
    }

    @Test(timeout = 60_000)
    fun urlGagalDiaksesDicatatDenganAlasanJelasLaluRetryMenyelesaikannya() = runBlocking {
        pagesByPath["/timeout"] = MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        pagesByPath["/forbidden"] = MockResponse().setResponseCode(403)
        pagesByPath["/kosong"] = MockResponse().setBody("<html><body><nav>Menu</nav></body></html>")
        val unreachable = "http://127.0.0.1:1/tidak-ada" // port tertutup: koneksi ditolak seketika
        val urls =
            listOf(
                url("/missing"),
                url("/forbidden"),
                url("/timeout"),
                url("/kosong"),
                unreachable,
                url("/walkthrough"),
            )
        val articleId = env.articles.insertArticle(gameId, "Campuran", urls)

        scrape(articleId) // tidak boleh crash maupun menggantung

        val pages = article("Campuran").pages
        assertEquals(
            listOf(
                PageFailure.Http(404),
                PageFailure.Http(403),
                PageFailure.Timeout,
                PageFailure.EmptyContent,
                PageFailure.ConnectionFailed,
                null,
            ),
            pages.map { it.failure },
        )
        assertEquals(ScrapingStatus.READY, article("Campuran").status) // partial: satu halaman berhasil

        // Perbaiki halaman 404 lalu retry: hanya halaman yang gagal diulang, halaman sukses tidak disentuh.
        pagesByPath["/missing"] = MockResponse().setBody(articleHtml).setHeader("Content-Type", "text/html")
        val nodesBefore = env.count("SELECT COUNT(*) FROM content_nodes WHERE source_page_id = ${pages.last().id}")
        assertEquals(5, env.articles.retryFailedPages(articleId))
        assertEquals(ScrapingStatus.SCRAPING, article("Campuran").status)
        scrape(articleId)

        val after = article("Campuran").pages
        assertEquals(null, after.first().failure)
        assertEquals(PageFailure.Http(403), after[1].failure)
        assertEquals(
            nodesBefore,
            env.count("SELECT COUNT(*) FROM content_nodes WHERE source_page_id = ${pages.last().id}"),
        )
        assertTrue(env.count("SELECT COUNT(*) FROM content_nodes WHERE source_page_id = ${pages.first().id}") > 0)
    }
}
