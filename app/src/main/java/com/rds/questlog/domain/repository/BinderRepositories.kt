package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.Binder
import com.rds.questlog.domain.model.BinderCategory
import kotlinx.coroutines.flow.Flow

/** Artikel utama game ("Lanjutkan Game"): satu per game, hanya menunjuk artikelnya. */
interface GameProgressRepository {

    /** Menjadikan [articleId] artikel utama game-nya; menimpa yang lama. Gagal bila artikel tidak ada. */
    suspend fun setMainArticle(articleId: Long)

    /** Id artikel utama game [gameId], atau null bila belum ada (atau sudah terhapus/dipindah). */
    suspend fun mainArticleId(gameId: Long): Long?
}

/** Kategori Game Binder dan penetapan artikel (premium). Validasi nama dilakukan UseCase, bukan di sini. */
interface BinderRepository {

    /** Stream kategori dan penetapan artikel game [gameId]; emit ulang tiap berubah. */
    fun observe(gameId: Long): Flow<Binder>

    /** Kategori bernama [name] di game [gameId] (tanpa peduli huruf besar-kecil), atau null. */
    suspend fun findByName(gameId: Long, name: String): BinderCategory?

    /** Kategori [id], atau null bila sudah terhapus. */
    suspend fun category(id: Long): BinderCategory?

    /** Membuat kategori di akhir urutan game [gameId]; nama sudah dinormalisasi. Mengembalikan id baru. */
    suspend fun createCategory(gameId: Long, name: String): Long

    /** Mengganti nama kategori [id]; nama sudah dinormalisasi dan diperiksa unik. */
    suspend fun renameCategory(id: Long, name: String)

    /** Menghapus kategori; artikel tidak ikut terhapus, hanya relasinya dicabut. */
    suspend fun deleteCategory(id: Long)

    /** Memasukkan atau mengeluarkan [articleId] dari kategori [categoryId]; kategori harus milik game artikel itu. */
    suspend fun setMembership(articleId: Long, categoryId: Long, member: Boolean)

    /**
     * Menetapkan kategori [articleId] persis sebesar [categoryIds] ditambah kategori bernama [newNames] (dibuat bila
     * belum ada, dipakai ulang bila sudah ada), dalam satu transaksi. Id yang bukan milik game artikel diabaikan.
     */
    suspend fun assign(articleId: Long, categoryIds: Set<Long>, newNames: List<String>)
}
