package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.relation.GameWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    /**
     * Game yang punya artikel (game kosong tidak tampil) beserta jumlah artikel dan artikel yang terakhir dibaca
     * (`checkpoints.last_read_at`).
     */
    @Query(
        "SELECT g.id AS id, g.title AS name, g.created_at AS createdAt, COUNT(a.id) AS articleCount, " +
            "(SELECT a2.title FROM articles a2 JOIN checkpoints c2 ON c2.article_id = a2.id " +
            "WHERE a2.game_id = g.id AND c2.last_read_at IS NOT NULL " +
            "ORDER BY c2.last_read_at DESC, a2.id DESC LIMIT 1) AS lastTitle, " +
            "(SELECT MAX(c3.last_read_at) FROM checkpoints c3 JOIN articles a3 ON a3.id = c3.article_id " +
            "WHERE a3.game_id = g.id) AS lastReadAt " +
            "FROM games g JOIN articles a ON a.game_id = g.id " +
            "GROUP BY g.id ORDER BY g.created_at ASC, g.id ASC",
    )
    fun observeWithArticleCount(): Flow<List<GameWithCount>>

    @Query("SELECT * FROM games WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findByTitle(title: String): GameEntity?

    /** Jumlah game yang punya artikel (dasar batas tier); game kosong peninggalan versi lama tidak dihitung. */
    @Query("SELECT COUNT(*) FROM games g WHERE EXISTS (SELECT 1 FROM articles a WHERE a.game_id = g.id)")
    suspend fun count(): Int

    /** Artikel yang pindah dari game [gameId] tidak lagi menjadi artikel utamanya. */
    @Query("DELETE FROM game_progress WHERE game_id = :gameId AND article_id = :articleId")
    suspend fun clearMainArticle(gameId: Long, articleId: Long)

    /** Kategori game lain tidak berlaku di game baru: cabut relasi [articleId] ke kategori game selain [gameId]. */
    @Query(
        "DELETE FROM article_binder WHERE article_id = :articleId AND category_id IN " +
            "(SELECT id FROM binder_categories WHERE game_id != :gameId)",
    )
    suspend fun clearForeignCategories(articleId: Long, gameId: Long)

    /** Menghapus game beserta seluruh artikel, bookmark, catatan, penanda artikel utama, dan kategorinya (CASCADE). */
    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun delete(gameId: Long)

    @Insert
    suspend fun insert(game: GameEntity): Long

    /** Menghapus game hanya bila tidak ada artikel yang memakainya; game berisi tidak disentuh. */
    @Query("DELETE FROM games WHERE id = :gameId AND NOT EXISTS (SELECT 1 FROM articles WHERE game_id = :gameId)")
    suspend fun deleteIfEmpty(gameId: Long)
}
