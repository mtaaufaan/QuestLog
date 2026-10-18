package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.R
import com.rds.questlog.presentation.displaysettings.DisplaySettingsSheet
import com.rds.questlog.presentation.model.ArticleStatus
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.Reader
import com.rds.questlog.presentation.navigation.showGamePage
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
    // Pratinjau Bookmark (P20): lokal di Reader (bukan activePopup) agar dialog dan Unlock Sheet tampil di atasnya.
    var peek by rememberSaveable(stateSaver = PeekTargetSaver) { mutableStateOf<PeekTarget?>(null) }
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
            // Kembali selalu ke Halaman Game artikel ini, juga bila Reader dibuka dari pencarian S0 atau notifikasi.
            navController.showGamePage(article.gameId)
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
        recentLabels = bookmarks.sortedByDescending { it.id }.map { it.label }.distinct(),
        bookmarksByNode = bookmarks.filter { it.articleId == articleId }.groupBy { it.nodeId },
        onAddBookmarkAt = { nodeId, label, snippet -> viewModel.addBookmark(nodeId, label, snippet) },
        onUnlock = { onShowPopup(Popup.Unlock) },
        onRenameBookmark = { onShowPopup(Popup.BookmarkLabel(it.id, fromPanel = false)) },
        onDeleteBookmark = { viewModel.deleteBookmark(it.id) },
        onPositionChange = { tools.position = it },
    )

    val data = ReaderToolsData(
        premium = state.premium,
        article = article,
        darkMode = state.darkMode,
        bookmarks = bookmarks,
        mainArticleId = article.gameMainArticleId,
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
            setMainArticle = viewModel::setMainArticle,
            deleteBookmark = viewModel::deleteBookmark,
            // Bookmark dibuka sebagai pratinjau (P20); artikel dan posisi baca Reader tidak berubah.
            openBookmark = { bookmark ->
                onDismissPopup()
                peek = PeekTarget(bookmark.articleId, bookmark.nodeId)
            },
            note = ReaderNoteCallbacks(viewModel::saveNote, viewModel::flushNote, viewModel::deleteNote),
        )
    }
    ReaderPopups(activePopup, tools, data, callbacks, onShowPopup, onDismissPopup)

    peek?.let { target ->
        val peekData by remember(target.articleId) { viewModel.observePeek(target.articleId) }
            .collectAsStateWithLifecycle(initialValue = null)
        BookmarkPeekSheet(
            target = target,
            data = peekData,
            bookmarks = bookmarks,
            context = PeekContext(articleId, article.title, state.premium, state.fontSize, state.darkMode),
            actions = PeekActions(
                addBookmark = { nodeId, label, snippet ->
                    viewModel.addBookmark(nodeId, label, snippet, forArticle = target.articleId)
                },
                renameBookmark = viewModel::renameBookmark,
                deleteBookmark = viewModel::deleteBookmark,
                setCheckpoint = { nodeId -> viewModel.setCheckpoint(nodeId, forArticle = target.articleId) },
                openFull = {
                    peek = null
                    // Artikel di pratinjau menggantikan Reader ini, dibuka di posisi bookmark.
                    navController.navigate(Reader(target.articleId, "peek", target.nodeId)) {
                        popUpTo<Reader> { inclusive = true }
                    }
                },
                moveHere = {
                    peek = null
                    tools.requestJump(target.nodeId, R.string.reader_snackbar_peek_opened)
                },
                onUnlock = { onShowPopup(Popup.Unlock) },
            ),
            onClose = { peek = null },
        )
    }

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
