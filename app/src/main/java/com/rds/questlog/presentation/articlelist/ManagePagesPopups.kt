package com.rds.questlog.presentation.articlelist

import androidx.compose.runtime.Composable
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.popup.Popup

/**
 * Page Manager Sheet dengan Delete Page Dialog di atasnya (screen_flow.md §4: keduanya satu pasangan, dialog
 * menutup kembali ke Page Manager). Halaman berasal dari data artikel (Flow Room), jadi geser dan hapus langsung
 * tampil tanpa state lokal. Unduh ulang per halaman (⟳) disambungkan di Sprint 5 Tahap 2C.
 */
@Composable
internal fun ManagePagesPopups(
    article: ArticleUiModel,
    deletingPageId: Long?,
    viewModel: ArticleListViewModel,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
) {
    val backToPages = { onShowPopup(Popup.ManagePages(article.id)) }

    PageManagerSheet(
        articleTitle = article.title,
        pages = article.pages,
        onMove = { index, dir -> viewModel.movePage(article, index, dir) },
        onRefreshPage = {},
        onDeletePage = { index -> onShowPopup(Popup.DeletePage(article.id, article.pages[index].id)) },
        onClose = onDismissPopup,
    )
    val deleting = article.pages.indexOfFirst { it.id == deletingPageId }
    if (deletingPageId != null && deleting >= 0) {
        DeletePageDialog(
            pageNumber = deleting + 1,
            pageUrl = article.pages[deleting].url,
            onConfirm = {
                viewModel.deletePage(article.id, deletingPageId)
                backToPages()
            },
            onClose = backToPages,
        )
    }
}
