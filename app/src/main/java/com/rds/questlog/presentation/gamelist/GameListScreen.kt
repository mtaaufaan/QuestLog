package com.rds.questlog.presentation.gamelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.binder.BinderDummy
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.AddArticle
import com.rds.questlog.presentation.navigation.ArticleList
import com.rds.questlog.presentation.navigation.Binder
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.scrapemode.ScrapeModeSheet

/**
 * S0 Game List (home). Popup yang menempel padanya (Scrape Mode Sheet, Delete Game Dialog) dirender di sini;
 * [activePopup] tetap satu state bersama di NavHost.
 */
@Composable
fun GameListScreen(
    navController: NavController,
    activePopup: Popup?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
    pendingSharedUrl: String?,
    viewModel: GameListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = GameListActions(
        onQuery = viewModel::onQueryChange,
        onGameTap = { navController.navigate(ArticleList(it.id)) },
        onGameLongPress = { onShowPopup(Popup.DeleteGame(it.id)) },
        // Artikel utama masih dari penyimpanan sementara Binder; aturan lengkap (snackbar, checkpoint) di tahap wiring.
        onResume = { game ->
            BinderDummy.mainArticles[game.id]?.let {
                navController.navigate(Reader(articleId = it.articleId, resumeFrom = "checkpoint"))
            }
        },
        onOpenBinder = {
            if (state.premium == PremiumUiState.FREE) {
                onShowPopup(
                    Popup.Unlock,
                )
            } else {
                navController.navigate(Binder(it.id))
            }
        },
        onResultTap = { article ->
            when (article.status) {
                ArticleStatus.READY -> navController.navigate(Reader(articleId = article.id, resumeFrom = "last"))
                ArticleStatus.SCRAPING -> viewModel.onScrapingArticleTapped()
                ArticleStatus.ERROR -> navController.navigate(ArticleList(article.gameId))
            }
        },
        onAdd = { onShowPopup(Popup.ScrapeMode) },
        onOpenUnlock = { onShowPopup(Popup.Unlock) },
    )

    GameListContent(
        games = state.games.map { it.copy(mainArticleTitle = BinderDummy.mainArticles[it.id]?.title) },
        results = state.results,
        totalArticles = state.totalArticles,
        premium = state.premium,
        query = state.query,
        snackbar = state.snackbar?.let { stringResource(it) },
        actions = actions,
    )

    when (activePopup) {
        Popup.ScrapeMode -> ScrapeModeSheet(
            hasArticles = state.totalArticles > 0,
            onPick = {
                onDismissPopup()
                navController.navigate(AddArticle(mode = it.route, sharedUrl = pendingSharedUrl))
            },
            onClose = onDismissPopup,
        )
        is Popup.DeleteGame -> state.games.firstOrNull { it.id == activePopup.gameId }?.let { game ->
            DeleteGameDialog(
                gameName = game.name,
                articleCount = game.articleCount,
                onConfirm = {
                    viewModel.deleteGame(game.id)
                    onDismissPopup()
                },
                onClose = onDismissPopup,
            )
        }
        else -> Unit
    }
}
