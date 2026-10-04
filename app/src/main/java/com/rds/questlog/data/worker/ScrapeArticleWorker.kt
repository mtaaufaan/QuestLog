package com.rds.questlog.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rds.questlog.domain.usecase.article.ScrapeArticleUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Menjalankan scraping halaman PENDING satu artikel di latar belakang. Kegagalan per halaman dicatat di database. */
@HiltWorker
class ScrapeArticleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scrapeArticle: ScrapeArticleUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getLong(KEY_ARTICLE_ID, NO_ARTICLE)
        if (articleId == NO_ARTICLE) return Result.failure()
        scrapeArticle(articleId)
        return Result.success()
    }

    companion object {
        const val KEY_ARTICLE_ID = "article_id"
        private const val NO_ARTICLE = -1L
    }
}
