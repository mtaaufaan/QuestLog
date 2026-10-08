package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.rds.questlog.data.local.entity.SourcePageEntity

/** Tata letak halaman sumber artikel (Sprint 5): urutan, slot display_order, dan penghapusan. */
@Dao
interface PageLayoutDao {

    @Query("SELECT * FROM source_pages WHERE article_id = :articleId ORDER BY page_order")
    suspend fun getByArticle(articleId: Long): List<SourcePageEntity>

    /** Menyetel urutan dan slot display_order satu halaman (dipakai saat hapus dan urut ulang halaman). */
    @Query("UPDATE source_pages SET page_order = :order, order_start = :start, order_end = :end WHERE id = :pageId")
    suspend fun setPosition(pageId: Long, order: Int, start: Int, end: Int)

    @Query("DELETE FROM source_pages WHERE id = :pageId")
    suspend fun delete(pageId: Long)
}
