package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.relation.GameWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query(
        "SELECT g.id AS id, g.title AS name, g.created_at AS createdAt, COUNT(a.id) AS articleCount " +
            "FROM games g LEFT JOIN articles a ON a.game_id = g.id " +
            "GROUP BY g.id ORDER BY g.created_at ASC, g.id ASC",
    )
    fun observeWithArticleCount(): Flow<List<GameWithCount>>

    @Query("SELECT * FROM games WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findByTitle(title: String): GameEntity?

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int

    @Insert
    suspend fun insert(game: GameEntity): Long
}
