package com.rds.questlog.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.scraper.ScrapingResult
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** URL yang langsung berupa gambar (PNG/SVG) menjadi artikel berisi satu gambar; gambar rusak gagal dengan jelas. */
@RunWith(AndroidJUnit4::class)
class DirectImageScrapingTest {

    private val server = MockWebServer()
    private lateinit var env: TestEnvironment

    private val svg =
        """<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40"><circle cx="20" cy="20" r="10"/></svg>"""

    private val png: ByteArray = ByteArrayOutputStream().also {
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
    }.toByteArray()

    @Before
    fun setUp() {
        env = TestEnvironment()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path?.substringBefore('?')) {
                "/Brosen_windrose.svg" -> MockResponse().setBody(svg).setHeader("Content-Type", "image/svg+xml")
                "/peta-desa.png" -> MockResponse().setBody(Buffer().write(png)).setHeader("Content-Type", "image/png")
                "/rusak.png" -> MockResponse().setBody("bukan gambar").setHeader("Content-Type", "image/png")
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        env.strings("SELECT filename FROM images").forEach(env.imageStore::delete)
        env.close()
    }

    private fun scrape(path: String) = runBlocking { env.scraper.scrape(server.url(path).toString()) }

    @Test(timeout = 30_000)
    fun svgLangsungDisimpanApaAdanyaDenganKeteranganDariNamaFile() {
        val result = scrape("/Brosen_windrose.svg?utm_source=en.wikipedia.org") as ScrapingResult.Success

        val node = result.nodes.single()
        assertEquals(NodeType.IMG, node.type)
        assertEquals("Brosen windrose", node.text)
        val image = result.images.single()
        assertTrue(image.filename.endsWith(".svg"))
        assertEquals(svg, File(image.filePath).readText())
        File(image.filePath).delete()
    }

    @Test(timeout = 30_000)
    fun pngLangsungDikompresMenjadiWebp() {
        val result = scrape("/peta-desa.png") as ScrapingResult.Success

        assertEquals("peta desa", result.nodes.single().text)
        assertTrue(result.images.single().filename.endsWith(".webp"))
        File(result.images.single().filePath).delete()
    }

    @Test(timeout = 30_000)
    fun gambarRusakGagalSebagaiKontenKosong() {
        val result = scrape("/rusak.png") as ScrapingResult.Failure

        assertEquals(PageFailure.EmptyContent, result.failure)
    }
}
