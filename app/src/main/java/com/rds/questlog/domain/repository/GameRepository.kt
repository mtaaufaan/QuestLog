package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {

    /** Stream semua game (terlama dulu) beserta jumlah artikelnya; emit ulang tiap data berubah. */
    fun observeGames(): Flow<List<Game>>

    /** Mengembalikan id game bernama [name] (case-insensitive); membuatnya bila belum ada. */
    suspend fun findOrCreate(name: String): Long
}
