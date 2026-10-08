package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.error.QuestLogError.DatabaseError
import com.rds.questlog.domain.model.ScrapingStatus
import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class RefreshArticleUseCase @Inject constructor(
    private val articles: ArticleRepository,
    private val management: ArticleManagementRepository,
    private val scheduler: ScrapeScheduler,
) {

    /**
     * Mengunduh ulang seluruh halaman artikel dari URL aslinya (QL-20). Semua halaman ditandai PENDING lalu worker
     * dijadwalkan; isi lama TIDAK dihapus di sini dan baru diganti per halaman setelah unduhannya sukses (gagal →
     * halaman FAILED, isi lama utuh). Checkpoint dan posisi baca dipertahankan.
     * Ditolak bila artikel masih diproses (ArticleError.Busy). Gagal → `Result.failure`; tidak dijadwalkan.
     */
    suspend operator fun invoke(articleId: Long): Result<Unit> = writing {
        requireIdle(articles, articleId)
        management.markPagesForRefresh(articleId)
        scheduler.schedule(articleId)
    }
}

/** Artikel harus ada dan tidak sedang diproses; dipakai bersama oleh unduh ulang artikel dan halaman. */
internal suspend fun requireIdle(articles: ArticleRepository, articleId: Long) {
    val article = articles.observeArticle(articleId).first() ?: throw DatabaseError.WriteFailed()
    if (article.status == ScrapingStatus.SCRAPING) throw ArticleError.Busy
}
