package com.rds.questlog.domain.usecase.article

import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.repository.ArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetArticlesUseCase @Inject constructor(private val repository: ArticleRepository) {

    /**
     * Stream artikel untuk S1. [query] di-trim; kosong berarti tanpa search. [gameId] null berarti semua game.
     * Pencarian dan filter dijalankan database sehingga hasilnya selalu konsisten dengan data tersimpan.
     */
    operator fun invoke(query: String, gameId: Long?): Flow<List<Article>> =
        repository.observeArticles(query.trim(), gameId)
}
