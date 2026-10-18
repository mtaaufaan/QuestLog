package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.BookmarkLabelMode
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.model.ReaderToolAction
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.theme.QuestLogTheme

/** Posisi baca saat ini: node pertama yang terlihat dan cuplikan teksnya. */
data class ReaderPosition(val nodeId: Long? = null, val snippet: String = "")

/**
 * Permintaan lompat ke [nodeId]; [serial] membedakan permintaan berturut-turut ke node yang sama; [messageRes] =
 * pesan snackbar setelah lompat (null = tanpa pesan).
 */
data class ReaderJump(val nodeId: Long, val serial: Int, val messageRes: Int? = null)

/** State UI murni Reader yang tidak disimpan di database: posisi baca saat ini dan permintaan lompat bookmark. */
@Stable
class ReaderToolsState {
    var position by mutableStateOf(ReaderPosition())
    var jump by mutableStateOf<ReaderJump?>(null)
    private var jumpSerial = 0

    fun requestJump(nodeId: Long, messageRes: Int? = null) {
        jump = ReaderJump(nodeId, ++jumpSerial, messageRes)
    }
}

/** Data yang ditampilkan popup Reader; semuanya berasal dari ViewModel. */
class ReaderToolsData(
    val premium: PremiumUiState,
    val article: ArticleUiModel,
    val darkMode: Boolean,
    val bookmarks: List<BookmarkUi>,
    /** Artikel utama game ini (dari Binder sementara); null = belum ada. */
    val mainArticleId: Long?,
    /** null = catatan belum dimuat; panel catatan menunggu agar tidak menimpa isi yang ada. */
    val note: String?,
)

/** Aksi catatan: simpan (autosave, true bila tersimpan), simpan sisa saat ditutup, dan hapus. */
class ReaderNoteCallbacks(
    val save: suspend (String) -> Boolean,
    val flush: (String) -> Unit,
    val delete: () -> Unit,
)

/** Aksi pengguna di popup Reader; diteruskan ke ViewModel (UseCase lewat PremiumGuard). */
class ReaderToolsCallbacks(
    val addBookmark: (String) -> Unit,
    val renameBookmark: (Long, String) -> Unit,
    val deleteBookmark: (Long) -> Unit,
    val openBookmark: (BookmarkUi) -> Unit,
    val setMainArticle: () -> Unit,
    val note: ReaderNoteCallbacks,
)

/** Popup Reader (P17, P12, P12a, P13) di atas layar baca; Display Settings tetap dirender [ReaderScreen]. */
@Composable
fun ReaderPopups(
    popup: Popup?,
    state: ReaderToolsState,
    data: ReaderToolsData,
    callbacks: ReaderToolsCallbacks,
    onShowPopup: (Popup) -> Unit,
    onDismiss: () -> Unit,
) {
    val readOnly = data.premium == PremiumUiState.LOST
    QuestLogTheme(darkTheme = data.darkMode) {
        when (popup) {
            Popup.ReaderTools -> ReaderToolsSheet(
                premium = data.premium,
                snippet = state.position.snippet,
                hasGameProgress = data.mainArticleId != null,
                isGameProgressHere = data.mainArticleId == data.article.id,
                hasNote = !data.note.isNullOrEmpty(),
                onAction = { action ->
                    when (action) {
                        ReaderToolAction.PROGRESS -> {
                            callbacks.setMainArticle()
                            onDismiss()
                        }
                        ReaderToolAction.NOTE ->
                            onShowPopup(if (data.premium == PremiumUiState.FREE) Popup.Unlock else Popup.NotePanel)
                        ReaderToolAction.DISPLAY -> onShowPopup(Popup.DisplaySettings)
                    }
                },
                onClose = onDismiss,
            )
            Popup.BookmarkPanel -> BookmarkPanel(
                gameName = data.article.gameName,
                bookmarks = data.bookmarks,
                currentArticleId = data.article.id,
                readOnly = readOnly,
                onAddHere = { onShowPopup(Popup.BookmarkLabel(null, fromPanel = true)) },
                onEdit = { onShowPopup(Popup.BookmarkLabel(it, fromPanel = true)) },
                onDelete = callbacks.deleteBookmark,
                onJump = { id -> data.bookmarks.firstOrNull { it.id == id }?.let(callbacks.openBookmark) },
                onClose = onDismiss,
            )
            is Popup.BookmarkLabel -> BookmarkLabelPopup(popup, state, data, callbacks, onShowPopup, onDismiss)
            Popup.NotePanel -> data.note?.let { note ->
                NotePanel(
                    articleTitle = data.article.title,
                    initialText = note,
                    readOnly = readOnly,
                    onSave = callbacks.note.save,
                    onFlush = callbacks.note.flush,
                    onDelete = callbacks.note.delete,
                    onClose = onDismiss,
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun BookmarkLabelPopup(
    popup: Popup.BookmarkLabel,
    state: ReaderToolsState,
    data: ReaderToolsData,
    callbacks: ReaderToolsCallbacks,
    onShowPopup: (Popup) -> Unit,
    onDismiss: () -> Unit,
) {
    val existing = popup.bookmarkId?.let { id -> data.bookmarks.firstOrNull { it.id == id } }
    // Dari Bookmark Panel, menutup dialog kembali ke panel (P12a → P12).
    val back = { if (popup.fromPanel) onShowPopup(Popup.BookmarkPanel) else onDismiss() }
    BookmarkLabelDialog(
        mode = if (existing == null) BookmarkLabelMode.ADD else BookmarkLabelMode.EDIT,
        snippet = existing?.snippet ?: state.position.snippet,
        initialLabel = existing?.label.orEmpty(),
        recent = data.bookmarks.sortedByDescending { it.id }.map { it.label }.distinct(),
        onSave = { label ->
            if (existing == null) callbacks.addBookmark(label) else callbacks.renameBookmark(existing.id, label)
            back()
        },
        onDismiss = back,
    )
}
