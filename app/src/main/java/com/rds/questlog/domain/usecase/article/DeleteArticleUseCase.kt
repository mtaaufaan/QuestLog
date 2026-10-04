package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class DeleteArticleUseCase @Inject constructor(private val repository: ArticleRepository) {

    /**
     * Menghapus artikel beserta halaman, content_nodes, checkpoint, dan gambar terkait.
     * Gagal → `Result.failure(DatabaseError.WriteFailed)`; tidak pernah melempar.
     */
    suspend operator fun invoke(articleId: Long): Result<Unit> = writing { repository.deleteArticle(articleId) }
}
