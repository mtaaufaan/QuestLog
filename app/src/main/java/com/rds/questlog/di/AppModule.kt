package com.rds.questlog.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.rds.questlog.BuildConfig
import com.rds.questlog.data.billing.FakeBillingService
import com.rds.questlog.data.billing.FullVersionBillingService
import com.rds.questlog.data.billing.GooglePlayBillingService
import com.rds.questlog.data.local.QuestLogDatabase
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
        Room.databaseBuilder(context, QuestLogDatabase::class.java, "questlog.db").build()

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
    fun provideBillingService(prefs: SharedPreferences): BillingService = when {
        BuildConfig.FULL_VERSION -> FullVersionBillingService()
        BuildConfig.DEBUG -> FakeBillingService(prefs)
        else -> GooglePlayBillingService()
    }
}
