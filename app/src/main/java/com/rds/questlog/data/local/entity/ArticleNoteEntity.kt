package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Catatan umum (v1.0, premium): satu per artikel, tidak terikat posisi konten sehingga tidak terpengaruh saat halaman
 * dihapus atau diunduh ulang. Baris dibuat saat pertama disimpan; tanpa baris berarti catatan kosong.
 */
@Entity(
    tableName = "article_notes",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ArticleNoteEntity(
    @PrimaryKey @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(defaultValue = "''") val content: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
