package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.error.QuestLogError.DatabaseError
import com.rds.questlog.domain.error.QuestLogError.TierError
import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.usecase.premium.ObservePremiumStatusUseCase
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateArticleUseCase @Inject constructor(
    private val articles: ArticleRepository,
    private val management: ArticleManagementRepository,
    private val games: GameRepository,
    private val premium: ObservePremiumStatusUseCase,
) {

    /**
     * Mengubah judul artikel dan/atau memindahkannya ke game lain (QL-18). Persis satu dari [gameId] (game yang
     * sudah ada, boleh game artikel sekarang) atau [newGameName] (game baru) harus terisi. Judul di-trim dan tidak
     * boleh kosong; nama game baru tidak boleh kosong atau lebih dari 100 karakter.
     *
     * Pengguna gratis dibatasi 2 game dan 5 artikel per game; game asal yang akan kosong dan dihapus otomatis
     * tidak dihitung ke limit game. Artikel yang sedang SCRAPING boleh diubah: judul dan game tidak dibaca worker.
     * Halaman, konten, dan checkpoint tidak berubah. Atomik; gagal → `Result.failure` berisi ValidationError /
     * TierError / DatabaseError.WriteFailed dan tidak ada yang berubah.
     */
    suspend operator fun invoke(articleId: Long, title: String, gameId: Long?, newGameName: String?): Result<Unit> =
        writing {
            val cleanTitle = title.trim()
            val name = newGameName?.trim()
            validate(cleanTitle, gameId, name)?.let { throw it }
            val article = articles.observeArticle(articleId).first() ?: throw DatabaseError.WriteFailed()
            val allGames = games.observeGames().first()
            val target = resolveTarget(article.gameId, gameId, name, allGames)
            if (!premium().first()) enforceTier(article.gameId, target, allGames)
            management.updateDetails(articleId, cleanTitle, target)
        }

    private fun validate(title: String, gameId: Long?, newGameName: String?): ValidationError? = when {
        title.isEmpty() -> ValidationError.EmptyTitle
        gameId == null && newGameName.isNullOrEmpty() -> ValidationError.EmptyGameName
        (newGameName?.length ?: 0) > MAX_GAME_NAME -> ValidationError.GameNameTooLong
        else -> null
    }

    private fun resolveTarget(currentGameId: Long, gameId: Long?, newName: String?, all: List<Game>): GameTarget {
        require(gameId == null || newName == null) { "Isi gameId atau newGameName, bukan keduanya" }
        // Nama yang sudah dipakai game lain berarti memilih game itu, bukan membuat duplikat.
        val chosen = gameId ?: all.firstOrNull { it.name.equals(newName, ignoreCase = true) }?.id
        return when {
            chosen == currentGameId -> GameTarget.Unchanged
            chosen != null -> GameTarget.Existing(chosen)
            else -> GameTarget.New(requireNotNull(newName))
        }
    }

    private fun enforceTier(currentGameId: Long, target: GameTarget, all: List<Game>) {
        val originEmpties = (all.firstOrNull { it.id == currentGameId }?.articleCount ?: 0) <= 1
        when (target) {
            GameTarget.Unchanged -> Unit
            is GameTarget.New -> if (all.size - (if (originEmpties) 1 else 0) >= FREE_GAME_LIMIT) {
                throw TierError.GameLimitReached
            }
            is GameTarget.Existing -> if ((all.firstOrNull { it.id == target.gameId }?.articleCount ?: 0) >=
                FREE_ARTICLES_PER_GAME
            ) {
                throw TierError.ArticleLimitReached
            }
        }
    }

    private companion object {
        const val MAX_GAME_NAME = 100
        const val FREE_GAME_LIMIT = 2
        const val FREE_ARTICLES_PER_GAME = 5
    }
}
