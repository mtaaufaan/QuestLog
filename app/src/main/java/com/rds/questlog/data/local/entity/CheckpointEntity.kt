package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContentNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["anchor_node_id"],
        ),
        ForeignKey(
            entity = ContentNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["last_visited_node_id"],
        ),
    ],
)
data class CheckpointEntity(
    @PrimaryKey @ColumnInfo(name = "article_id") val articleId: Long,
    /** Checkpoint MANUAL yang disengaja user. */
    @ColumnInfo(name = "anchor_node_id") val anchorNodeId: Long,
    /** display_order anchor node; dipakai bila anchor terhapus setelah re-scrape. */
    @ColumnInfo(name = "fallback_order") val fallbackOrder: Int,
    /** Posisi terakhir AUTO-SAVED saat user meninggalkan Reader. */
    @ColumnInfo(name = "last_visited_node_id") val lastVisitedNodeId: Long? = null,
    /** Skema siap, UI ditunda ke v2. */
    @ColumnInfo(name = "last_read_at") val lastReadAt: Long? = null,
    /** 0.0-1.0; skema siap, UI ditunda ke v2. */
    @ColumnInfo(name = "read_progress") val readProgress: Double? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
