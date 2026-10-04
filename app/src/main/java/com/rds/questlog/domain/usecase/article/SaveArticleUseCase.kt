package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SaveArticleUseCase @Inject constructor(
    private val games: GameRepository,
    private val articles: ArticleRepository,
    private val scheduler: ScrapeScheduler,
) {

    /**
     * Menyimpan artikel baru di game [gameName] (dibuat bila belum ada) lalu menjadwalkan scraping [urls].
     * Validasi: nama game dan judul tidak kosong, nama game ≤ 100 karakter, 1-10 URL, semuanya diawali `https://`.
     * Mengembalikan id artikel, atau `Result.failure` berisi `ValidationError` / `DatabaseError.WriteFailed`.
     * Batas tier gratis (QL-12) belum ditegakkan di sini.
     */
    suspend operator fun invoke(gameName: String, title: String, urls: List<String>): Result<Long> {
        validate(gameName.trim(), title.trim(), urls.map { it.trim() })?.let { return Result.failure(it) }
        return writing {
            val gameId = games.findOrCreate(gameName.trim())
            val articleId = articles.insertArticle(gameId, title.trim(), urls.map { it.trim() })
            scheduler.schedule(articleId)
            articleId
        }
    }

    private fun validate(gameName: String, title: String, urls: List<String>): ValidationError? = when {
        gameName.isEmpty() -> ValidationError.EmptyGameName
        gameName.length > MAX_GAME_NAME -> ValidationError.GameNameTooLong
        title.isEmpty() -> ValidationError.EmptyTitle
        urls.isEmpty() -> ValidationError.NoUrlsProvided
        urls.size > MAX_URLS -> ValidationError.TooManyUrls
        urls.any { !it.startsWith("https://", ignoreCase = true) } -> ValidationError.InvalidUrl
        else -> null
    }

    private companion object {
        const val MAX_GAME_NAME = 100
        const val MAX_URLS = 10
    }
}
