package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.ContentNodeEntity

@Dao
interface ContentNodeDao {

    @Insert
    suspend fun insertAll(nodes: List<ContentNodeEntity>)

    /** Dipakai sebelum menyimpan ulang satu halaman agar tidak ada node ganda. */
    @Query("DELETE FROM content_nodes WHERE source_page_id = :pageId")
    suspend fun deleteByPage(pageId: Long)

    @Query("SELECT COUNT(*) FROM content_nodes WHERE article_id = :articleId")
    suspend fun countByArticle(articleId: Long): Int
}
