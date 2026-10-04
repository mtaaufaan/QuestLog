package com.rds.questlog.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.rds.questlog.BuildConfig
import com.rds.questlog.data.billing.CurrentActivityHolder
import com.rds.questlog.data.billing.FakeBillingService
import com.rds.questlog.data.billing.FullVersionBillingService
import com.rds.questlog.data.billing.GooglePlayBillingService
import com.rds.questlog.data.local.MIGRATION_1_2
import com.rds.questlog.data.local.MIGRATION_2_3
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.CheckpointDao
import com.rds.questlog.data.local.dao.ContentNodeDao
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.dao.ImageDao
import com.rds.questlog.data.local.dao.SourcePageDao
import com.rds.questlog.domain.repository.BillingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

// @Provides hanya untuk class pihak ketiga; binding interface repository memakai @Binds di RepositoryModule.
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuestLogDatabase =
        Room.databaseBuilder(context, QuestLogDatabase::class.java, "questlog.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideGameDao(db: QuestLogDatabase): GameDao = db.gameDao()

    @Provides
    fun provideArticleDao(db: QuestLogDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideSourcePageDao(db: QuestLogDatabase): SourcePageDao = db.sourcePageDao()

    @Provides
    fun provideContentNodeDao(db: QuestLogDatabase): ContentNodeDao = db.contentNodeDao()

    @Provides
    fun provideImageDao(db: QuestLogDatabase): ImageDao = db.imageDao()

    @Provides
    fun provideCheckpointDao(db: QuestLogDatabase): CheckpointDao = db.checkpointDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("questlog_prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideBillingService(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
        activities: CurrentActivityHolder,
    ): BillingService = when {
        BuildConfig.FULL_VERSION -> FullVersionBillingService()
        BuildConfig.DEBUG -> FakeBillingService(prefs)
        else -> GooglePlayBillingService(context, activities)
    }
}
