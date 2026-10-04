package com.rds.questlog.presentation.model

/** Bentuk data yang mengalir ke layar (component-contract.md §0). */
enum class PageStatus { PENDING, DONE, FAILED }

enum class ArticleStatus { SCRAPING, READY, ERROR }

enum class ReadMode { SEAMLESS, PAGED }

data class GameUiModel(
    val id: Long,
    val name: String,
    /** Jumlah artikel game ini; jumlahnya dipakai untuk total "walkthrough" dan opsi Game Filter Sheet. */
    val articleCount: Int = 0,
)

/** Opsi di Game Filter Sheet; [count] = jumlah artikel game itu (component-contract.md §10). */
data class GameFilterOption(
    val id: Long,
    val name: String,
    val count: Int,
)

data class PageUiModel(
    val url: String,
    val status: PageStatus,
    /** Alasan gagal, mis. "HTTP 404 — halaman tidak ditemukan". */
    val reason: String? = null,
)

data class ArticleUiModel(
    val id: Long,
    val gameId: Long,
    val gameName: String,
    val title: String,
    val status: ArticleStatus,
    val pages: List<PageUiModel>,
    /** Penanda manual, 1 per artikel. Terpisah dari [lastNodeId]. */
    val checkpointNodeId: Long? = null,
    /** Posisi baca terakhir (auto-resume). */
    val lastNodeId: Long? = null,
    /** Label relatif, mis. "dibaca 2 jam lalu". */
    val lastRead: String? = null,
    val readMode: ReadMode? = null,
) {
    val totalPageCount: Int get() = pages.size
    val failedPageCount: Int get() = pages.count { it.status == PageStatus.FAILED }
    val donePageCount: Int get() = pages.count { it.status == PageStatus.DONE }

    /** Halaman yang sudah selesai diproses (sukses + gagal); dasar progress "Memproses… n/total". */
    val handledPageCount: Int get() = pages.count { it.status != PageStatus.PENDING }

    /** READY tapi ada halaman gagal: badge "X halaman gagal" dan banner retry di Reader. */
    val isPartial: Boolean get() = status == ArticleStatus.READY && failedPageCount > 0
}
