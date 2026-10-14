package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {

    /**
     * Stream game yang punya artikel (terlama dulu) beserta jumlah artikel dan artikel terakhir dibaca; emit ulang
     * tiap data berubah.
     */
    fun observeGames(): Flow<List<Game>>

    /** Id game bernama [name] (case-insensitive), atau null bila belum ada. */
    suspend fun findIdByName(name: String): Long?

    /** Jumlah game yang tersimpan (untuk batas tier gratis). */
    suspend fun count(): Int

    /** Mengembalikan id game bernama [name] (case-insensitive); membuatnya bila belum ada. */
    suspend fun findOrCreate(name: String): Long

    /**
     * Menghapus game [gameId] beserta semua artikel, halaman, konten, checkpoint, bookmark, catatan, penanda artikel
     * utama, kategori, dan file gambar yang tidak dipakai lagi.
     */
    suspend fun deleteGame(gameId: Long)

    /** Menghapus game [gameId] hanya bila tidak punya artikel lagi; no-op bila masih berisi. */
    suspend fun deleteIfEmpty(gameId: Long)
}
