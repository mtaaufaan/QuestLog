package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    val title: String,
    /** false = WorkManager masih berjalan atau ada page PENDING/IN_PROGRESS; true = semua page COMPLETED/FAILED. */
    @ColumnInfo(name = "is_scraping_done", defaultValue = "0") val isScrapingDone: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /** Mode baca terakhir artikel ini: SEAMLESS | PAGED; null = belum dipilih (default SEAMLESS). Skema v3. */
    @ColumnInfo(name = "read_mode") val readMode: String? = null,
)
