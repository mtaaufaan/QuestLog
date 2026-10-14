package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Artikel utama sebuah game (v1.1, "Lanjutkan Game"): satu baris per game ([gameId] unik) yang hanya menunjuk
 * artikelnya, tanpa anchor elemen; posisi baca diambil dari checkpoint artikel. Hapus game atau artikel utamanya
 * menghapus baris ini (CASCADE); memindahkan artikel ke game lain dibersihkan manual oleh repository.
 */
@Entity(
    tableName = "game_progress",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["article_id"], name = "idx_game_progress_article")],
)
data class GameProgressEntity(
    @PrimaryKey @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "article_id") val articleId: Long,
)
