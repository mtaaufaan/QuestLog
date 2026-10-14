package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.model.ScrapeJob
import com.rds.questlog.presentation.model.ScrapeMode
import com.rds.questlog.presentation.popup.Popup

/**
 * S2. Data dan penyimpanan dari [AddArticleViewModel]; [onSaved] menerima pekerjaan scraping yang baru dijadwalkan
 * (untuk Scrape Notification) setelah layar kembali ke daftar. [sharedUrl] = URL dari "Bagikan" di browser
 * (QL-21): mengisi Hal. 1 dan menampilkan penanda. [incomingSharedUrl] = URL yang dibagikan saat layar ini sudah
 * terbuka; isian yang ada tidak ditimpa tanpa konfirmasi, dan [onIncomingHandled] dipanggil setelah diproses.
 */
@Composable
fun AddArticleScreen(
    mode: String,
    targetArticleId: Long?,
    navController: NavController,
    onShowPopup: (Popup) -> Unit,
    onSaved: (ScrapeJob) -> Unit,
    sharedUrl: String? = null,
    incomingSharedUrl: String? = null,
    onIncomingHandled: () -> Unit = {},
    viewModel: AddArticleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val submitError by viewModel.submitError.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    AddArticleContent(
        mode = ScrapeMode.fromRoute(mode),
        targetArticleId = targetArticleId,
        games = state.games,
        articles = state.articles,
        isPremium = state.isPremium,
        onBack = { navController.popBackStack() },
        onSave = { payload ->
            viewModel.save(payload) { job ->
                navController.popBackStack()
                onSaved(job)
            }
        },
        onOpenUnlock = { onShowPopup(Popup.Unlock) },
        submitError = submitError?.let { stringResource(it) },
        sharedUrl = sharedUrl,
        incomingSharedUrl = incomingSharedUrl,
        onIncomingHandled = onIncomingHandled,
        // Clipboard dibaca HANYA saat Tempel diketuk (QL-22); tidak pernah otomatis saat layar dibuka.
        readClipboard = { clipboard.getText()?.text },
    )
}
