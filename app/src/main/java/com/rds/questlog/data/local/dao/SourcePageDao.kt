package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.SourcePageEntity

@Dao
interface SourcePageDao {

    @Insert
    suspend fun insertAll(pages: List<SourcePageEntity>)

    @Query("SELECT COALESCE(MAX(page_order), 0) FROM source_pages WHERE article_id = :articleId")
    suspend fun maxPageOrder(articleId: Long): Int

    /** Akhir slot display_order terbesar milik artikel; 0 bila belum tercatat. */
    @Query("SELECT COALESCE(MAX(order_end), 0) FROM source_pages WHERE article_id = :articleId")
    suspend fun maxOrderEnd(articleId: Long): Int

    @Query("SELECT source_url FROM source_pages WHERE source_url IN (:urls)")
    suspend fun findStoredUrls(urls: List<String>): List<String>

    @Query("SELECT * FROM source_pages WHERE id = :pageId")
    suspend fun getById(pageId: Long): SourcePageEntity?

    @Query("SELECT * FROM source_pages WHERE article_id = :articleId AND status = 'PENDING' ORDER BY page_order")
    suspend fun getPending(articleId: Long): List<SourcePageEntity>

    @Query("SELECT COUNT(*) FROM source_pages WHERE article_id = :articleId AND status IN ('PENDING', 'IN_PROGRESS')")
    suspend fun countUnfinished(articleId: Long): Int

    @Query("UPDATE source_pages SET status = :status, failure_reason = :failureReason WHERE id = :pageId")
    suspend fun setStatus(pageId: Long, status: String, failureReason: String?)

    /** Retry: semua halaman FAILED milik artikel kembali PENDING; halaman COMPLETED tidak disentuh. */
    @Query(
        "UPDATE source_pages SET status = 'PENDING', failure_reason = NULL " +
            "WHERE article_id = :articleId AND status = 'FAILED'",
    )
    suspend fun resetFailed(articleId: Long): Int

    /** Halaman yang tertinggal IN_PROGRESS (proses mati di tengah jalan) kembali PENDING. */
    @Query("UPDATE source_pages SET status = 'PENDING' WHERE article_id = :articleId AND status = 'IN_PROGRESS'")
    suspend fun resetInterrupted(articleId: Long): Int
}
