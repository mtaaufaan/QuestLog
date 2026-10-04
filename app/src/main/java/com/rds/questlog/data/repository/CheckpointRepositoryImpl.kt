package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.CheckpointDao
import com.rds.questlog.data.local.dao.ContentNodeDao
import com.rds.questlog.data.local.entity.CheckpointEntity
import com.rds.questlog.domain.repository.CheckpointRepository
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Penulisan memakai [NonCancellable]: Reader menyimpan posisi tepat saat layarnya ditutup, ketika ViewModel
 * bisa segera dibersihkan; tanpa ini penulisan yang sedang berjalan bisa ikut dibatalkan.
 */
class CheckpointRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val checkpointDao: CheckpointDao,
    private val nodeDao: ContentNodeDao,
) : CheckpointRepository {

    override suspend fun setCheckpoint(articleId: Long, nodeId: Long) = withContext(NonCancellable) {
        db.withTransaction {
            val order = requireNotNull(nodeDao.displayOrderOf(articleId, nodeId)) { "Node $nodeId bukan milik artikel" }
            val now = System.currentTimeMillis()
            val current = checkpointDao.get(articleId)
            checkpointDao.upsert(
                (current ?: CheckpointEntity(articleId = articleId, updatedAt = now))
                    .copy(anchorNodeId = nodeId, fallbackOrder = order, updatedAt = now),
            )
        }
    }

    override suspend fun saveLastPosition(articleId: Long, nodeId: Long) = withContext(NonCancellable) {
        db.withTransaction {
            requireNotNull(nodeDao.displayOrderOf(articleId, nodeId)) { "Node $nodeId bukan milik artikel" }
            val now = System.currentTimeMillis()
            val current = checkpointDao.get(articleId)
            checkpointDao.upsert(
                (current ?: CheckpointEntity(articleId = articleId, updatedAt = now))
                    .copy(lastVisitedNodeId = nodeId, lastReadAt = now, updatedAt = now),
            )
        }
    }
}
