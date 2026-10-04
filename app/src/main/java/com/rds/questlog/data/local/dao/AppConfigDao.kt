package com.rds.questlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rds.questlog.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {

    /** Nilai satu key; null bila belum pernah ditulis. Emit ulang tiap barisnya berubah. */
    @Query("SELECT value FROM app_config WHERE key = :key")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<AppConfigEntity>)

    @Query("DELETE FROM app_config WHERE key = :key")
    suspend fun delete(key: String)
}
