package com.rds.questlog.presentation.navigation

import com.rds.questlog.presentation.model.ScrapeNotificationKind
import com.rds.questlog.presentation.model.ScrapeNotificationUi
import org.junit.Assert.assertEquals
import org.junit.Test

class GameNavigationTest {

    @Test
    fun `kembali cukup satu layar bila di bawahnya sudah Halaman Game yang sama`() {
        assertEquals(GamePageAction.BACK, gamePageAction(belowGameId = 4, gameId = 4))
    }

    @Test
    fun `dari S0, pencarian, atau game lain halaman game dibuka di atas S0`() {
        assertEquals(GamePageAction.REPLACE, gamePageAction(belowGameId = null, gameId = 4))
        assertEquals(GamePageAction.REPLACE, gamePageAction(belowGameId = 9, gameId = 4))
    }

    private fun notification(kind: ScrapeNotificationKind) = ScrapeNotificationUi(
        kind = kind,
        articleTitle = "A",
        current = 1,
        total = 1,
        articleId = 7,
        gameId = 3,
    )

    @Test
    fun `notifikasi selesai atau sebagian membuka Reader artikelnya`() {
        listOf(ScrapeNotificationKind.DONE, ScrapeNotificationKind.PARTIAL).forEach {
            assertEquals(NotificationTarget.OpenReader(7), notificationTarget(notification(it)))
        }
    }

    @Test
    fun `notifikasi gagal membuka Halaman Game artikel itu dengan dialog error`() {
        assertEquals(
            NotificationTarget.OpenGameWithError(gameId = 3, articleId = 7),
            notificationTarget(notification(ScrapeNotificationKind.ERROR)),
        )
    }

    @Test
    fun `notifikasi progres tidak berpindah layar`() {
        assertEquals(NotificationTarget.None, notificationTarget(notification(ScrapeNotificationKind.PROGRESS)))
    }
}
