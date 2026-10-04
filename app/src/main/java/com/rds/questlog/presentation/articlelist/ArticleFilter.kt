package com.rds.questlog.presentation.articlelist

import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameFilterOption
import com.rds.questlog.presentation.model.GameUiModel

/**
 * Search (QL-10) dan filter game (QL-11) digabung: judul memuat [query] (case-insensitive, partial, spasi
 * pinggir diabaikan) DAN, bila [filterGameId] tidak null, artikel milik game itu.
 */
fun filterArticles(articles: List<ArticleUiModel>, query: String, filterGameId: Long?): List<ArticleUiModel> {
    val q = query.trim()
    return articles.filter { article ->
        (filterGameId == null || article.gameId == filterGameId) &&
            (q.isEmpty() || article.title.contains(q, ignoreCase = true))
    }
}

/** Opsi Game Filter Sheet: tiap game beserta jumlah artikelnya (dihitung dari seluruh artikel, bukan hasil search). */
fun gameFilterOptions(games: List<GameUiModel>, articles: List<ArticleUiModel>): List<GameFilterOption> =
    games.map { game -> GameFilterOption(game.id, game.name, articles.count { it.gameId == game.id }) }
