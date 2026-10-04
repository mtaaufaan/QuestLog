package com.rds.questlog.domain.usecase.reader

import com.rds.questlog.domain.model.ReadMode
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SetReadModeUseCase @Inject constructor(private val repository: ArticleRepository) {

    /** Menyimpan mode baca per artikel (QL-6) agar tidak perlu diubah lagi saat artikel dibuka kembali. */
    suspend operator fun invoke(articleId: Long, mode: ReadMode): Result<Unit> =
        writing { repository.setReadMode(articleId, mode) }
}
