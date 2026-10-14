package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Logo kustom: path relatif ke `filesDir` (`game_logos/{id}.webp`); null = belum ada. */
    @ColumnInfo(name = "logo_path") val logoPath: String? = null,
)
