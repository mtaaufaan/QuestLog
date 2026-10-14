package com.rds.questlog.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.rds.questlog.presentation.model.ScrapeNotificationKind
import com.rds.questlog.presentation.model.ScrapeNotificationUi

/** Cara menuju Halaman Game: cukup kembali satu layar, atau ganti tumpukan sampai S0 lalu buka halaman itu. */
enum class GamePageAction { BACK, REPLACE }

/**
 * Aturan "kembali selalu ke Halaman Game artikel itu" (QL-37): bila layar tepat di bawah sudah Halaman Game yang sama
 * ([belowGameId], null bila bukan Halaman Game) cukup kembali; selain itu (S0, hasil pencarian, notifikasi) buka
 * Halaman Game [gameId] di atas S0.
 */
fun gamePageAction(belowGameId: Long?, gameId: Long): GamePageAction =
    if (belowGameId == gameId) GamePageAction.BACK else GamePageAction.REPLACE

/** Tujuan saat Scrape Notification diketuk (QL-37). */
sealed interface NotificationTarget {
    data class OpenReader(val articleId: Long) : NotificationTarget

    /** Halaman Game artikel itu, lalu Scrape Error Dialog artikel itu dibuka. */
    data class OpenGameWithError(val gameId: Long, val articleId: Long) : NotificationTarget

    data object None : NotificationTarget
}

/** Selesai atau sebagian membuka Reader, gagal membuka Halaman Game + dialog error, progres tidak berbuat apa-apa. */
fun notificationTarget(n: ScrapeNotificationUi): NotificationTarget = when (n.kind) {
    ScrapeNotificationKind.DONE, ScrapeNotificationKind.PARTIAL -> NotificationTarget.OpenReader(n.articleId)
    ScrapeNotificationKind.ERROR -> NotificationTarget.OpenGameWithError(n.gameId, n.articleId)
    ScrapeNotificationKind.PROGRESS -> NotificationTarget.None
}

/**
 * Menuju Halaman Game [gameId] dari layar yang sedang terbuka (Reader, S2, notifikasi). [gameId] belum dikenal
 * (<= 0, mis. artikel tidak ditemukan) cukup kembali satu layar.
 */
fun NavController.showGamePage(gameId: Long) {
    if (gameId <= 0) {
        popBackStack()
        return
    }
    val below = previousBackStackEntry?.takeIf { it.destination.hasRoute<ArticleList>() }?.toRoute<ArticleList>()
    when (gamePageAction(below?.gameId, gameId)) {
        GamePageAction.BACK -> popBackStack()
        GamePageAction.REPLACE -> navigate(ArticleList(gameId)) { popUpTo<GameList> { inclusive = false } }
    }
}
