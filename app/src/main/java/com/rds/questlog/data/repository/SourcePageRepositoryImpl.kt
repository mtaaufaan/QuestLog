package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.CheckpointDao
import com.rds.questlog.data.local.dao.ContentNodeDao
import com.rds.questlog.data.local.dao.ImageDao
import com.rds.questlog.data.local.dao.SourcePageDao
import com.rds.questlog.data.local.entity.ContentNodeEntity
import com.rds.questlog.data.local.entity.ImageEntity
import com.rds.questlog.data.mapper.toCode
import com.rds.questlog.data.mapper.toDomain
import com.rds.questlog.domain.model.PageFailure
import com.rds.questlog.domain.model.ScrapeLimits
import com.rds.questlog.domain.model.ScrapedImage
import com.rds.questlog.domain.model.ScrapedNode
import com.rds.questlog.domain.model.SourcePage
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.repository.SourcePageRepository
import javax.inject.Inject

class SourcePageRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val pageDao: SourcePageDao,
    private val nodeDao: ContentNodeDao,
    private val imageDao: ImageDao,
    private val articleDao: ArticleDao,
    private val checkpointDao: CheckpointDao,
) : SourcePageRepository {

    private val bookmarkDao get() = db.bookmarkDao()

    override suspend fun recoverInterrupted(articleId: Long) {
        pageDao.resetInterrupted(articleId)
    }

    override suspend fun getPendingPages(articleId: Long): List<SourcePage> =
        pageDao.getPending(articleId).map { it.toDomain() }

    override suspend fun markInProgress(pageId: Long) =
        pageDao.setStatus(pageId, SourcePageStatus.IN_PROGRESS.name, null)

    override suspend fun saveScrapedPage(
        articleId: Long,
        pageId: Long,
        nodes: List<ScrapedNode>,
        images: List<ScrapedImage>,
    ) = db.withTransaction {
        val page = pageDao.getById(pageId) ?: return@withTransaction
        val start = page.orderStart ?: ((page.pageOrder - 1) * ScrapeLimits.MAX_NODES_PER_PAGE + 1)
        val old = nodeDao.getByPage(pageId)
        nodeDao.insertAll(
            nodes.mapIndexed { index, node ->
                ContentNodeEntity(
                    articleId = articleId,
                    sourcePageId = pageId,
                    nodeType = node.type.dbValue,
                    displayOrder = start + index,
                    textContent = node.text,
                    metadataJson = node.metadataJson,
                )
            },
        )
        if (old.isNotEmpty()) replaceOldNodes(articleId, pageId, old)
        val now = System.currentTimeMillis()
        val known = imageDao.filenamesOf(articleId).toSet()
        imageDao.insertAll(
            images.filter { it.filename !in known }.map {
                ImageEntity(
                    articleId = articleId,
                    filename = it.filename,
                    filePath = it.filePath,
                    fileSize = it.fileSize,
                    sourceUrl = it.sourceUrl,
                    createdAt = now,
                )
            },
        )
        pageDao.setStatus(pageId, SourcePageStatus.COMPLETED.name, null)
    }

    /**
     * Unduh ulang: node baru sudah tersimpan di samping node lama. Checkpoint (anchor dan posisi baca) serta bookmark
     * yang menunjuk node lama diarahkan ke node baru yang paling cocok ([pickReplacement]) SEBELUM node lama dihapus,
     * karena FK tanpa cascade menolak menghapus node yang masih dirujuk. Tanpa node baru, checkpoint dilepas dan
     * bookmark dihapus.
     */
    private suspend fun replaceOldNodes(articleId: Long, pageId: Long, old: List<ContentNodeEntity>) {
        val maxOldId = old.maxOf { it.id }
        val fresh = nodeDao.getByPage(pageId).filter { it.id > maxOldId }
        val oldById = old.associateBy { it.id }
        val checkpoint = checkpointDao.get(articleId)
        checkpoint?.anchorNodeId?.let(oldById::get)?.let { anchor ->
            val next = pickReplacement(anchor, fresh)
            if (next != null) {
                checkpointDao.setAnchor(articleId, next.id, next.displayOrder)
            } else {
                checkpointDao.moveAnchor(articleId, pageId, null, null)
            }
        }
        checkpoint?.lastVisitedNodeId?.let(oldById::get)?.let { last ->
            val next = pickReplacement(last, fresh)
            if (next != null) {
                checkpointDao.setLastVisited(articleId, next.id)
            } else {
                checkpointDao.moveLastVisited(articleId, pageId, null)
            }
        }
        bookmarkDao.getByPage(pageId).forEach { bookmark ->
            val next = oldById[bookmark.nodeId]?.let { pickReplacement(it, fresh) }
            if (next != null) {
                bookmarkDao.repoint(
                    bookmark.id,
                    next.id,
                    next.displayOrder,
                )
            } else {
                bookmarkDao.delete(bookmark.id)
            }
        }
        nodeDao.deleteByPageUpTo(pageId, maxOldId)
    }

    override suspend fun markFailed(pageId: Long, failure: PageFailure) =
        pageDao.setStatus(pageId, SourcePageStatus.FAILED.name, failure.toCode())

    override suspend fun updateScrapingDone(articleId: Long) =
        articleDao.setScrapingDone(articleId, pageDao.countUnfinished(articleId) == 0)
}
