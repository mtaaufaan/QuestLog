package com.rds.questlog.presentation.articlelist

import android.content.Context
import androidx.annotation.StringRes
import com.rds.questlog.R
import com.rds.questlog.domain.model.PageFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Mengubah [PageFailure] menjadi teks untuk Scrape Error Dialog, mis. "HTTP 404 — halaman tidak ditemukan". */
class PageFailureFormatter @Inject constructor(@ApplicationContext private val context: Context) {

    fun format(failure: PageFailure): String = when (failure) {
        is PageFailure.Http ->
            context.getString(R.string.page_failure_http, failure.code, context.getString(httpReason(failure.code)))
        PageFailure.Timeout -> context.getString(R.string.page_failure_timeout)
        PageFailure.ConnectionFailed -> context.getString(R.string.page_failure_connection)
        PageFailure.EmptyContent -> context.getString(R.string.page_failure_empty)
        PageFailure.TooLong -> context.getString(R.string.page_failure_too_long)
        PageFailure.Unknown -> context.getString(R.string.page_failure_unknown)
    }

    @StringRes
    private fun httpReason(code: Int): Int = when (code) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> R.string.page_failure_http_forbidden
        HTTP_NOT_FOUND, HTTP_GONE -> R.string.page_failure_http_not_found
        in HTTP_SERVER_ERRORS -> R.string.page_failure_http_server
        else -> R.string.page_failure_http_other
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_GONE = 410
        val HTTP_SERVER_ERRORS = 500..599
    }
}
