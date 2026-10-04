package com.rds.questlog.presentation.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.rds.questlog.presentation.displaysettings.DEFAULT_FONT_SIZE
import com.rds.questlog.presentation.displaysettings.DisplaySettingsSheet
import com.rds.questlog.presentation.model.ReaderViewState
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.preview.ReaderPreviewData

/**
 * S3. Tahap 1: artikel dummy ([ReaderPreviewData]) dan preferensi tampilan di state lokal; DataStore, checkpoint,
 * dan auto-resume nyata menyusul di Tahap 2. Display Settings Sheet dirender di sini agar berbagi state dengan Reader.
 */
@Suppress("UnusedParameter")
@Composable
fun ReaderScreen(
    articleId: Long,
    resumeFrom: String,
    navController: NavController,
    activePopup: Popup?,
    onShowPopup: (Popup) -> Unit,
    onDismissPopup: () -> Unit,
) {
    var fontSize by rememberSaveable { mutableIntStateOf(DEFAULT_FONT_SIZE) }
    var darkMode by rememberSaveable { mutableStateOf(false) }
    val article = ReaderPreviewData.article

    ReaderContent(
        viewState = ReaderViewState.SUCCESS,
        fontSize = fontSize,
        darkMode = darkMode,
        article = article,
        content = ReaderPreviewData.content,
        resumeNodeId = if (resumeFrom == "checkpoint") article.checkpointNodeId else article.lastNodeId,
        resumeFrom = resumeFrom,
        snackbar = null,
        onBack = { navController.popBackStack() },
        onLeave = {},
        onOpenSettings = { onShowPopup(Popup.DisplaySettings) },
        onSetCheckpoint = {},
        onModeChange = {},
        onRetryFailed = {},
        onRetryLoad = {},
    )

    if (activePopup == Popup.DisplaySettings) {
        DisplaySettingsSheet(
            fontSize = fontSize,
            darkMode = darkMode,
            onFontSizeChange = { fontSize = it },
            onDarkModeToggle = { darkMode = it },
            onClose = onDismissPopup,
        )
    }
}
