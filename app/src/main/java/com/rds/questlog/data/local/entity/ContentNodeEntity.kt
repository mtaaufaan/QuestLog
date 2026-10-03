package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "content_nodes",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SourcePageEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_page_id"],
        ),
    ],
    indices = [
        Index(value = ["article_id", "display_order"], name = "idx_content_nodes_article_order"),
        Index(value = ["source_page_id"], name = "idx_content_nodes_source_page"),
    ],
)
data class ContentNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(name = "source_page_id") val sourcePageId: Long,
    /** h1, h2, h3, p, p_cont, img, pre, table, li */
    @ColumnInfo(name = "node_type") val nodeType: String,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    @ColumnInfo(name = "text_content") val textContent: String? = null,
    /** img: {"path","alt"}; table: {"html"}. */
    @ColumnInfo(name = "metadata_json") val metadataJson: String? = null,
)
