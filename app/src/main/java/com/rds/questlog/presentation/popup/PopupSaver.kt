package com.rds.questlog.presentation.popup

import androidx.compose.runtime.saveable.Saver

/**
 * Menyimpan popup aktif ke state instance (rotasi layar, proses dimatikan sistem) sebagai daftar [Long]:
 * kode jenis popup diikuti id-nya. Tanpa ini popup menghilang saat layar diputar.
 */
val PopupSaver: Saver<Popup?, Any> = Saver(
    save = { popup -> popup?.let(::encode) },
    restore = { saved -> (saved as? List<*>)?.filterIsInstance<Long>()?.let(::decode) },
)

private const val SCRAPE_MODE = 0L
private const val ARTICLE_ACTIONS = 1L
private const val DELETE_ARTICLE = 2L
private const val SCRAPE_ERROR = 3L
private const val UNLOCK = 4L
private const val GAME_FILTER = 5L
private const val DISPLAY_SETTINGS = 6L
private const val EDIT_ARTICLE = 7L
private const val MANAGE_PAGES = 8L
private const val DELETE_PAGE = 9L
private const val REFRESH_ARTICLE = 10L

private fun encode(popup: Popup): ArrayList<Long> = when (popup) {
    Popup.ScrapeMode -> arrayListOf(SCRAPE_MODE)
    is Popup.ArticleActions -> arrayListOf(ARTICLE_ACTIONS, popup.articleId)
    is Popup.DeleteArticle -> arrayListOf(DELETE_ARTICLE, popup.articleId)
    is Popup.ScrapeError -> arrayListOf(SCRAPE_ERROR, popup.articleId)
    Popup.Unlock -> arrayListOf(UNLOCK)
    Popup.GameFilter -> arrayListOf(GAME_FILTER)
    Popup.DisplaySettings -> arrayListOf(DISPLAY_SETTINGS)
    is Popup.EditArticle -> arrayListOf(EDIT_ARTICLE, popup.articleId)
    is Popup.ManagePages -> arrayListOf(MANAGE_PAGES, popup.articleId)
    is Popup.DeletePage -> arrayListOf(DELETE_PAGE, popup.articleId, popup.pageId)
    is Popup.RefreshArticle -> arrayListOf(REFRESH_ARTICLE, popup.articleId)
}

/** Kebalikan [encode]; null bila kode tidak dikenal atau isinya tidak lengkap (popup tidak dipulihkan). */
private fun decode(values: List<Long>): Popup? {
    val id = values.getOrNull(1)
    return when (values.firstOrNull()) {
        SCRAPE_MODE -> Popup.ScrapeMode
        UNLOCK -> Popup.Unlock
        GAME_FILTER -> Popup.GameFilter
        DISPLAY_SETTINGS -> Popup.DisplaySettings
        DELETE_PAGE -> values.getOrNull(2)?.let { page -> id?.let { Popup.DeletePage(it, page) } }
        else -> id?.let { withArticle(values.first(), it) }
    }
}

private fun withArticle(code: Long, articleId: Long): Popup? = when (code) {
    ARTICLE_ACTIONS -> Popup.ArticleActions(articleId)
    DELETE_ARTICLE -> Popup.DeleteArticle(articleId)
    SCRAPE_ERROR -> Popup.ScrapeError(articleId)
    EDIT_ARTICLE -> Popup.EditArticle(articleId)
    MANAGE_PAGES -> Popup.ManagePages(articleId)
    REFRESH_ARTICLE -> Popup.RefreshArticle(articleId)
    else -> null
}
