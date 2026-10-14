package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rds.questlog.data.local.entity.ArticleBinderEntity
import com.rds.questlog.data.local.entity.BinderCategoryEntity
import com.rds.questlog.data.local.entity.GameProgressEntity
import com.rds.questlog.data.local.relation.AssignmentRow
import kotlinx.coroutines.flow.Flow

/** Artikel utama game; satu baris per game (`game_id` PK). */
@Dao
interface GameProgressDao {

    /** Menimpa artikel utama lama game ini. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: GameProgressEntity)

    @Query("SELECT article_id FROM game_progress WHERE game_id = :gameId")
    suspend fun articleIdOf(gameId: Long): Long?
}

/** Kategori Binder dan relasi artikel-kategori. */
@Dao
interface BinderDao {

    @Query("SELECT * FROM binder_categories WHERE game_id = :gameId ORDER BY position, id")
    fun observeCategories(gameId: Long): Flow<List<BinderCategoryEntity>>

    @Query("SELECT * FROM binder_categories WHERE id = :id")
    suspend fun category(id: Long): BinderCategoryEntity?

    @Query("SELECT * FROM binder_categories WHERE game_id = :gameId AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(gameId: Long, name: String): BinderCategoryEntity?

    @Query("SELECT COALESCE(MAX(position), 0) FROM binder_categories WHERE game_id = :gameId")
    suspend fun maxPosition(gameId: Long): Int

    @Insert
    suspend fun insert(category: BinderCategoryEntity): Long

    @Query("UPDATE binder_categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** Menghapus kategori; relasi artikelnya ikut terhapus lewat CASCADE, artikelnya tidak. */
    @Query("DELETE FROM binder_categories WHERE id = :id")
    suspend fun delete(id: Long)
}

/** Relasi artikel-kategori Binder (M:N). */
@Dao
interface BinderMemberDao {

    @Query(
        "SELECT ab.article_id, ab.category_id FROM article_binder ab " +
            "JOIN binder_categories bc ON bc.id = ab.category_id WHERE bc.game_id = :gameId",
    )
    fun observeAssignments(gameId: Long): Flow<List<AssignmentRow>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(row: ArticleBinderEntity)

    @Query("DELETE FROM article_binder WHERE article_id = :articleId AND category_id = :categoryId")
    suspend fun removeMember(articleId: Long, categoryId: Long)

    /** Mencabut semua kategori [articleId] di game [gameId] (kategori game lain tidak ada). */
    @Query(
        "DELETE FROM article_binder WHERE article_id = :articleId AND category_id IN " +
            "(SELECT id FROM binder_categories WHERE game_id = :gameId)",
    )
    suspend fun clearMembers(articleId: Long, gameId: Long)
}
