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
 * S4 Game Binder. Category Name Sheet dirender di sini; [activePopup] tetap satu state bersama di NavHost. Premium
 * hilang (atau gratis) = hanya-baca; data kategori tidak dihapus dan kembali bisa diubah saat premium kembali.
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
    val categories = state.categories
    val articles = state.articles.map { article ->
        val own = state.assignments[article.id].orEmpty()
        BinderArticleUi(article.id, article.title, categories.filter { it.id in own }.map { it.id }.toSet())
    }
    var pickerCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }

    val actions = BinderActions(
        onBack = { navController.popBackStack() },
        onCreate = { onShowPopup(Popup.CategoryName(null)) },
        onRename = { onShowPopup(Popup.CategoryName(it)) },
        onDelete = {
            viewModel.deleteCategory(it)
            if (pickerCategoryId == it) pickerCategoryId = null
        },
        onTogglePicker = { pickerCategoryId = if (pickerCategoryId == it) null else it },
        onSetMember = viewModel::setMember,
        onArticleTap = { id ->
            val article = state.articles.firstOrNull { it.id == id }
            if (article?.status == ArticleStatus.READY) {
                navController.navigate(Reader(articleId = id, resumeFrom = "last"))
            } else {
                viewModel.showMessage(R.string.article_list_snackbar_scraping)
            }
        },
    )

    BinderContent(
        gameName = state.gameName,
        categories = categories,
        articles = articles,
        readOnly = state.premium != PremiumUiState.PREMIUM,
        pickerCategoryId = pickerCategoryId,
        snackbar = state.message?.let { message ->
            message.arg?.let { stringResource(message.res, it) } ?: stringResource(message.res)
        },
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
                if (editing == null) viewModel.createCategory(name) else viewModel.renameCategory(editing.id, name)
                onDismissPopup()
            },
            onClose = onDismissPopup,
        )
    }
}
