package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.rds.questlog.presentation.model.ScrapeJob
import com.rds.questlog.presentation.model.ScrapeMode
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.preview.PreviewData

/**
 * S2. Data dan penyimpanan dari [AddArticleViewModel]; [onSaved] menerima pekerjaan scraping yang baru dijadwalkan
 * (untuk Scrape Notification) setelah layar kembali ke daftar. [sharedUrl] = URL dari "Bagikan" di browser
 * (QL-21): mengisi Hal. 1 dan menampilkan penanda; belum dihubungkan ke Intent (Sprint 6 Tahap 2).
 */
@Composable
fun AddArticleScreen(
    mode: String,
    targetArticleId: Long?,
    navController: NavController,
    onShowPopup: (Popup) -> Unit,
    onSaved: (ScrapeJob) -> Unit,
    sharedUrl: String? = null,
    viewModel: AddArticleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val submitError by viewModel.submitError.collectAsStateWithLifecycle()

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
        // Tahap 1: isi clipboard contoh; Tahap 2 membaca ClipboardManager hanya saat Tempel diketuk.
        readClipboard = { PreviewData.CLIPBOARD_TEXT },
    )
}
