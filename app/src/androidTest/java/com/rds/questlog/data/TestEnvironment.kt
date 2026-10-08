package com.rds.questlog.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.repository.ArticleManagementRepositoryImpl
import com.rds.questlog.data.repository.ArticleRepositoryImpl
import com.rds.questlog.data.repository.CheckpointRepositoryImpl
import com.rds.questlog.data.repository.GameRepositoryImpl
import com.rds.questlog.data.repository.SourcePageRepositoryImpl
import com.rds.questlog.data.scraper.HtmlContentParser
import com.rds.questlog.data.scraper.ImageDownloader
import com.rds.questlog.data.scraper.ImageStore
import com.rds.questlog.data.scraper.KsoupScraperEngine
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/** Merakit database in-memory beserta repository dan scraper nyata tanpa Hilt, untuk test instrumented. */
class TestEnvironment(timeoutSeconds: Long = 3) {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db: QuestLogDatabase = Room.inMemoryDatabaseBuilder(context, QuestLogDatabase::class.java).build()
    val imageStore = ImageStore(context)
    val games = GameRepositoryImpl(db.gameDao())
    val articles =
        ArticleRepositoryImpl(db, db.articleDao(), db.sourcePageDao(), db.imageDao(), imageStore, db.contentNodeDao())
    val management = ArticleManagementRepositoryImpl(db, db.articleDao(), db.gameDao())
    val checkpoints = CheckpointRepositoryImpl(db, db.checkpointDao(), db.contentNodeDao())
    val pages = SourcePageRepositoryImpl(db, db.sourcePageDao(), db.contentNodeDao(), db.imageDao(), db.articleDao())

    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()
    val scraper = KsoupScraperEngine(client, HtmlContentParser(), ImageDownloader(client, imageStore))

    fun count(sql: String): Int = db.openHelper.writableDatabase.query(sql).use {
        it.moveToFirst()
        it.getInt(0)
    }

    fun strings(sql: String): List<String> = db.openHelper.writableDatabase.query(sql).use { cursor ->
        generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
    }

    fun close() = db.close()
}
