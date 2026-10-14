package com.rds.questlog.presentation.popup

import androidx.compose.runtime.Composable
import com.rds.questlog.presentation.unlock.UnlockSheetHost

/**
 * Merender popup global di atas layar. Popup S1 dirender ArticleListScreen, popup Reader (alat baca, bookmark,
 * catatan, Display Settings) dirender ReaderScreen; hanya Unlock Sheet yang dipakai bersama dan dirender di sini.
 * Dismiss (scrim / back / Tutup) selalu menutup lewat [onDismiss].
 */
@Composable
fun PopupHost(popup: Popup?, onDismiss: () -> Unit) {
    if (popup == Popup.Unlock) UnlockSheetHost(onClose = onDismiss)
}
