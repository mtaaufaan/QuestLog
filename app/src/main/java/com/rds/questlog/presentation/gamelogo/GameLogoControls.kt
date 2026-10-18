package com.rds.questlog.presentation.gamelogo

import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import com.rds.questlog.presentation.components.LogoBadge
import com.rds.questlog.presentation.model.PremiumUiState
import com.rds.questlog.presentation.navigation.GameLogoEdit

/** Yang dibutuhkan layar untuk menampilkan [com.rds.questlog.presentation.components.GameLogo]. */
@Stable
class GameLogoControls(
    /** Badge kotak logo kosong menurut status premium. */
    val badge: LogoBadge,
    /** Ketuk logo game [gameId]; tujuan bergantung pada status premium (diputuskan layar). */
    val onTap: (gameId: Long) -> Unit,
) {
    companion object {
        /** Tanpa interaksi (preview). */
        val None = GameLogoControls(LogoBadge.PLUS) {}
    }
}

/** Badge menurut status: premium ＋, gratis gembok, premium hilang tanpa badge. */
fun PremiumUiState.logoBadge(): LogoBadge = when (this) {
    PremiumUiState.PREMIUM -> LogoBadge.PLUS
    PremiumUiState.FREE -> LogoBadge.LOCK
    PremiumUiState.LOST -> LogoBadge.NONE
}

/**
 * Kendali logo S0/S1 menurut status premium: premium -> halaman Logo game (P19), gratis -> Unlock Sheet
 * ([onUnlock]), premium hilang -> pesan hanya-baca ([onReadOnly]).
 */
fun gameLogoControls(
    premium: PremiumUiState,
    navController: NavController,
    onUnlock: () -> Unit,
    onReadOnly: () -> Unit,
) = GameLogoControls(premium.logoBadge()) { gameId ->
    when (premium) {
        PremiumUiState.PREMIUM -> navController.navigate(GameLogoEdit(gameId))
        PremiumUiState.FREE -> onUnlock()
        PremiumUiState.LOST -> onReadOnly()
    }
}
