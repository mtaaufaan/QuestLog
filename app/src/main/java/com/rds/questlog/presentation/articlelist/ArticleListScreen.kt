package com.rds.questlog.presentation.articlelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.binder.AssignCategorySheet
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.model.ScrapeJob
import com.rds.questlog.presentation.navigation.AddArticle
import com.rds.questlog.presentation.navigation.Binder
import com.rds.questlog.presentation.navigation.GameList
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.scrapemode.ScrapeModeSheet

/**
 * S1 Halaman Game. Popup yang menempel padanya (ArticleActions, DeleteArticle, ScrapeError, dst.) dirender di sini;
 * [activePopup] tetap satu state bersama di NavHost (screen_flow.md §4).
 */
@Composable
fun ArticleListScreen(
    navController: NavController,
    activePopup: Popup?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
    onScrapeStarted: (ScrapeJob) -> Unit,
    pendingSharedUrl: String? = null,
    viewModel: ArticleListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val game = uiState.games.firstOrNull { it.id == uiState.selectedGameFilter }
    ArticleListContent(
        gameName = game?.name.orEmpty(),
        articles = uiState.articles,
        totalArticles = game?.articleCount ?: 0,
        premium = uiState.premium,
        query = uiState.query,
        snackbar = uiState.snackbar?.let { res ->
            uiState.snackbarArg?.let { stringResource(res, it) } ?: stringResource(res)
        },
        onBack = { navController.popBackStack<GameList>(inclusive = false) },
        onQuery = viewModel::onQueryChange,
        onOpenBinder = {
            if (uiState.premium == PremiumUiState.FREE) {
                onShowPopup(Popup.Unlock)
            } else {
                game?.let { navController.navigate(Binder(it.id)) }
            }
        },
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
    )

    ArticleListPopups(
        activePopup,
        uiState,
        viewModel,
        navController,
        onShowPopup,
        onDismissPopup,
        onScrapeStarted,
        pendingSharedUrl,
    )
}

@Composable
private fun ArticleListPopups(
    activePopup: Popup?,
    uiState: ArticleListUiState,
    viewModel: ArticleListViewModel,
    navController: NavController,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
    onScrapeStarted: (ScrapeJob) -> Unit,
    pendingSharedUrl: String?,
) {
    when (activePopup) {
        Popup.ScrapeMode -> ScrapeModeSheet(
            hasArticles = uiState.games.sumOf { it.articleCount } > 0,
            onPick = {
                onDismissPopup()
                navController.navigate(
                    AddArticle(mode = it.route, sharedUrl = pendingSharedUrl, gameId = uiState.selectedGameFilter),
                )
            },
            onClose = onDismissPopup,
        )
        is Popup.ArticleActions -> uiState.articleById(activePopup.articleId)?.let { article ->
            ArticleActionsSheet(
                article = article,
                premium = uiState.premium,
                categoryNames = article.categories,
                onAction = { action ->
                    if (action == ArticleAction.CATEGORIES) {
                        // Gratis: Unlock Sheet (data tidak berubah); premium: Assign Category Sheet.
                        onShowPopup(
                            if (uiState.premium == PremiumUiState.FREE) {
                                Popup.Unlock
                            } else {
                                Popup.AssignCategory(
                                    article.id,
                                )
                            },
                        )
                    } else {
                        onDismissPopup()
                        navController.perform(action, article, onShowPopup)
                    }
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
        else -> ArticleManagementPopups(activePopup, uiState, viewModel, onShowPopup, onDismissPopup, onScrapeStarted)
    }
}

/** Popup pengelolaan artikel (Sprint 5): ubah judul/game, kelola halaman, unduh ulang. */
@Composable
private fun ArticleManagementPopups(
    activePopup: Popup?,
    uiState: ArticleListUiState,
    viewModel: ArticleListViewModel,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
    onScrapeStarted: (ScrapeJob) -> Unit,
) {
    when (activePopup) {
        is Popup.EditArticle -> uiState.articleById(activePopup.articleId)?.let { article ->
            EditArticleSheet(
                article = article,
                games = uiState.games,
                isPremium = uiState.isPremium,
                saving = uiState.isSavingEdit,
                saveError = uiState.editSaveError,
                onSave = { viewModel.updateArticle(article.id, it, onSaved = onDismissPopup) },
                onOpenUnlock = {
                    viewModel.onEditDismissed()
                    onShowPopup(Popup.Unlock)
                },
                onClose = {
                    viewModel.onEditDismissed()
                    onDismissPopup()
                },
            )
        }
        is Popup.ManagePages, is Popup.DeletePage -> uiState.articleById(activePopup.managedArticleId())?.let {
            ManagePagesPopups(it, activePopup.deletingPageId(), viewModel, onShowPopup, onDismissPopup, onScrapeStarted)
        }
        is Popup.AssignCategory -> uiState.articleById(activePopup.articleId)?.let { article ->
            AssignCategorySheet(
                articleTitle = article.title,
                gameName = article.gameName,
                categories = uiState.categories,
                selectedIds = uiState.assignments[article.id].orEmpty(),
                onSave = {
                    viewModel.assignCategories(article.id, it)
                    onDismissPopup()
                },
                onClose = onDismissPopup,
            )
        }
        is Popup.RefreshArticle -> uiState.articleById(activePopup.articleId)?.let { article ->
            RefreshArticleDialog(
                articleTitle = article.title,
                pageCount = article.totalPageCount,
                onConfirm = {
                    viewModel.refreshArticle(article, onScrapeStarted)
                    onDismissPopup()
                },
                onClose = onDismissPopup,
            )
        }
        else -> Unit
    }
}

private fun Popup.managedArticleId(): Long = when (this) {
    is Popup.ManagePages -> articleId
    is Popup.DeletePage -> articleId
    else -> -1L
}

private fun Popup.deletingPageId(): Long? = (this as? Popup.DeletePage)?.pageId

private fun NavController.openReader(article: ArticleUiModel, resumeFrom: String) {
    navigate(Reader(articleId = article.id, resumeFrom = resumeFrom))
}

private fun NavController.perform(action: ArticleAction, article: ArticleUiModel, onShowPopup: (Popup) -> Unit) {
    when (action) {
        ArticleAction.RESUME -> openReader(article, resumeFrom = "checkpoint")
        ArticleAction.OPEN -> openReader(article, resumeFrom = "last")
        ArticleAction.APPEND -> navigate(AddArticle(mode = "append", targetArticleId = article.id))
        ArticleAction.EDIT -> onShowPopup(Popup.EditArticle(article.id))
        ArticleAction.CATEGORIES -> onShowPopup(Popup.AssignCategory(article.id))
        ArticleAction.PAGES -> onShowPopup(Popup.ManagePages(article.id))
        ArticleAction.REFRESH -> onShowPopup(Popup.RefreshArticle(article.id))
        ArticleAction.DELETE -> onShowPopup(Popup.DeleteArticle(article.id))
    }
}
