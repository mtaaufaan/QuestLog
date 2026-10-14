package com.rds.questlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Kategori Game Binder (v1.1, premium): bebas buatan pengguna, milik satu game. Nama unik per game tanpa peduli huruf
 * besar-kecil (indeks unik pada kolom ber-collation NOCASE); trim dan batas 100 karakter ditegakkan UseCase. Hapus game
 * menghapus kategorinya (CASCADE).
 */
@Entity(
    tableName = "binder_categories",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["game_id"], name = "idx_binder_categories_game"),
        Index(value = ["game_id", "name"], name = "idx_binder_categories_game_name", unique = true),
    ],
)
data class BinderCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    /** Urutan tampil di Halaman Game. */
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * Relasi artikel-kategori (M:N). Menghapus kategori hanya mencabut relasinya (CASCADE dari kategori); artikel tidak
 * ikut terhapus. Menghapus artikel mencabut semua relasinya.
 */
@Entity(
    tableName = "article_binder",
    primaryKeys = ["article_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["article_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BinderCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["category_id"], name = "idx_article_binder_category")],
)
data class ArticleBinderEntity(
    @ColumnInfo(name = "article_id") val articleId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
)
