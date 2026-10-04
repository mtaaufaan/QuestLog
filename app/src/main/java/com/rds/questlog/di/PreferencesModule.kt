package com.rds.questlog.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.AppConfigDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** DataStore preferensi tampilan app-wide (ukuran teks, mode gelap); diakses lewat UserPreferencesRepository. */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    fun provideAppConfigDao(db: QuestLogDatabase): AppConfigDao = db.appConfigDao()

    @Provides
    @Singleton
    fun provideDisplayDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("display_preferences") }
}
