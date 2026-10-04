package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {

    /** Stream semua game (terlama dulu) beserta jumlah artikelnya; emit ulang tiap data berubah. */
    fun observeGames(): Flow<List<Game>>

    /** Id game bernama [name] (case-insensitive), atau null bila belum ada. */
    suspend fun findIdByName(name: String): Long?

    /** Jumlah game yang tersimpan (untuk batas tier gratis). */
    suspend fun count(): Int

    /** Mengembalikan id game bernama [name] (case-insensitive); membuatnya bila belum ada. */
    suspend fun findOrCreate(name: String): Long
}
