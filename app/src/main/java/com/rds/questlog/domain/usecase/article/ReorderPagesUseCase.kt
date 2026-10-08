package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class ReorderPagesUseCase @Inject constructor(private val management: ArticleManagementRepository) {

    /**
     * Menyimpan urutan halaman baru (QL-19). [orderedPageIds] harus memuat tepat semua halaman artikel; konten
     * dan checkpoint ikut menyesuaikan sehingga Reader langsung memakai urutan baru.
     * Gagal → `Result.failure` berisi ArticleError.PageNotFound atau DatabaseError.WriteFailed; atomik.
     */
    suspend operator fun invoke(articleId: Long, orderedPageIds: List<Long>): Result<Unit> =
        writing { management.reorderPages(articleId, orderedPageIds) }
}
