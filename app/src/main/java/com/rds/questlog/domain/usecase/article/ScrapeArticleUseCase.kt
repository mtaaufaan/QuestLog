package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapeLimits
import com.rds.questlog.domain.repository.SourcePageRepository
import com.rds.questlog.domain.scraper.ScraperEngine
import com.rds.questlog.domain.scraper.ScrapingResult
import javax.inject.Inject

class ScrapeArticleUseCase @Inject constructor(
    private val pages: SourcePageRepository,
    private val scraper: ScraperEngine,
) {

    /**
     * Memproses halaman PENDING milik [articleId] satu per satu (sequential agar tidak terkena rate-limit):
     * sukses → konten tersimpan dan halaman COMPLETED; gagal (jaringan/HTTP/kosong/terlalu panjang) → halaman
     * FAILED beserta alasannya. Kegagalan satu halaman tidak menghentikan halaman lain. Halaman yang tertinggal
     * IN_PROGRESS dari proses yang mati dipulihkan lebih dulu. Kegagalan database dilempar ke pemanggil (worker).
     */
    suspend operator fun invoke(articleId: Long) {
        pages.recoverInterrupted(articleId)
        for (page in pages.getPendingPages(articleId)) {
            pages.markInProgress(page.id)
            when (val result = scraper.scrape(page.url)) {
                is ScrapingResult.Failure -> pages.markFailed(page.id, result.failure)
                is ScrapingResult.Success -> when {
                    result.nodes.isEmpty() -> pages.markFailed(page.id, PageFailure.EmptyContent)
                    result.nodes.size > ScrapeLimits.MAX_NODES_PER_PAGE -> pages.markFailed(
                        page.id,
                        PageFailure.TooLong,
                    )
                    else -> pages.saveScrapedPage(articleId, page.id, result.nodes, result.images)
                }
            }
        }
        pages.updateScrapingDone(articleId)
    }
}
