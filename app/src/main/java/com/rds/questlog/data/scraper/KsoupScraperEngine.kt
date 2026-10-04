package com.rds.questlog.data.scraper

import com.rds.questlog.domain.model.NodeType
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.scraper.ScraperEngine
import com.rds.questlog.domain.scraper.ScrapingResult
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Mengganti User-Agent bawaan OkHttp: sebagian situs (mis. GameFAQs) menolaknya dengan 403. */
internal const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

/** Scraper satu URL: OkHttp mengunduh HTML, [HtmlContentParser] mengekstrak, [ImageDownloader] menyimpan gambar. */
class KsoupScraperEngine @Inject constructor(
    private val client: OkHttpClient,
    private val parser: HtmlContentParser,
    private val images: ImageDownloader,
) : ScraperEngine {

    override suspend fun scrape(url: String): ScrapingResult = withContext(Dispatchers.IO) {
        when (val page = fetch(url)) {
            is Fetched.Failed -> ScrapingResult.Failure(page.failure)
            is Fetched.Html -> toResult(parser.parse(page.html, url))
            is Fetched.Image -> toImageResult(url, page.bytes)
        }
    }

    /** URL yang langsung berupa gambar: satu blok gambar (keterangan dari nama file) menjadi isi halamannya. */
    private fun toImageResult(url: String, bytes: ByteArray): ScrapingResult {
        val image = images.save(url, bytes) ?: return ScrapingResult.Failure(PageFailure.EmptyContent)
        val alt = imageCaption(url)
        val metadata = JSONObject().put("path", image.filePath).put("alt", alt)
        return ScrapingResult.Success(listOf(ScrapedNode(NodeType.IMG, alt, metadata.toString())), listOf(image))
    }

    /** Nama file dari URL tanpa ekstensi, garis bawah/strip jadi spasi ("Brosen_windrose.svg" -> "Brosen windrose"). */
    private fun imageCaption(url: String): String = url.substringBefore('?').substringBefore('#')
        .substringAfterLast('/').substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()

    private suspend fun toResult(parsed: List<ParsedNode>): ScrapingResult {
        val nodes = mutableListOf<ScrapedNode>()
        val saved = mutableListOf<ScrapedImage>()
        for (node in parsed) {
            when (node.type) {
                NodeType.IMG -> images.download(node.imageUrl.orEmpty())?.let { image ->
                    saved += image
                    val metadata = JSONObject().put("path", image.filePath).put("alt", node.imageAlt.orEmpty())
                    nodes += ScrapedNode(NodeType.IMG, node.imageAlt, metadata.toString())
                }
                NodeType.TABLE -> {
                    val metadata = JSONObject().put("html", node.tableHtml.orEmpty())
                    nodes += ScrapedNode(NodeType.TABLE, null, metadata.toString())
                }
                else -> nodes += ScrapedNode(node.type, node.text)
            }
        }
        return ScrapingResult.Success(nodes, saved.distinctBy { it.filename })
    }

    @Suppress("ReturnCount")
    private fun fetch(url: String): Fetched {
        val request = try {
            Request.Builder().url(
                url,
            ).header("User-Agent", BROWSER_USER_AGENT).header("Accept", "text/html,*/*").build()
        } catch (_: IllegalArgumentException) {
            return Fetched.Failed(PageFailure.Unknown)
        }
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful && response.body?.contentType()?.type == "image") {
                    Fetched.Image(response.body?.bytes() ?: ByteArray(0))
                } else if (response.isSuccessful) {
                    Fetched.Html(response.body?.string().orEmpty())
                } else {
                    Fetched.Failed(PageFailure.Http(response.code))
                }
            }
        } catch (_: InterruptedIOException) {
            Fetched.Failed(PageFailure.Timeout)
        } catch (_: IOException) {
            Fetched.Failed(PageFailure.ConnectionFailed)
        }
    }

    private sealed interface Fetched {
        data class Html(val html: String) : Fetched
        class Image(val bytes: ByteArray) : Fetched
        data class Failed(val failure: PageFailure) : Fetched
    }
}
