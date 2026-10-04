package com.rds.questlog.domain.usecase.reader

import com.rds.questlog.domain.repository.CheckpointRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SetCheckpointUseCase @Inject constructor(private val repository: CheckpointRepository) {

    /**
     * Menandai [nodeId] sebagai checkpoint manual artikel (QL-7); hanya satu aktif per artikel, yang baru menimpa
     * yang lama. Gagal -> Result.failure(DatabaseError.WriteFailed).
     */
    suspend operator fun invoke(articleId: Long, nodeId: Long): Result<Unit> =
        writing { repository.setCheckpoint(articleId, nodeId) }
}
