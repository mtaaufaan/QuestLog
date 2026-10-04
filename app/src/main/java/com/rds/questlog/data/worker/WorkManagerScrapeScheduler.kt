package com.rds.questlog.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.rds.questlog.domain.scraper.ScrapeScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerScrapeScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScrapeScheduler {

    override fun schedule(articleId: Long) {
        val request = OneTimeWorkRequestBuilder<ScrapeArticleWorker>()
            .setInputData(workDataOf(ScrapeArticleWorker.KEY_ARTICLE_ID to articleId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // Satu antrean per artikel; retry saat worker masih berjalan ditambahkan di belakangnya.
        WorkManager.getInstance(context)
            .enqueueUniqueWork("scrape-$articleId", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}
