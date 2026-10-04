package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rds.questlog.data.local.entity.CheckpointEntity

@Dao
interface CheckpointDao {

    @Query("SELECT * FROM checkpoints WHERE article_id = :articleId")
    suspend fun get(articleId: Long): CheckpointEntity?

    /** Satu baris per artikel (article_id adalah primary key), jadi REPLACE sama dengan upsert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkpoint: CheckpointEntity)
}
