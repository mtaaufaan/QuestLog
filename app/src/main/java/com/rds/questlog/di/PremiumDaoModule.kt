package com.rds.questlog.di

import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleNoteDao
import com.rds.questlog.data.local.dao.BinderDao
import com.rds.questlog.data.local.dao.BookmarkDao
import com.rds.questlog.data.local.dao.GameProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO fitur v1.0/v1.1 (bookmark, catatan, artikel utama, Binder); dipisah dari [AppModule]. */
@Module
@InstallIn(SingletonComponent::class)
object PremiumDaoModule {

    @Provides
    fun provideBookmarkDao(db: QuestLogDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideArticleNoteDao(db: QuestLogDatabase): ArticleNoteDao = db.articleNoteDao()

    @Provides
    fun provideGameProgressDao(db: QuestLogDatabase): GameProgressDao = db.gameProgressDao()

    @Provides
    fun provideBinderDao(db: QuestLogDatabase): BinderDao = db.binderDao()
}
