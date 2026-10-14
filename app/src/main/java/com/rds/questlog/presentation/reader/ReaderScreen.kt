package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.displaysettings.DisplaySettingsSheet
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.Reader
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
    val article = state.article ?: placeholderArticle(articleId)
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val tools = remember(articleId) { ReaderToolsState() }
    // Pengguna gratis menulis lewat UseCase: guard menolak, Unlock Sheet dibuka dan data tidak berubah.
    LaunchedEffect(viewModel) { viewModel.unlockRequests.collect { onShowPopup(Popup.Unlock) } }

    ReaderContent(
        viewState = state.viewState,
        fontSize = state.fontSize,
        darkMode = state.darkMode,
        article = article,
        content = state.content,
        resumeNodeId = state.resumeNodeId,
        resumeFrom = viewModel.resumeFrom,
        snackbar = state.snackbar?.let { stringResource(it) },
        onBack = { nodeId ->
            nodeId?.let(viewModel::saveLastPosition)
            navController.popBackStack()
        },
        onLeave = { nodeId -> nodeId?.let(viewModel::saveLastPosition) },
        onOpenTools = { onShowPopup(Popup.ReaderTools) },
        onSetCheckpoint = viewModel::setCheckpoint,
        onModeChange = viewModel::setReadMode,
        onRetryFailed = viewModel::retryFailed,
        onRetryLoad = viewModel::reload,
        premium = state.premium,
        bookmarkCount = bookmarks.count { it.articleId == articleId },
        jumpRequest = tools.jump,
        onOpenBookmarks = {
            onShowPopup(if (state.premium == PremiumUiState.FREE) Popup.Unlock else Popup.BookmarkPanel)
        },
        onQuickAddBookmark = {
            onShowPopup(if (state.premium == PremiumUiState.FREE) Popup.Unlock else Popup.BookmarkLabel(null, false))
        },
        onPositionChange = { tools.position = it },
    )

    val data = ReaderToolsData(
        premium = state.premium,
        article = article,
        darkMode = state.darkMode,
        bookmarks = bookmarks,
        note = note,
    )
    val callbacks = remember(viewModel, articleId) {
        ReaderToolsCallbacks(
            addBookmark = { label ->
                tools.position.nodeId?.let {
                    viewModel.addBookmark(
                        it,
                        label,
                        tools.position.snippet,
                    )
                }
            },
            renameBookmark = viewModel::renameBookmark,
            deleteBookmark = viewModel::deleteBookmark,
            openBookmark = { bookmark ->
                onDismissPopup()
                if (bookmark.articleId == articleId) {
                    tools.requestJump(bookmark.nodeId)
                } else {
                    // Artikel lain di game yang sama: ganti Reader ini dengan artikel itu pada posisi bookmark.
                    navController.navigate(Reader(bookmark.articleId, "bookmark", bookmark.nodeId)) {
                        popUpTo<Reader> { inclusive = true }
                    }
                }
            },
            note = ReaderNoteCallbacks(viewModel::saveNote, viewModel::flushNote, viewModel::deleteNote),
        )
    }
    ReaderPopups(activePopup, tools, data, callbacks, onShowPopup, onDismissPopup)

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
