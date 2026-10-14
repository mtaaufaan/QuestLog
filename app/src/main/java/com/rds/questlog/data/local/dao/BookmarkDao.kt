package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.BookmarkEntity

/** Akses bookmark; UseCase dan UI bookmark menyusul di Sprint 7. Di sini yang dibutuhkan pemulihan anchor. */
@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks WHERE article_id = :articleId ORDER BY created_at, id")
    suspend fun getByArticle(articleId: Long): List<BookmarkEntity>

    /** Bookmark yang anchor-nya berada di halaman [pageId]. */
    @Query(
        "SELECT b.* FROM bookmarks b JOIN content_nodes n ON n.id = b.node_id " +
            "WHERE n.source_page_id = :pageId ORDER BY b.id",
    )
    suspend fun getByPage(pageId: Long): List<BookmarkEntity>

    /** Mengarahkan satu bookmark ke node pengganti beserta cadangan posisinya. */
    @Query("UPDATE bookmarks SET node_id = :nodeId, fallback_order = :order WHERE id = :id")
    suspend fun repoint(id: Long, nodeId: Long, order: Int)

    /** Halaman dihapus: semua bookmark di halaman itu pindah ke node terdekat [nodeId]. */
    @Query(
        "UPDATE bookmarks SET node_id = :nodeId, fallback_order = :order " +
            "WHERE node_id IN (SELECT id FROM content_nodes WHERE source_page_id = :pageId)",
    )
    suspend fun moveFromPage(pageId: Long, nodeId: Long, order: Int)

    /** Tidak ada node lain untuk dituju: bookmark di halaman itu dihapus (node_id wajib terisi). */
    @Query("DELETE FROM bookmarks WHERE node_id IN (SELECT id FROM content_nodes WHERE source_page_id = :pageId)")
    suspend fun deleteInPage(pageId: Long)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    /** Menghitung ulang fallback_order dari posisi node sekarang (setelah display_order bergeser). */
    @Query(
        "UPDATE bookmarks SET fallback_order = " +
            "(SELECT display_order FROM content_nodes WHERE id = bookmarks.node_id) WHERE article_id = :articleId",
    )
    suspend fun refreshFallback(articleId: Long)
}
