package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class RefreshPageUseCase @Inject constructor(
    private val articles: ArticleRepository,
    private val management: ArticleManagementRepository,
    private val scheduler: ScrapeScheduler,
) {

    /**
     * Seperti [RefreshArticleUseCase] tetapi hanya untuk satu halaman (tombol ⟳ di Page Manager): halaman lain
     * tidak disentuh. Ditolak bila artikel masih diproses (ArticleError.Busy) atau halaman bukan milik artikel
     * (ArticleError.PageNotFound).
     */
    suspend operator fun invoke(articleId: Long, pageId: Long): Result<Unit> = writing {
        requireIdle(articles, articleId)
        management.markPagesForRefresh(articleId, pageId)
        scheduler.schedule(articleId)
    }
}
