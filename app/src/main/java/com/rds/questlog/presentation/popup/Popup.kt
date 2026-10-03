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
}
