package com.rds.questlog.presentation.reader

import androidx.annotation.StringRes
import com.rds.questlog.R
import com.rds.questlog.presentation.model.BookmarkUi
import com.rds.questlog.presentation.model.PremiumUiState

/** Aksi bookmark di daftar node: simpan dari popover, Unlock (gratis), ubah label, dan hapus. */
class NodeBookmarkCallbacks(
    val onSave: (nodeId: Long, label: String, snippet: String) -> Unit,
    val onUnlock: () -> Unit,
    val onRename: (BookmarkUi) -> Unit,
    val onDelete: (BookmarkUi) -> Unit,
)

/** Data bookmark untuk membangun [NodeBookmark] per node; sama untuk Reader (S3) dan Pratinjau Bookmark (P20). */
class NodeBookmarkSource(
    val premium: PremiumUiState,
    val recentLabels: List<String>,
    /** Bookmark artikel yang ditampilkan, dikelompokkan per node untuk penanda inline. */
    val bookmarksByNode: Map<Long, List<BookmarkUi>>,
    /** Node yang disorot (tint), mis. node bookmark yang sedang dipratinjau. */
    val highlightNodeId: Long?,
    val callbacks: NodeBookmarkCallbacks,
)

/** State daftar baca yang dibutuhkan popover dan penanda: node popover aktif, cuplikan teks, dan pesan singkat. */
interface NodeBookmarkState {
    /** Node yang popover bookmark-nya terbuka (tekan-tahan); null = tidak ada. */
    var popoverNodeId: Long?

    /** Cuplikan teks node [id] untuk label bookmark. */
    fun snippetOf(id: Long): String

    /** Pesan singkat (snackbar) dari string resource [id]. */
    fun showToast(@StringRes id: Int, vararg args: Any)
}

/** Interaksi bookmark untuk node [id]: popover aktif hanya di node yang ditahan; penanda menu Ubah/Hapus. */
fun nodeBookmarkFor(id: Long, source: NodeBookmarkSource, state: NodeBookmarkState) = NodeBookmark(
    markers = source.bookmarksByNode[id].orEmpty(),
    markerTools = MarkerTools(
        readOnly = source.premium == PremiumUiState.LOST,
        onRename = source.callbacks.onRename,
        onDelete = { bookmark ->
            source.callbacks.onDelete(bookmark)
            state.showToast(R.string.reader_snackbar_bookmark_deleted, bookmark.label)
        },
    ),
    active = state.popoverNodeId == id,
    highlighted = source.highlightNodeId == id,
    onLongPress = { state.popoverNodeId = id },
    popover = { width ->
        BookmarkPopover(
            premium = source.premium,
            nodeWidth = width,
            recent = source.recentLabels,
            onSave = { label ->
                source.callbacks.onSave(id, label, state.snippetOf(id))
                state.popoverNodeId = null
                state.showToast(R.string.reader_snackbar_bookmark_saved, label)
            },
            onUnlock = {
                state.popoverNodeId = null
                source.callbacks.onUnlock()
            },
            onDismiss = { state.popoverNodeId = null },
        )
    },
)
