package com.rds.questlog.di

import com.rds.questlog.data.prefs.DataStoreUserPreferencesRepository
import com.rds.questlog.data.repository.AppConfigRepositoryImpl
import com.rds.questlog.data.repository.ArticleManagementRepositoryImpl
import com.rds.questlog.data.repository.ArticleRepositoryImpl
import com.rds.questlog.data.repository.BinderRepositoryImpl
import com.rds.questlog.data.repository.BookmarkRepositoryImpl
import com.rds.questlog.data.repository.CheckpointRepositoryImpl
import com.rds.questlog.data.repository.GameProgressRepositoryImpl
import com.rds.questlog.data.repository.GameRepositoryImpl
import com.rds.questlog.data.repository.NoteRepositoryImpl
import com.rds.questlog.data.repository.SourcePageRepositoryImpl
import com.rds.questlog.data.scraper.KsoupScraperEngine
import com.rds.questlog.data.worker.WorkManagerScrapeScheduler
import com.rds.questlog.domain.repository.AppConfigRepository
import com.rds.questlog.domain.repository.ArticleManagementRepository
import com.rds.questlog.domain.repository.ArticleRepository
import com.rds.questlog.domain.repository.BinderRepository
import com.rds.questlog.domain.repository.BookmarkRepository
import com.rds.questlog.domain.repository.CheckpointRepository
import com.rds.questlog.domain.repository.GameProgressRepository
import com.rds.questlog.domain.repository.GameRepository
import com.rds.questlog.domain.repository.NoteRepository
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
    abstract fun bindArticleManagementRepository(impl: ArticleManagementRepositoryImpl): ArticleManagementRepository

    @Binds
    @Singleton
    abstract fun bindSourcePageRepository(impl: SourcePageRepositoryImpl): SourcePageRepository

    @Binds
    @Singleton
    abstract fun bindAppConfigRepository(impl: AppConfigRepositoryImpl): AppConfigRepository

    @Binds
    @Singleton
    abstract fun bindCheckpointRepository(impl: CheckpointRepositoryImpl): CheckpointRepository

    @Binds
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    abstract fun bindGameProgressRepository(impl: GameProgressRepositoryImpl): GameProgressRepository

    @Binds
    abstract fun bindBinderRepository(impl: BinderRepositoryImpl): BinderRepository

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
