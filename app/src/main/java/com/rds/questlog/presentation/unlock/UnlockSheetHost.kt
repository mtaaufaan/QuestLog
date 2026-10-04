package com.rds.questlog.presentation.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Menempel [UnlockSheet] ke data nyata: jumlah game, status premium dari app_config (Flow, jadi layar sukses dan batas
 * tier berubah tanpa restart), serta aksi Unlock / Restore Purchase lewat use case.
 */
@Composable
fun UnlockSheetHost(onClose: () -> Unit, viewModel: UnlockViewModel = hiltViewModel()) {
    val gamesUsed by viewModel.gamesUsed.collectAsStateWithLifecycle()
    val premium by viewModel.isPremium.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    UnlockSheet(
        gamesUsed = gamesUsed.coerceIn(0, 2),
        isPremium = premium,
        phase = phase,
        message = message,
        onPurchase = viewModel::purchase,
        onRestore = viewModel::restore,
        onClose = onClose,
    )
}
