package com.rds.questlog.domain.usecase.reader

import com.rds.questlog.domain.repository.CheckpointRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SaveLastPositionUseCase @Inject constructor(private val repository: CheckpointRepository) {

    /**
     * Menyimpan posisi baca terakhir otomatis (QL-16) saat pengguna meninggalkan Reader. Terpisah dari checkpoint
     * manual: tidak membuat atau mengubah checkpoint. Gagal -> Result.failure(DatabaseError.WriteFailed).
     */
    suspend operator fun invoke(articleId: Long, nodeId: Long): Result<Unit> =
        writing { repository.saveLastPosition(articleId, nodeId) }
}
