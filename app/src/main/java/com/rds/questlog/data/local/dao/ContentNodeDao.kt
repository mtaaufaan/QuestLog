package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.data.local.relation.NodeRef
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

    /** Seluruh node satu halaman berurutan; dipakai mencocokkan ulang checkpoint saat halaman diunduh ulang. */
    @Query("SELECT * FROM content_nodes WHERE source_page_id = :pageId ORDER BY display_order")
    suspend fun getByPage(pageId: Long): List<ContentNodeEntity>

    /** Menggeser display_order seluruh node satu halaman sebesar [delta] (halaman pindah slot). */
    @Query("UPDATE content_nodes SET display_order = display_order + :delta WHERE source_page_id = :pageId")
    suspend fun shiftOrder(pageId: Long, delta: Int)

    /** Node terakhir milik halaman lain yang berada sebelum [before]; null bila tidak ada. */
    @Query(
        "SELECT id, display_order AS displayOrder FROM content_nodes " +
            "WHERE article_id = :articleId AND source_page_id != :pageId AND display_order < :before " +
            "ORDER BY display_order DESC LIMIT 1",
    )
    suspend fun lastBefore(articleId: Long, pageId: Long, before: Int): NodeRef?

    /** Node pertama milik halaman lain yang berada setelah [after]; null bila tidak ada. */
    @Query(
        "SELECT id, display_order AS displayOrder FROM content_nodes " +
            "WHERE article_id = :articleId AND source_page_id != :pageId AND display_order > :after " +
            "ORDER BY display_order ASC LIMIT 1",
    )
    suspend fun firstAfter(articleId: Long, pageId: Long, after: Int): NodeRef?
}
