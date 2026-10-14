package com.rds.questlog.data.repository

import com.rds.questlog.data.local.dao.ArticleNoteDao
import com.rds.questlog.data.local.entity.ArticleNoteEntity
import com.rds.questlog.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Penulisan memakai [NonCancellable]: autosave terakhir dipicu saat panel catatan ditutup. */
class NoteRepositoryImpl @Inject constructor(private val noteDao: ArticleNoteDao) : NoteRepository {

    override fun observe(articleId: Long): Flow<String> = noteDao.observe(articleId).map { it?.content.orEmpty() }

    override suspend fun save(articleId: Long, content: String) = withContext(NonCancellable) {
        noteDao.upsert(ArticleNoteEntity(articleId, content, System.currentTimeMillis()))
    }

    override suspend fun delete(articleId: Long) = withContext(NonCancellable) { noteDao.delete(articleId) }
}
