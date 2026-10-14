package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rds.questlog.presentation.model.BookmarkLabelMode
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.model.ReaderToolAction
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.preview.ReaderPreviewData
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Posisi baca saat ini: node pertama yang terlihat dan cuplikan teksnya. */
data class ReaderPosition(val nodeId: Long? = null, val snippet: String = "")

/** Permintaan lompat ke [nodeId]; [serial] membedakan permintaan berturut-turut ke node yang sama. */
data class ReaderJump(val nodeId: Long, val serial: Int)

/**
 * Data bookmark dan catatan SEMENTARA (dummy, belum ke Room) untuk UI Sprint 7 Tahap 1; diganti UseCase di
 * tahap wiring. Hilang saat layar dibuat ulang.
 */
@Stable
class ReaderToolsState(private val articleId: Long) {
    var bookmarks by mutableStateOf(ReaderPreviewData.bookmarks)
    var note by mutableStateOf(if (articleId == ReaderPreviewData.article.id) ReaderPreviewData.NOTE else "")
    var position by mutableStateOf(ReaderPosition())
    var jump by mutableStateOf<ReaderJump?>(null)
    private var nextId = bookmarks.maxOfOrNull { it.id } ?: 0L
    private var jumpSerial = 0

    val count get() = bookmarks.count { it.articleId == articleId }
    val recentLabels get() = bookmarks.sortedByDescending { it.id }.map { it.label }.distinct()

    fun add(label: String, articleTitle: String) {
        val here = position.nodeId ?: return
        bookmarks = bookmarks + BookmarkUi(++nextId, articleId, articleTitle, label, position.snippet, here)
    }

    fun rename(id: Long, label: String) {
        bookmarks = bookmarks.map { if (it.id == id) it.copy(label = label) else it }
    }

    fun remove(id: Long) {
        bookmarks = bookmarks.filterNot { it.id == id }
    }

    /** Hanya bookmark artikel ini yang bisa dilompati di sini; antar-artikel menyusul di tahap wiring. */
    fun requestJump(id: Long) {
        val target = bookmarks.firstOrNull { it.id == id && it.articleId == articleId } ?: return
        jump = ReaderJump(target.nodeId, ++jumpSerial)
    }
}

/** Popup Reader (P17, P12, P12a, P13) di atas layar baca; Display Settings tetap dirender [ReaderScreen]. */
@Composable
fun ReaderPopups(
    popup: Popup?,
    tools: ReaderToolsState,
    premium: PremiumUiState,
    gameName: String,
    articleId: Long,
    articleTitle: String,
    darkMode: Boolean,
    onShowPopup: (Popup) -> Unit,
    onDismiss: () -> Unit,
) {
    val readOnly = premium == PremiumUiState.LOST
    QuestLogTheme(darkTheme = darkMode) {
        when (popup) {
            Popup.ReaderTools -> ReaderToolsSheet(
                premium = premium,
                snippet = tools.position.snippet,
                hasGameProgress = false,
                isGameProgressHere = false,
                hasNote = tools.note.isNotEmpty(),
                onAction = { action ->
                    when (action) {
                        ReaderToolAction.PROGRESS -> onDismiss()
                        ReaderToolAction.NOTE ->
                            onShowPopup(if (premium == PremiumUiState.FREE) Popup.Unlock else Popup.NotePanel)
                        ReaderToolAction.DISPLAY -> onShowPopup(Popup.DisplaySettings)
                    }
                },
                onClose = onDismiss,
            )
            Popup.BookmarkPanel -> BookmarkPanel(
                gameName = gameName,
                bookmarks = tools.bookmarks,
                currentArticleId = articleId,
                readOnly = readOnly,
                onAddHere = { onShowPopup(Popup.BookmarkLabel(null, fromPanel = true)) },
                onEdit = { onShowPopup(Popup.BookmarkLabel(it, fromPanel = true)) },
                onDelete = tools::remove,
                onJump = {
                    tools.requestJump(it)
                    onDismiss()
                },
                onClose = onDismiss,
            )
            is Popup.BookmarkLabel -> BookmarkLabelPopup(popup, tools, articleTitle, onShowPopup, onDismiss)
            Popup.NotePanel -> NotePanel(
                articleTitle = articleTitle,
                initialText = tools.note,
                readOnly = readOnly,
                onSave = { tools.note = it },
                onClose = onDismiss,
            )
            else -> Unit
        }
    }
}

@Composable
private fun BookmarkLabelPopup(
    popup: Popup.BookmarkLabel,
    tools: ReaderToolsState,
    articleTitle: String,
    onShowPopup: (Popup) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = popup.bookmarkId?.let { id -> tools.bookmarks.firstOrNull { it.id == id } }
    // Dari Bookmark Panel, menutup dialog kembali ke panel (P12a → P12).
    val back = { if (popup.fromPanel) onShowPopup(Popup.BookmarkPanel) else onDismiss() }
    BookmarkLabelDialog(
        mode = if (existing == null) BookmarkLabelMode.ADD else BookmarkLabelMode.EDIT,
        snippet = existing?.snippet ?: tools.position.snippet,
        initialLabel = existing?.label.orEmpty(),
        recent = tools.recentLabels,
        onSave = { label ->
            if (existing == null) tools.add(label, articleTitle) else tools.rename(existing.id, label)
            back()
        },
        onDismiss = back,
    )
}
