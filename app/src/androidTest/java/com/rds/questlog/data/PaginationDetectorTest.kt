package com.rds.questlog.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rds.questlog.data.scraper.KsoupPaginationDetector
import com.rds.questlog.data.scraper.PaginationLinkParser
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.scraper.PaginationResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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

/** QL-34: pengambilan satu halaman lewat OkHttp (timeout seperti scraper), pengenalan tautan, dan kegagalan jaringan. */
@RunWith(AndroidJUnit4::class)
class PaginationDetectorTest {

    private val server = MockWebServer()
    private val hits = mutableListOf<String>()
    private var respond: (String) -> MockResponse = { MockResponse().setResponseCode(404) }
    private lateinit var detector: KsoupPaginationDetector

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                hits += request.path.orEmpty()
                return respond(request.path.orEmpty())
            }
        }
        server.start()
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
        detector = KsoupPaginationDetector(client, PaginationLinkParser())
    }

    @After
    fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun html(body: String) =
        MockResponse().setHeader("Content-Type", "text/html").setBody("<html><body>$body</body></html>")

    @Test
    fun hanyaHalamanPertamaDiambilDanTautanLanjutanDikenaliBerurutan() = runBlocking {
        respond = {
            html("<a href=\"/guide?page=3\">3</a><a href=\"/guide?page=2\">2</a><a href=\"/guide?page=2\">Next</a>")
        }
        val url = server.url("/guide?page=1").toString()

        val result = detector.detect(url) as PaginationResult.Links

        assertEquals(
            listOf(server.url("/guide?page=2").toString(), server.url("/guide?page=3").toString()),
            result.urls,
        )
        // Halaman usulan tidak pernah diambil sebelum pengguna mengonfirmasi.
        assertEquals(listOf("/guide?page=1"), hits)
    }

    @Test
    fun halamanTanpaTautanLanjutanBukanKegagalan() = runBlocking {
        respond = { html("<p>Tidak ada navigasi.</p>") }

        val result = detector.detect(server.url("/guide").toString()) as PaginationResult.Links

        assertTrue(result.urls.isEmpty())
    }

    @Test
    fun statusHttpGagalMenjadiFailureHttp() = runBlocking {
        respond = { MockResponse().setResponseCode(503) }

        val result = detector.detect(server.url("/guide").toString())

        assertEquals(PaginationResult.Failure(PageFailure.Http(503)), result)
    }

    @Test
    fun serverTidakMeresponsMenjadiTimeout() = runBlocking {
        respond = { html("lambat").setBodyDelay(5, TimeUnit.SECONDS) }

        val result = detector.detect(server.url("/guide").toString())

        assertEquals(PaginationResult.Failure(PageFailure.Timeout), result)
    }

    @Test
    fun koneksiGagalMenjadiConnectionFailed() = runBlocking {
        val url = server.url("/guide").toString()
        server.shutdown()

        val result = detector.detect(url)

        assertEquals(PaginationResult.Failure(PageFailure.ConnectionFailed), result)
    }

    @Test
    fun urlRusakMenjadiFailureTanpaMelempar() = runBlocking {
        assertEquals(PaginationResult.Failure(PageFailure.Unknown), detector.detect("bukan url"))
    }
}
