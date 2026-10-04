package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rds.questlog.data.local.entity.ArticleEntity
import com.rds.questlog.data.local.relation.ArticleWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    /**
     * Search judul (QL-10) dan filter game (QL-11) dikerjakan SQLite. [query] sudah di-escape untuk LIKE
     * (lihat `escapeLike`); LIKE bawaan SQLite tidak peduli huruf besar-kecil untuk ASCII.
     */
    @Transaction
    @Query(
        "SELECT * FROM articles " +
            "WHERE (:gameId IS NULL OR game_id = :gameId) AND title LIKE '%' || :query || '%' ESCAPE '\\' " +
            "ORDER BY created_at DESC, id DESC",
    )
    fun observeArticles(query: String, gameId: Long?): Flow<List<ArticleWithDetails>>

    @Insert
    suspend fun insert(article: ArticleEntity): Long

    @Query("SELECT COUNT(*) FROM articles WHERE game_id = :gameId")
    suspend fun countByGame(gameId: Long): Int

    @Query("UPDATE articles SET is_scraping_done = :done WHERE id = :articleId")
    suspend fun setScrapingDone(articleId: Long, done: Boolean)

    /** Menghapus artikel; source_pages, content_nodes, checkpoints, dan images ikut terhapus lewat FK cascade. */
    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun delete(articleId: Long)
}
