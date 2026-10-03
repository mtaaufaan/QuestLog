package com.rds.questlog.presentation.articlelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.popup.Popup

/**
 * S1. Tap artikel: READY → Reader, SCRAPING → snackbar, ERROR → Scrape Error Dialog.
 * Popup dibuka lewat [onShowPopup] (state `activePopup` bersama di NavHost, screen_flow.md §4).
 */
@Composable
fun ArticleListScreen(
    navController: NavController,
    onShowPopup: (Popup) -> Unit,
    viewModel: ArticleListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ArticleListContent(
        games = uiState.games,
        articles = uiState.articles,
        isPremium = uiState.isPremium,
        query = uiState.query,
        filterGameId = uiState.selectedGameFilter,
        snackbar = uiState.snackbar?.let { stringResource(it) },
        onQuery = viewModel::onQueryChange,
        onOpenFilter = { onShowPopup(Popup.GameFilter) },
        onArticleTap = { article ->
            when (article.status) {
                ArticleStatus.READY -> navController.openReader(article, resumeFrom = "last")
                ArticleStatus.SCRAPING -> viewModel.onScrapingArticleTapped()
                ArticleStatus.ERROR -> onShowPopup(Popup.ScrapeError(article.id))
            }
        },
        onResume = { navController.openReader(it, resumeFrom = "checkpoint") },
        onOpenActions = { onShowPopup(Popup.ArticleActions(it.id)) },
        onAdd = { onShowPopup(Popup.ScrapeMode) },
        onOpenUnlock = { onShowPopup(Popup.Unlock) },
    )
}

private fun NavController.openReader(article: ArticleUiModel, resumeFrom: String) {
    navigate(Reader(articleId = article.id, resumeFrom = resumeFrom))
}
