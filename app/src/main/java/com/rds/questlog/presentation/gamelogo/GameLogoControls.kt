package com.rds.questlog.presentation.gamelogo

import androidx.compose.runtime.Stable

/** Yang dibutuhkan layar untuk menampilkan [com.rds.questlog.presentation.components.GameLogo]. */
@Stable
class GameLogoControls(
    /** Game yang logonya sedang diproses (spinner di atas logo); null = tidak ada. */
    val busyGameId: Long?,
    /** Ketuk logo game [gameId]: belum ada logo -> Photo Picker; sudah ada -> sheet Ganti/Hapus. */
    val onTap: (gameId: Long, hasLogo: Boolean) -> Unit,
) {
    companion object {
        /** Tanpa interaksi (preview). */
        val None = GameLogoControls(null) { _, _ -> }
    }
}
