package com.rds.questlog.data.scraper

import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.scraper.PaginationDetector
import com.rds.questlog.domain.scraper.PaginationResult
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Mengambil satu halaman dengan klien OkHttp yang sama dengan scraper (timeout sama) lalu meminta
 * [PaginationLinkParser] mengenali tautan lanjutannya. Hanya halaman [PaginationDetector.detect] itu yang diambil.
 */
class KsoupPaginationDetector @Inject constructor(
    private val client: OkHttpClient,
    private val parser: PaginationLinkParser,
) : PaginationDetector {

    override suspend fun detect(url: String): PaginationResult = withContext(Dispatchers.IO) {
        val request = try {
            Request.Builder().url(
                url,
            ).header("User-Agent", BROWSER_USER_AGENT).header("Accept", "text/html,*/*").build()
        } catch (_: IllegalArgumentException) {
            return@withContext PaginationResult.Failure(PageFailure.Unknown)
        }
        try {
            client.newCall(request).execute().use { response ->
                when {
                    !response.isSuccessful -> PaginationResult.Failure(PageFailure.Http(response.code))
                    response.body?.contentType()?.type == "image" -> PaginationResult.Links(emptyList())
                    else -> PaginationResult.Links(parser.parse(response.body?.string().orEmpty(), url))
                }
            }
        } catch (_: InterruptedIOException) {
            PaginationResult.Failure(PageFailure.Timeout)
        } catch (_: IOException) {
            PaginationResult.Failure(PageFailure.ConnectionFailed)
        }
    }
}
