package com.rds.questlog.domain.model

/** Game tujuan saat mengubah artikel (QL-18). */
sealed interface GameTarget {
    /** Artikel tetap di game-nya sekarang. */
    data object Unchanged : GameTarget

    /** Pindah ke game yang sudah ada. */
    data class Existing(val gameId: Long) : GameTarget

    /** Pindah ke game baru bernama [name]; dipakai game yang sudah bernama sama bila ada (case-insensitive). */
    data class New(val name: String) : GameTarget
}
