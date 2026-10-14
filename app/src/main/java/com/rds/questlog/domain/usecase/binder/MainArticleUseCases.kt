package com.rds.questlog.domain.usecase.binder

import com.rds.questlog.domain.model.ContinueGameResult
import com.rds.questlog.domain.model.ScrapingStatus
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.GameProgressRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Menjadikan artikel utama game-nya (QL-31, gratis): satu per game, yang baru menimpa yang lama. Checkpoint dan posisi
 * terakhir artikel tidak tersentuh. Gagal -> `Result.failure(DatabaseError.WriteFailed)`.
 */
class SetMainArticleUseCase @Inject constructor(private val repository: GameProgressRepository) {
    /** Menjadikan [articleId] artikel utama game-nya. */
    suspend operator fun invoke(articleId: Long): Result<Unit> = writing { repository.setMainArticle(articleId) }
}

/**
 * Lanjutkan Game: membuka artikel utama [gameId]. Masih SCRAPING -> [ContinueGameResult.StillScraping]; penanda atau
 * artikelnya sudah tidak ada -> [ContinueGameResult.ArticleGone]; selain itu [ContinueGameResult.Open]. Posisi buka
 * (checkpoint, atau posisi terakhir bila belum ada checkpoint) ditentukan Reader.
 */
class ContinueGameUseCase @Inject constructor(
    private val progress: GameProgressRepository,
    private val articles: ArticleRepository,
) {
    /** Hasil Lanjutkan Game untuk [gameId]. */
    suspend operator fun invoke(gameId: Long): ContinueGameResult {
        val articleId = progress.mainArticleId(gameId)
        val article = articleId?.let { articles.observeArticle(it).first() }
        return when {
            articleId == null || article == null -> ContinueGameResult.ArticleGone
            article.status == ScrapingStatus.SCRAPING -> ContinueGameResult.StillScraping
            else -> ContinueGameResult.Open(articleId)
        }
    }
}
