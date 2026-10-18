package com.rds.questlog.domain.usecase.game

import com.rds.questlog.domain.model.LogoCrop
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.repository.GameLogoRepository
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SaveGameLogoUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: GameLogoRepository,
) {

    /**
     * Menyimpan logo game [gameId] dari area bingkai [crop] pada gambar [sourceUri] (null = pakai ulang gambar asli
     * yang tersimpan, hanya mengatur ulang posisi). Guard dulu: gratis -> `PremiumError.Locked`, premium hilang ->
     * `PremiumError.ReadOnly`. Gagal -> `ImageError.InvalidImage`, `TooLarge` (> 10 MB), atau `IoError`; tidak
     * pernah melempar.
     */
    suspend operator fun invoke(gameId: Long, sourceUri: String?, crop: LogoCrop): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.GAME_LOGO)
        repository.save(gameId, sourceUri, crop)
    }
}

class RemoveGameLogoUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: GameLogoRepository,
) {

    /** Mencabut logo game [gameId] (lewat guard); masih bisa dipulihkan lewat [RestoreGameLogoUseCase]. */
    suspend operator fun invoke(gameId: Long): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.GAME_LOGO)
        repository.remove(gameId)
    }
}

class RestoreGameLogoUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: GameLogoRepository,
) {

    /** Urungkan hapus logo game [gameId] (lewat guard). */
    suspend operator fun invoke(gameId: Long): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.GAME_LOGO)
        repository.restore(gameId)
    }
}
