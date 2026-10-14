package com.rds.questlog.di

import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleNoteDao
import com.rds.questlog.data.local.dao.BookmarkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAO fitur premium v1 (bookmark dan catatan); dipisah dari [AppModule] agar modul itu tetap ringkas. */
@Module
@InstallIn(SingletonComponent::class)
object PremiumDaoModule {

    @Provides
    fun provideBookmarkDao(db: QuestLogDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideArticleNoteDao(db: QuestLogDatabase): ArticleNoteDao = db.articleNoteDao()
}
