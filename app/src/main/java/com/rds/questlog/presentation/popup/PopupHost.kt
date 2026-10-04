package com.rds.questlog.presentation.popup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rds.questlog.R
import com.rds.questlog.presentation.components.QlBottomSheet
import com.rds.questlog.presentation.components.QlGhostButton

/**
 * Merender popup aktif di atas layar. Popup S1 (GameFilter, ArticleActions, DeleteArticle, ScrapeError)
 * sudah nyata dan dirender oleh ArticleListScreen; sisanya masih placeholder berisi nama popup,
 * diisi per sprint. Dismiss (scrim / back / Tutup) selalu menutup lewat [onDismiss].
 */
@Composable
fun PopupHost(popup: Popup?, onDismiss: () -> Unit) {
    when (popup) {
        null, Popup.GameFilter, Popup.ScrapeMode, Popup.DisplaySettings, is Popup.ArticleActions,
        is Popup.DeleteArticle, is Popup.ScrapeError,
        -> Unit
        else -> QlBottomSheet(onDismiss) { PlaceholderBody(popup.nameRes(), onDismiss) }
    }
}

@Composable
private fun PlaceholderBody(@StringRes name: Int, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(name), style = MaterialTheme.typography.titleLarge)
        QlGhostButton(stringResource(R.string.popup_close), onClose)
    }
}

@StringRes
private fun Popup.nameRes(): Int = when (this) {
    Popup.ScrapeMode -> R.string.popup_scrape_mode
    is Popup.ArticleActions -> R.string.popup_article_actions
    is Popup.DeleteArticle -> R.string.popup_delete_article
    is Popup.ScrapeError -> R.string.popup_scrape_error
    Popup.Unlock -> R.string.popup_unlock
    Popup.GameFilter -> R.string.popup_game_filter
    Popup.DisplaySettings -> R.string.popup_display_settings
}
