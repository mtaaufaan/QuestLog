package com.rds.questlog.presentation.binder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.R
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.popup.Popup

/**
 * S4 Game Binder. Category Name Sheet dirender di sini; [activePopup] tetap satu state bersama di NavHost. Kategori
 * dan penetapan artikel masih memakai [BinderDummy] (belum Room). Premium hilang = hanya-baca.
 */
@Composable
fun BinderScreen(
    navController: NavController,
    activePopup: Popup?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
    viewModel: BinderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gameId = viewModel.gameId
    val categories = BinderDummy.categoriesOf(gameId)
    val articles = state.articles.map { article ->
        val own = BinderDummy.assignments[article.id].orEmpty()
        BinderArticleUi(article.id, article.title, categories.filter { it.id in own }.map { it.id }.toSet())
    }
    var pickerCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scrapingMessage = R.string.article_list_snackbar_scraping

    val actions = BinderActions(
        onBack = { navController.popBackStack() },
        onCreate = { onShowPopup(Popup.CategoryName(null)) },
        onRename = { onShowPopup(Popup.CategoryName(it)) },
        onDelete = {
            BinderDummy.delete(it)
            if (pickerCategoryId == it) pickerCategoryId = null
            viewModel.showMessage(R.string.binder_deleted)
        },
        onTogglePicker = { pickerCategoryId = if (pickerCategoryId == it) null else it },
        onSetMember = BinderDummy::setMembership,
        onArticleTap = { id ->
            val article = state.articles.firstOrNull { it.id == id }
            if (article?.status == ArticleStatus.READY) {
                navController.navigate(Reader(articleId = id, resumeFrom = "last"))
            } else {
                viewModel.showMessage(scrapingMessage)
            }
        },
    )

    BinderContent(
        gameName = state.gameName,
        categories = categories,
        articles = articles,
        readOnly = state.premium != PremiumUiState.PREMIUM,
        pickerCategoryId = pickerCategoryId,
        snackbar = state.snackbar?.let { stringResource(it) },
        actions = actions,
    )

    if (activePopup is Popup.CategoryName) {
        val editing = activePopup.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
        CategoryNameSheet(
            rename = editing != null,
            gameName = state.gameName,
            initialName = editing?.name.orEmpty(),
            existingNames = categories.map { it.name },
            onConfirm = { name ->
                if (editing == null) BinderDummy.create(gameId, name) else BinderDummy.rename(editing.id, name)
                onDismissPopup()
            },
            onClose = onDismissPopup,
        )
    }
}
