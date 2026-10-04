package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.SourcePage

/** Operasi penyimpanan yang dipakai proses scraping per halaman sumber. */
interface SourcePageRepository {

    /** Halaman yang tertinggal IN_PROGRESS (proses sebelumnya mati) dikembalikan ke PENDING. */
    suspend fun recoverInterrupted(articleId: Long)

    /** Halaman PENDING milik artikel, berurutan menurut urutan halaman. */
    suspend fun getPendingPages(articleId: Long): List<SourcePage>

    /** Menandai [pageId] sedang diproses. */
    suspend fun markInProgress(pageId: Long)

    /**
     * Menyimpan [nodes] halaman ke slot display_order miliknya, mencatat [images], dan menandai halaman COMPLETED,
     * seluruhnya dalam satu transaksi.
     */
    suspend fun saveScrapedPage(articleId: Long, pageId: Long, nodes: List<ScrapedNode>, images: List<ScrapedImage>)

    /** Menandai [pageId] FAILED beserta alasannya. */
    suspend fun markFailed(pageId: Long, failure: PageFailure)

    /** Menyetel `is_scraping_done` artikel: true bila tidak ada lagi halaman PENDING/IN_PROGRESS. */
    suspend fun updateScrapingDone(articleId: Long)
}
