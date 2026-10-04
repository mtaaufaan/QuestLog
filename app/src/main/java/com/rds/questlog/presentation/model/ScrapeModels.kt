package com.rds.questlog.presentation.model

/** Dua cara menyimpan walkthrough (component-contract.md §2 dan §7: `'new' | 'append'`). */
enum class ScrapeMode(val route: String) {
    NEW("new"),
    APPEND("append"),
    ;

    companion object {
        /** Nilai `mode` pada route `AddArticle`; selain "append" dianggap [NEW]. */
        fun fromRoute(value: String): ScrapeMode = entries.firstOrNull { it.route == value } ?: NEW
    }
}

/** Isi form S2 yang dikirim lewat `onSave` (field persis sesuai prototipe `AddArticle.dc.html`). */
data class AddArticlePayload(
    val mode: ScrapeMode,
    /** Game yang sudah ada; null bila [newGameName] terisi atau mode [ScrapeMode.APPEND]. */
    val gameId: Long?,
    val newGameName: String?,
    val title: String,
    val urls: List<String>,
    /** Artikel yang dilengkapi pada mode [ScrapeMode.APPEND]. */
    val targetArticleId: Long?,
)

/**
 * Pekerjaan scraping yang baru dijadwalkan. [skipPages] = jumlah halaman yang sudah ada sebelumnya (mode Lengkapi),
 * agar progres notifikasi hanya menghitung halaman yang baru ditambahkan.
 */
data class ScrapeJob(val articleId: Long, val skipPages: Int)

/** Empat varian Scrape Notification (component-contract.md §8). */
enum class ScrapeNotificationKind { PROGRESS, DONE, PARTIAL, ERROR }

data class ScrapeNotificationUi(
    val kind: ScrapeNotificationKind,
    val articleTitle: String,
    /** Halaman yang sudah diproses (sukses + gagal). */
    val current: Int,
    val total: Int,
    val failedCount: Int = 0,
    val articleId: Long = 0,
)
