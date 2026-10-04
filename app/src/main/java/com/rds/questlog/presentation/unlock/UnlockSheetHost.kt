package com.rds.questlog.presentation.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Menempel [UnlockSheet] ke data nyata: gamesUsed dari daftar game. Tahap 1: pembelian disimulasikan di state lokal
 * (selama sheet ini hidup); status premium nyata dan Billing menyusul di Tahap 2.
 */
@Composable
fun UnlockSheetHost(onClose: () -> Unit, viewModel: UnlockViewModel = hiltViewModel()) {
    val gamesUsed by viewModel.gamesUsed.collectAsStateWithLifecycle()
    val premium by viewModel.isPremium.collectAsStateWithLifecycle()
    var simulatedPurchase by rememberSaveable { mutableStateOf(false) }

    UnlockSheet(
        gamesUsed = gamesUsed.coerceIn(0, 2),
        isPremium = premium || simulatedPurchase,
        onPurchase = { simulatedPurchase = true },
        onRestore = { simulatedPurchase = true },
        onClose = onClose,
    )
}
