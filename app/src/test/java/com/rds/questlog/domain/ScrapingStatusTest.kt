package com.rds.questlog.domain

import com.rds.questlog.domain.model.ScrapingStatus
import com.rds.questlog.domain.model.SourcePageStatus.COMPLETED
import com.rds.questlog.domain.model.SourcePageStatus.FAILED
import com.rds.questlog.domain.model.SourcePageStatus.IN_PROGRESS
import com.rds.questlog.domain.model.SourcePageStatus.PENDING
import com.rds.questlog.domain.model.toScrapingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrapingStatusTest {

    private fun statusOf(vararg statuses: com.rds.questlog.domain.model.SourcePageStatus) =
        statuses.mapIndexed { i, s -> page(i + 1L, status = s) }.toScrapingStatus()

    @Test
    fun `masih ada halaman PENDING atau IN_PROGRESS berarti SCRAPING`() {
        assertEquals(ScrapingStatus.SCRAPING, statusOf(COMPLETED, PENDING))
        assertEquals(ScrapingStatus.SCRAPING, statusOf(FAILED, IN_PROGRESS))
    }

    @Test
    fun `semua halaman gagal berarti ERROR`() {
        assertEquals(ScrapingStatus.ERROR, statusOf(FAILED, FAILED))
    }

    @Test
    fun `sebagian gagal atau semua sukses berarti READY`() {
        assertEquals(ScrapingStatus.READY, statusOf(COMPLETED, FAILED))
        assertEquals(ScrapingStatus.READY, statusOf(COMPLETED, COMPLETED))
    }

    @Test
    fun `retry mengubah ERROR kembali menjadi SCRAPING`() {
        assertEquals(ScrapingStatus.SCRAPING, statusOf(PENDING, PENDING))
    }
}
