package com.rds.questlog.domain.usecase.reader

import com.rds.questlog.domain.model.ArticleDetail
import com.rds.questlog.domain.repository.ArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetArticleDetailUseCase @Inject constructor(private val repository: ArticleRepository) {

    /**
     * Stream artikel beserta seluruh kontennya untuk Reader; null bila artikel tidak ada (sudah dihapus).
     * Emit ulang saat halaman atau konten berubah (mis. retry halaman gagal selesai).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(articleId: Long): Flow<ArticleDetail?> =
        repository.observeArticle(articleId).flatMapLatest { a ->
            if (a == null) flowOf(null) else repository.observeContent(articleId).map { ArticleDetail(a, it) }
        }
}
