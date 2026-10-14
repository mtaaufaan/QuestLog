package com.rds.questlog.presentation.popup

/** Satu popup aktif per waktu; bukan route NavController (screen_flow.md §4). */
sealed interface Popup {
    data object ScrapeMode : Popup
    data class ArticleActions(val articleId: Long) : Popup
    data class DeleteArticle(val articleId: Long) : Popup
    data class ScrapeError(val articleId: Long) : Popup
    data object Unlock : Popup
    data object GameFilter : Popup
    data object DisplaySettings : Popup

    // Sprint 5 — pengelolaan artikel (S1)
    data class EditArticle(val articleId: Long) : Popup
    data class ManagePages(val articleId: Long) : Popup
    data class DeletePage(val articleId: Long, val pageId: Long) : Popup
    data class RefreshArticle(val articleId: Long) : Popup

    // Sprint 7 — Reader: alat baca, bookmark, catatan
    data object ReaderTools : Popup
    data object BookmarkPanel : Popup

    /** [bookmarkId] null = tambah di posisi ini; [fromPanel] = kembali ke Bookmark Panel setelah ditutup. */
    data class BookmarkLabel(val bookmarkId: Long?, val fromPanel: Boolean) : Popup
    data object NotePanel : Popup
}
