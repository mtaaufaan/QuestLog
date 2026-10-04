package com.rds.questlog.di

import com.rds.questlog.data.prefs.DataStoreUserPreferencesRepository
import com.rds.questlog.data.repository.ArticleRepositoryImpl
import com.rds.questlog.data.repository.CheckpointRepositoryImpl
import com.rds.questlog.data.repository.GameRepositoryImpl
import com.rds.questlog.data.repository.SourcePageRepositoryImpl
import com.rds.questlog.data.scraper.KsoupScraperEngine
import com.rds.questlog.data.worker.WorkManagerScrapeScheduler
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.CheckpointRepository
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.repository.SourcePageRepository
import com.rds.questlog.domain.repository.UserPreferencesRepository
import com.rds.questlog.domain.scraper.ScrapeScheduler
import com.rds.questlog.domain.scraper.ScraperEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binding interface domain → implementasi data (coding_convention.md §5: `@Binds`, bukan `@Provides`). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository

    @Binds
    @Singleton
    abstract fun bindArticleRepository(impl: ArticleRepositoryImpl): ArticleRepository

    @Binds
    @Singleton
    abstract fun bindSourcePageRepository(impl: SourcePageRepositoryImpl): SourcePageRepository

    @Binds
    @Singleton
    abstract fun bindCheckpointRepository(impl: CheckpointRepositoryImpl): CheckpointRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindScraperEngine(impl: KsoupScraperEngine): ScraperEngine

    @Binds
    @Singleton
    abstract fun bindScrapeScheduler(impl: WorkManagerScrapeScheduler): ScrapeScheduler
}
