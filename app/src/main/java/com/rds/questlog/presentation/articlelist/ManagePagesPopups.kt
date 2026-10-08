package com.rds.questlog.presentation.articlelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.rds.questlog.presentation.model.ArticleUiModel
import com.rds.questlog.presentation.model.PageStatus
import com.rds.questlog.presentation.model.PageUiModel
import com.rds.questlog.presentation.popup.Popup

/**
 * Page Manager Sheet dengan Delete Page Dialog di atasnya (screen_flow.md §4: keduanya satu pasangan, dialog
 * menutup kembali ke Page Manager). Sprint 5 Tahap 1: geser, unduh ulang, dan hapus hanya mengubah salinan lokal
 * halaman; Tahap 2 menggantinya dengan ReorderPagesUseCase / RefreshPageUseCase / DeletePageUseCase.
 */
@Composable
internal fun ManagePagesPopups(
    article: ArticleUiModel,
    deletingPageId: Long?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
) {
    val pages = remember(article.id) { mutableStateListOf<PageUiModel>().apply { addAll(article.pages) } }
    val backToPages = { onShowPopup(Popup.ManagePages(article.id)) }

    PageManagerSheet(
        articleTitle = article.title,
        pages = pages,
        onMove = { index, dir -> pages.add(index + dir, pages.removeAt(index)) },
        onRefreshPage = { index -> pages[index] = pages[index].copy(status = PageStatus.PENDING, reason = null) },
        onDeletePage = { index -> onShowPopup(Popup.DeletePage(article.id, pages[index].id)) },
        onClose = onDismissPopup,
    )
    val deleting = pages.firstOrNull { it.id == deletingPageId }
    if (deleting != null) {
        DeletePageDialog(
            pageNumber = pages.indexOf(deleting) + 1,
            pageUrl = deleting.url,
            onConfirm = {
                pages.remove(deleting)
                backToPages()
            },
            onClose = backToPages,
        )
    }
}
