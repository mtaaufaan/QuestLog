package com.rds.questlog.presentation.notification

import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.model.SourcePageStatus.COMPLETED
import com.rds.questlog.domain.model.SourcePageStatus.FAILED
import com.rds.questlog.domain.model.SourcePageStatus.IN_PROGRESS
import com.rds.questlog.domain.model.SourcePageStatus.PENDING
import com.rds.questlog.presentation.model.ScrapeNotificationKind
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrapeNotificationMappingTest {

    private fun article(vararg statuses: SourcePageStatus) = Article(
        id = 3,
        gameId = 1,
        gameName = "G",
        title = "Judul",
        pages = statuses.mapIndexed { i, s -> SourcePage(i + 1L, "https://x.com/$i", i + 1, s) },
        checkpointNodeId = null,
        lastVisitedNodeId = null,
        lastReadAt = null,
        createdAt = 0,
    )

    @Test
    fun `progres menghitung halaman selesai dan gagal selama masih ada yang diproses`() {
        val n = article(COMPLETED, FAILED, IN_PROGRESS, PENDING).toNotification()
        assertEquals(ScrapeNotificationKind.PROGRESS, n.kind)
        assertEquals(2, n.current)
        assertEquals(4, n.total)
    }

    @Test
    fun `varian akhir mengikuti hasil - selesai, parsial, gagal`() {
        assertEquals(ScrapeNotificationKind.DONE, article(COMPLETED, COMPLETED).toNotification().kind)
        val partial = article(COMPLETED, FAILED).toNotification()
        assertEquals(ScrapeNotificationKind.PARTIAL, partial.kind)
        assertEquals(1, partial.failedCount)
        assertEquals(ScrapeNotificationKind.ERROR, article(FAILED, FAILED).toNotification().kind)
    }

    @Test
    fun `unduh ulang hanya menghitung halaman yang dipilih dan menandai varian unduh ulang`() {
        val one = article(COMPLETED, PENDING, COMPLETED).toNotification(pageIds = setOf(2L))
        assertEquals(ScrapeNotificationKind.PROGRESS, one.kind)
        assertEquals(0, one.current)
        assertEquals(1, one.total)
        assertEquals(true, one.isRefresh)

        val both = setOf(1L, 2L)
        assertEquals(ScrapeNotificationKind.DONE, article(COMPLETED, COMPLETED).toNotification(pageIds = both).kind)
        assertEquals(ScrapeNotificationKind.ERROR, article(COMPLETED, FAILED).toNotification(pageIds = setOf(2L)).kind)
        assertEquals(ScrapeNotificationKind.PARTIAL, article(COMPLETED, FAILED).toNotification(pageIds = both).kind)
        assertEquals(false, article(COMPLETED).toNotification().isRefresh)
    }

    @Test
    fun `mode lengkapi hanya menghitung halaman baru`() {
        val n = article(COMPLETED, COMPLETED, COMPLETED, PENDING).toNotification(skipPages = 3)
        assertEquals(ScrapeNotificationKind.PROGRESS, n.kind)
        assertEquals(0, n.current)
        assertEquals(1, n.total)
        assertEquals(
            ScrapeNotificationKind.DONE,
            article(COMPLETED, COMPLETED, COMPLETED, COMPLETED).toNotification(3).kind,
        )
    }
}
