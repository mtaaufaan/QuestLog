package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "source_pages",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SourcePageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "page_order") val pageOrder: Int,
    /** PENDING | IN_PROGRESS | COMPLETED | FAILED */
    @ColumnInfo(defaultValue = "'PENDING'") val status: String = "PENDING",
    /** Awal slot display_order yang dipesan untuk page ini (mis. 1, 1001, 2001). */
    @ColumnInfo(name = "order_start") val orderStart: Int? = null,
    /** Akhir slot display_order yang dipesan untuk page ini (mis. 1000, 2000, 3000). */
    @ColumnInfo(name = "order_end") val orderEnd: Int? = null,
    /** Kode alasan gagal (mis. "HTTP:404", "TIMEOUT"); null bila tidak gagal. Ditambahkan di skema v2. */
    @ColumnInfo(name = "failure_reason") val failureReason: String? = null,
)
