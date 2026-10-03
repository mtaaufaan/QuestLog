package com.rds.questlog.di

import android.content.Context
import android.content.SharedPreferences
import com.rds.questlog.BuildConfig
import com.rds.questlog.data.billing.FakeBillingService
import com.rds.questlog.data.billing.FullVersionBillingService
import com.rds.questlog.data.billing.GooglePlayBillingService
import com.rds.questlog.domain.repository.BillingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hanya pemilihan BillingService per build variant; AppModule (database, OkHttp) menyusul di Sprint 0 Tahap 2.
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

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
