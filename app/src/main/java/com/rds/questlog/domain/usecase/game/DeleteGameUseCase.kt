package com.rds.questlog.domain.usecase.game

import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class DeleteGameUseCase @Inject constructor(private val repository: GameRepository) {

    /**
     * Menghapus game beserta seluruh artikelnya dan data turunannya (bookmark, catatan, penanda artikel utama,
     * kategori, gambar). Gagal -> `Result.failure(DatabaseError.WriteFailed)`; tidak pernah melempar.
     */
    suspend operator fun invoke(gameId: Long): Result<Unit> = writing { repository.deleteGame(gameId) }
}
