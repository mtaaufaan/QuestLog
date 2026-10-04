package com.rds.questlog.domain.model

enum class SourcePageStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }

/** Alasan sebuah halaman gagal diunduh; ditampilkan di Scrape Error Dialog. */
sealed interface PageFailure {
    data class Http(val code: Int) : PageFailure
    data object Timeout : PageFailure
    data object ConnectionFailed : PageFailure

    /** Halaman berhasil diambil tapi tidak menghasilkan konten. */
    data object EmptyContent : PageFailure

    /** Melebihi [ScrapeLimits.MAX_NODES_PER_PAGE] blok. */
    data object TooLong : PageFailure
    data object Unknown : PageFailure
}

data class SourcePage(
    val id: Long,
    val url: String,
    /** Urutan halaman dalam artikel, mulai dari 1. */
    val order: Int,
    val status: SourcePageStatus,
    val failure: PageFailure? = null,
)
