package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.BookmarkDao
import com.rds.questlog.data.local.dao.ContentNodeDao
import com.rds.questlog.data.local.entity.BookmarkEntity
import com.rds.questlog.data.local.relation.BookmarkRow
import com.rds.questlog.domain.model.Bookmark
import com.rds.questlog.domain.repository.BookmarkRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val bookmarkDao: BookmarkDao,
    private val nodeDao: ContentNodeDao,
) : BookmarkRepository {

    override fun observeByGame(gameId: Long): Flow<List<Bookmark>> =
        bookmarkDao.observeByGame(gameId).map { rows -> rows.map(BookmarkRow::toDomain) }

    override suspend fun add(articleId: Long, nodeId: Long, label: String, snippet: String): Long = db.withTransaction {
        val order = requireNotNull(nodeDao.displayOrderOf(articleId, nodeId)) { "Node $nodeId bukan milik artikel" }
        bookmarkDao.insert(
            BookmarkEntity(
                articleId = articleId,
                nodeId = nodeId,
                fallbackOrder = order,
                label = label,
                snippet = snippet,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateLabel(id: Long, label: String) = bookmarkDao.updateLabel(id, label)

    override suspend fun delete(id: Long) = bookmarkDao.delete(id)
}

private fun BookmarkRow.toDomain() = Bookmark(
    id = id,
    articleId = articleId,
    articleTitle = articleTitle,
    nodeId = nodeId,
    label = label,
    snippet = snippet,
    createdAt = createdAt,
)
