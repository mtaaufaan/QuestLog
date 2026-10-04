package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class RetryFailedPagesUseCase @Inject constructor(
    private val repository: ArticleRepository,
    private val scheduler: ScrapeScheduler,
) {

    /**
     * Mengulang SEMUA halaman FAILED milik artikel (FAILED → PENDING, artikel kembali SCRAPING) lalu menjadwalkan
     * scraping. Halaman COMPLETED tidak disentuh. Mengembalikan jumlah halaman yang diulang (0 = tidak ada yang gagal,
     * tidak ada yang dijadwalkan). Gagal → `Result.failure(DatabaseError.WriteFailed)`.
     */
    suspend operator fun invoke(articleId: Long): Result<Int> = writing {
        val reset = repository.retryFailedPages(articleId)
        if (reset > 0) scheduler.schedule(articleId)
        reset
    }
}
