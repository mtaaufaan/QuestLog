package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "article_id") val articleId: Long,
    /** MD5(source_url) + ".webp" */
    val filename: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_size") val fileSize: Long? = null,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
