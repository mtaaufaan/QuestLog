package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SaveArticleUseCase @Inject constructor(
    private val games: GameRepository,
    private val articles: ArticleRepository,
    private val scheduler: ScrapeScheduler,
    private val premium: ObservePremiumStatusUseCase,
) {

    /**
     * Menyimpan artikel baru di game [gameName] (dibuat bila belum ada) lalu menjadwalkan scraping [urls].
     * Validasi: nama game dan judul tidak kosong, nama game ≤ 100 karakter, 1-10 URL https:// tanpa duplikat,
     * dan belum ada yang tersimpan di artikel mana pun. Pengguna gratis dibatasi 2 game dan 5 artikel per game
     * (QL-12); batas ditegakkan di sini, bukan di database.
     * Mengembalikan id artikel, atau Result.failure berisi ValidationError / TierError / DatabaseError.WriteFailed.
     * Tidak ada yang tersimpan atau dijadwalkan bila gagal.
     */
    suspend operator fun invoke(gameName: String, title: String, urls: List<String>): Result<Long> {
        val name = gameName.trim()
        val clean = urls.map { it.trim() }
        return writing {
            validate(name, title.trim(), clean)?.let { throw it }
            articles.requireNotStored(clean)
            enforceTier(name)
            val gameId = games.findOrCreate(name)
            val articleId = articles.insertArticle(gameId, title.trim(), clean)
            scheduler.schedule(articleId)
            articleId
        }
    }

    private suspend fun enforceTier(gameName: String) {
        if (premium().first()) return
        val existing = games.findIdByName(gameName)
        when {
            existing == null && games.count() >= FREE_GAME_LIMIT -> throw TierError.GameLimitReached
            existing != null && articles.countForGame(existing) >= FREE_ARTICLES_PER_GAME ->
                throw TierError.ArticleLimitReached
        }
    }

    private fun validate(gameName: String, title: String, urls: List<String>): ValidationError? = when {
        gameName.isEmpty() -> ValidationError.EmptyGameName
        gameName.length > MAX_GAME_NAME -> ValidationError.GameNameTooLong
        title.isEmpty() -> ValidationError.EmptyTitle
        else -> validateUrls(urls)
    }

    private companion object {
        const val MAX_GAME_NAME = 100
        const val FREE_GAME_LIMIT = 2
        const val FREE_ARTICLES_PER_GAME = 5
    }
}
