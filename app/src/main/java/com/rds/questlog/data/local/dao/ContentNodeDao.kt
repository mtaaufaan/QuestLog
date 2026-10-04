package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.ContentNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentNodeDao {

    @Insert
    suspend fun insertAll(nodes: List<ContentNodeEntity>)

    /** Dipakai sebelum menyimpan ulang satu halaman agar tidak ada node ganda. */
    @Query("DELETE FROM content_nodes WHERE source_page_id = :pageId")
    suspend fun deleteByPage(pageId: Long)

    /** Seluruh konten artikel berurutan menurut display_order (urutan halaman, lalu urutan dalam halaman). */
    @Query("SELECT * FROM content_nodes WHERE article_id = :articleId ORDER BY display_order")
    fun observeByArticle(articleId: Long): Flow<List<ContentNodeEntity>>

    @Query("SELECT display_order FROM content_nodes WHERE id = :nodeId AND article_id = :articleId")
    suspend fun displayOrderOf(articleId: Long, nodeId: Long): Int?

    @Query("SELECT COUNT(*) FROM content_nodes WHERE article_id = :articleId")
    suspend fun countByArticle(articleId: Long): Int
}
