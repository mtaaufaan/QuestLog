package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
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
) : SourcePageRepository {

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
        nodeDao.deleteByPage(pageId)
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
        val now = System.currentTimeMillis()
        imageDao.insertAll(
            images.map {
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

    override suspend fun markFailed(pageId: Long, failure: PageFailure) =
        pageDao.setStatus(pageId, SourcePageStatus.FAILED.name, failure.toCode())

    override suspend fun updateScrapingDone(articleId: Long) =
        articleDao.setScrapingDone(articleId, pageDao.countUnfinished(articleId) == 0)
}
