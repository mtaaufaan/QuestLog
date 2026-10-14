package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rds.questlog.data.local.entity.ArticleNoteEntity
import kotlinx.coroutines.flow.Flow

/** Akses catatan umum per artikel. */
@Dao
interface ArticleNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: ArticleNoteEntity)

    @Query("SELECT * FROM article_notes WHERE article_id = :articleId")
    suspend fun get(articleId: Long): ArticleNoteEntity?

    @Query("SELECT * FROM article_notes WHERE article_id = :articleId")
    fun observe(articleId: Long): Flow<ArticleNoteEntity?>

    @Query("DELETE FROM article_notes WHERE article_id = :articleId")
    suspend fun delete(articleId: Long)
}
