package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.displaysettings.DisplaySettingsSheet
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.popup.Popup

/**
 * S3. Data, checkpoint, posisi baca, mode baca, retry, dan preferensi tampilan dari [ReaderViewModel]. Display
 * Settings Sheet dirender di sini agar berbagi state dengan Reader. Posisi baca disimpan saat layar ditinggalkan
 * (tombol/gesture kembali maupun keluar dari komposisi).
 */
@Composable
fun ReaderScreen(
    articleId: Long,
    navController: NavController,
    activePopup: Popup?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReaderContent(
        viewState = state.viewState,
        fontSize = state.fontSize,
        darkMode = state.darkMode,
        article = state.article ?: placeholderArticle(articleId),
        content = state.content,
        resumeNodeId = state.resumeNodeId,
        resumeFrom = viewModel.resumeFrom,
        snackbar = state.snackbar?.let { stringResource(it) },
        onBack = { nodeId ->
            nodeId?.let(viewModel::saveLastPosition)
            navController.popBackStack()
        },
        onLeave = { nodeId -> nodeId?.let(viewModel::saveLastPosition) },
        onOpenSettings = { onShowPopup(Popup.DisplaySettings) },
        onSetCheckpoint = viewModel::setCheckpoint,
        onModeChange = viewModel::setReadMode,
        onRetryFailed = viewModel::retryFailed,
        onRetryLoad = viewModel::reload,
    )

    if (activePopup == Popup.DisplaySettings) {
        DisplaySettingsSheet(
            fontSize = state.fontSize,
            darkMode = state.darkMode,
            onFontSizeChange = viewModel::setFontSize,
            onDarkModeToggle = viewModel::setDarkMode,
            onClose = onDismissPopup,
        )
    }
}

/** Header Reader tetap butuh artikel saat data belum ada (loading / tidak ditemukan / error). */
private fun placeholderArticle(articleId: Long) = ArticleUiModel(
    id = articleId,
    gameId = 0,
    gameName = "",
    title = "",
    status = ArticleStatus.READY,
    pages = emptyList(),
)
