package com.rds.questlog.presentation.articlelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.GameFilterOption
import com.rds.questlog.presentation.navigation.AddArticle
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.popup.Popup

/**
 * S1. Popup yang menempel padanya (GameFilter, ArticleActions, DeleteArticle, ScrapeError) dirender di sini;
 * [activePopup] tetap satu state bersama di NavHost (screen_flow.md §4).
 */
@Composable
fun ArticleListScreen(
    navController: NavController,
    activePopup: Popup?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
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

    ArticleListPopups(activePopup, uiState, viewModel, navController, onShowPopup, onDismissPopup)
}

@Composable
private fun ArticleListPopups(
    activePopup: Popup?,
    uiState: ArticleListUiState,
    viewModel: ArticleListViewModel,
    navController: NavController,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
) {
    when (activePopup) {
        Popup.GameFilter -> {
            val options = remember(uiState.games) {
                uiState.games.map { GameFilterOption(it.id, it.name, it.articleCount) }
            }
            GameFilterSheet(
                games = options,
                selectedId = uiState.selectedGameFilter,
                onSelect = {
                    viewModel.onSelectFilter(it)
                    onDismissPopup()
                },
                onClose = onDismissPopup,
            )
        }
        is Popup.ArticleActions -> uiState.articleById(activePopup.articleId)?.let { article ->
            ArticleActionsSheet(
                article = article,
                onAction = { action ->
                    onDismissPopup()
                    navController.perform(action, article, onShowPopup)
                },
                onClose = onDismissPopup,
            )
        }
        is Popup.DeleteArticle -> uiState.articleById(activePopup.articleId)?.let { article ->
            DeleteArticleDialog(
                articleTitle = article.title,
                onConfirm = {
                    viewModel.deleteArticle(article.id)
                    onDismissPopup()
                },
                onClose = onDismissPopup,
            )
        }
        is Popup.ScrapeError -> uiState.articleById(activePopup.articleId)?.let { article ->
            ScrapeErrorDialog(
                article = article,
                onRetry = {
                    viewModel.retryFailedPages(article.id)
                    onDismissPopup()
                },
                onDelete = { onShowPopup(Popup.DeleteArticle(article.id)) },
                onClose = onDismissPopup,
            )
        }
        else -> Unit
    }
}

private fun NavController.openReader(article: ArticleUiModel, resumeFrom: String) {
    navigate(Reader(articleId = article.id, resumeFrom = resumeFrom))
}

private fun NavController.perform(action: ArticleAction, article: ArticleUiModel, onShowPopup: (Popup) -> Unit) {
    when (action) {
        ArticleAction.RESUME -> openReader(article, resumeFrom = "checkpoint")
        ArticleAction.OPEN -> openReader(article, resumeFrom = "last")
        ArticleAction.APPEND -> navigate(AddArticle(mode = "append", targetArticleId = article.id))
        ArticleAction.DELETE -> onShowPopup(Popup.DeleteArticle(article.id))
    }
}
