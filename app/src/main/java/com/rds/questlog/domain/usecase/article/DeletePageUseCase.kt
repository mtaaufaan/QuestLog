package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class DeletePageUseCase @Inject constructor(private val management: ArticleManagementRepository) {

    /**
     * Menghapus satu halaman artikel (QL-19): konten halaman hilang, nomor halaman dirapatkan, dan checkpoint di
     * halaman itu dipindah ke elemen terdekat. Halaman FAILED boleh dihapus; halaman yang sedang diunduh tidak.
     * Gagal → `Result.failure` berisi ArticleError.LastPage / Busy / PageNotFound atau DatabaseError.WriteFailed;
     * tidak ada yang berubah (atomik).
     */
    suspend operator fun invoke(articleId: Long, pageId: Long): Result<Unit> =
        writing { management.deletePage(articleId, pageId) }
}
