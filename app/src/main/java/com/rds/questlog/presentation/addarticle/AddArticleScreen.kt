package com.rds.questlog.presentation.addarticle

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.rds.questlog.presentation.model.AddArticlePayload
import com.rds.questlog.presentation.model.ScrapeMode
import com.rds.questlog.presentation.popup.Popup
import com.rds.questlog.presentation.preview.PreviewData

/**
 * S2. Tahap 1 memakai data dummy ([PreviewData]); penyimpanan dan data nyata menyusul di Tahap 2.
 * [onSaved] dipanggil setelah form valid disimpan, sebelum kembali ke daftar.
 */
@Composable
fun AddArticleScreen(
    mode: String,
    targetArticleId: Long?,
    navController: NavController,
    onShowPopup: (Popup) -> Unit,
    onSaved: (AddArticlePayload) -> Unit,
) {
    AddArticleContent(
        mode = ScrapeMode.fromRoute(mode),
        targetArticleId = targetArticleId,
        games = PreviewData.games,
        articles = PreviewData.articles,
        isPremium = false,
        onBack = { navController.popBackStack() },
        onSave = {
            navController.popBackStack()
            onSaved(it)
        },
        onOpenUnlock = { onShowPopup(Popup.Unlock) },
    )
}
