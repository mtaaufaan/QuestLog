package com.rds.questlog.domain.usecase.game

import com.rds.questlog.domain.repository.GameLogoRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SetGameLogoUseCase @Inject constructor(private val repository: GameLogoRepository) {

    /**
     * Memasang gambar [uri] sebagai logo game [gameId] (persegi, 256x256, WebP). Gagal -> `ImageError.InvalidImage`,
     * `ImageError.TooLarge` (> 10 MB), atau `ImageError.IoError`; tidak pernah melempar.
     */
    suspend operator fun invoke(gameId: Long, uri: String): Result<Unit> = writing { repository.set(gameId, uri) }
}

class RemoveGameLogoUseCase @Inject constructor(private val repository: GameLogoRepository) {

    /** Mencabut logo game [gameId]; masih bisa dipulihkan lewat [RestoreGameLogoUseCase] sampai logo diubah lagi. */
    suspend operator fun invoke(gameId: Long): Result<Unit> = writing { repository.remove(gameId) }
}

class RestoreGameLogoUseCase @Inject constructor(private val repository: GameLogoRepository) {

    /** Urungkan hapus logo game [gameId]. */
    suspend operator fun invoke(gameId: Long): Result<Unit> = writing { repository.restore(gameId) }
}
