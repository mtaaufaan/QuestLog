package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bookmark (v1.0, premium): banyak per artikel, terpisah dari checkpoint. Anchor ke id node (bukan posisi scroll);
 * [nodeId] sengaja tanpa ON DELETE CASCADE karena dipindahkan manual saat halaman dihapus atau diunduh ulang
 * (database_schema.md, Operasi Pengelolaan Artikel). Hapus artikel menghapus bookmark-nya (CASCADE).
 */
@Entity(
    tableName = "bookmarks",
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
            childColumns = ["node_id"],
        ),
    ],
    indices = [
        Index(value = ["article_id"], name = "idx_bookmarks_article"),
        Index(value = ["node_id"], name = "idx_bookmarks_node"),
    ],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(name = "node_id") val nodeId: Long,
    /** display_order node anchor; dipakai bila node hilang. */
    @ColumnInfo(name = "fallback_order") val fallbackOrder: Int,
    /** Wajib diisi pengguna, maks. 60 karakter (ditegakkan di lapisan UseCase). */
    val label: String,
    /** Cuplikan teks elemen anchor saat bookmark dibuat; ditampilkan di daftar bookmark. */
    @ColumnInfo(defaultValue = "''") val snippet: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
