package com.rds.questlog.data.local.relation

import androidx.room.ColumnInfo

/** Baris daftar bookmark per game: bookmark digabung judul artikelnya. */
data class BookmarkRow(
    val id: Long,
    @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(name = "article_title") val articleTitle: String,
    @ColumnInfo(name = "node_id") val nodeId: Long,
    val label: String,
    val snippet: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
