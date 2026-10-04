package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class AppendPagesUseCase @Inject constructor(
    private val articles: ArticleRepository,
    private val scheduler: ScrapeScheduler,
) {

    /**
     * Mode "Lengkapi" (QL-3): menambahkan [urls] di akhir artikel [articleId] (append-only) lalu menjadwalkan scraping.
     * Validasi: 1-10 URL https://, tanpa duplikat dalam input, dan belum tersimpan di artikel mana pun (termasuk
     * artikel ini) — semuanya sebelum scraping dimulai. Halaman lama dan checkpoint tidak disentuh.
     * Gagal → Result.failure(ValidationError / DatabaseError.WriteFailed); tidak ada yang dijadwalkan.
     */
    suspend operator fun invoke(articleId: Long, urls: List<String>): Result<Unit> {
        val clean = urls.map { it.trim() }
        return writing {
            validateUrls(clean)?.let { throw it }
            articles.requireNotStored(clean)
            articles.appendPages(articleId, clean)
            scheduler.schedule(articleId)
        }
    }
}
