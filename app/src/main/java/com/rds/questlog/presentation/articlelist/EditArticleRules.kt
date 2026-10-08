package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameUiModel

private const val FREE_GAME_LIMIT = 2
private const val FREE_ARTICLES_PER_GAME = 5

/** Isi form Edit Article Sheet. [gameId] null dan [newGame] false = game belum dipilih. */
data class EditArticleForm(
    val title: String,
    val gameInput: String,
    val gameId: Long?,
    val newGame: Boolean = false,
) {
    /** Game sudah valid: dipilih dari daftar atau akan dibuat baru. */
    val hasGame: Boolean get() = gameId != null || newGame

    companion object {
        /** Nilai awal: judul dan game artikel saat ini. */
        fun of(article: ArticleUiModel, games: List<GameUiModel>) = EditArticleForm(
            title = article.title,
            gameInput = games.firstOrNull { it.id == article.gameId }?.name ?: article.gameName,
            gameId = article.gameId,
        )
    }
}

/** Hasil Edit Article Sheet: persis satu dari [gameId] / [newGameName] terisi (sama dengan payload S2). */
data class EditArticlePayload(val title: String, val gameId: Long?, val newGameName: String?)

/** Pelanggaran limit tier gratis saat memindahkan artikel (component-contract.md §12). */
enum class EditLimit { GAME, ARTICLES }

/** Artikel lain di game [gameId]; [GameUiModel.articleCount] sudah mencakup artikel ini bila ia di game itu. */
private fun othersIn(gameId: Long, article: ArticleUiModel, games: List<GameUiModel>): Int =
    (games.firstOrNull { it.id == gameId }?.articleCount ?: 0) - if (gameId == article.gameId) 1 else 0

/** Game asal akan kosong setelah artikel ini pindah; game kosong dihapus otomatis dan tidak dihitung ke limit. */
fun oldGameBecomesEmpty(article: ArticleUiModel, games: List<GameUiModel>): Boolean =
    othersIn(article.gameId, article, games) == 0

/** Limit yang dilanggar oleh pilihan di [form]; null bila aman atau tier premium. */
fun editLimit(
    article: ArticleUiModel,
    games: List<GameUiModel>,
    form: EditArticleForm,
    isPremium: Boolean,
): EditLimit? {
    if (isPremium) return null
    val gamesAfter = games.size - if (oldGameBecomesEmpty(article, games)) 1 else 0
    return when {
        form.newGame && gamesAfter >= FREE_GAME_LIMIT -> EditLimit.GAME
        form.gameId != null && form.gameId != article.gameId &&
            othersIn(form.gameId, article, games) >= FREE_ARTICLES_PER_GAME -> EditLimit.ARTICLES
        else -> null
    }
}
