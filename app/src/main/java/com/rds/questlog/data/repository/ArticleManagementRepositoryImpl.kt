package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.local.entity.SourcePageEntity
import com.rds.questlog.domain.error.QuestLogError.ArticleError
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.model.ScrapeLimits
import com.rds.questlog.domain.model.SourcePageStatus
import com.rds.questlog.domain.repository.ArticleManagementRepository
import javax.inject.Inject

class ArticleManagementRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val articleDao: ArticleDao,
    private val gameDao: GameDao,
) : ArticleManagementRepository {

    private val pageDao get() = db.pageLayoutDao()
    private val nodeDao get() = db.contentNodeDao()
    private val checkpointDao get() = db.checkpointDao()

    override suspend fun updateDetails(articleId: Long, title: String, target: GameTarget) = db.withTransaction {
        val origin = checkNotNull(articleDao.gameIdOf(articleId)) { "Artikel $articleId tidak ada" }
        articleDao.setTitle(articleId, title)
        val targetId = when (target) {
            GameTarget.Unchanged -> origin
            is GameTarget.Existing -> target.gameId
            is GameTarget.New -> gameDao.findByTitle(target.name)?.id
                ?: gameDao.insert(GameEntity(title = target.name, createdAt = System.currentTimeMillis()))
        }
        if (targetId != origin) {
            articleDao.setGame(articleId, targetId)
            gameDao.deleteIfEmpty(origin)
        }
    }

    override suspend fun deletePage(articleId: Long, pageId: Long) = db.withTransaction {
        val pages = pageDao.getByArticle(articleId)
        val page = pages.firstOrNull { it.id == pageId } ?: throw ArticleError.PageNotFound
        if (pages.size == 1) throw ArticleError.LastPage
        if (page.status == SourcePageStatus.PENDING.name || page.status == SourcePageStatus.IN_PROGRESS.name) {
            throw ArticleError.Busy
        }
        val start = page.slotStart()
        // Pindahkan checkpoint DULU: FK tanpa cascade menolak menghapus node yang masih dirujuk.
        val nearest = nodeDao.lastBefore(articleId, pageId, start) ?: nodeDao.firstAfter(articleId, pageId, start)
        checkpointDao.moveAnchor(articleId, pageId, nearest?.id, nearest?.displayOrder)
        checkpointDao.moveLastVisited(articleId, pageId, nearest?.id)
        nodeDao.deleteByPage(pageId)
        pageDao.delete(pageId)
        renumber(articleId, pages.filter { it.id != pageId })
    }

    override suspend fun reorderPages(articleId: Long, orderedPageIds: List<Long>) = db.withTransaction {
        val pages = pageDao.getByArticle(articleId)
        val byId = pages.associateBy { it.id }
        if (orderedPageIds.size != pages.size || orderedPageIds.toSet() != byId.keys) throw ArticleError.PageNotFound
        renumber(articleId, orderedPageIds.map { byId.getValue(it) })
    }

    override suspend fun markPagesForRefresh(articleId: Long, pageId: Long?) = db.withTransaction {
        if (pageId == null) {
            pageDao.markAllPending(articleId)
        } else {
            if (pageDao.getByArticle(articleId).none { it.id == pageId }) throw ArticleError.PageNotFound
            pageDao.markPending(pageId)
        }
        articleDao.setScrapingDone(articleId, false)
    }

    /** Memberi nomor ulang halaman [ordered] (urutan 1..n, slot kelipatan 1000), menggeser node-nya, lalu fallback. */
    private suspend fun renumber(articleId: Long, ordered: List<SourcePageEntity>) {
        val slot = ScrapeLimits.MAX_NODES_PER_PAGE
        ordered.forEachIndexed { index, page ->
            val start = index * slot + 1
            val delta = start - page.slotStart()
            if (delta != 0) nodeDao.shiftOrder(page.id, delta)
            pageDao.setPosition(page.id, index + 1, start, (index + 1) * slot)
        }
        checkpointDao.refreshFallback(articleId)
    }

    /** Awal slot; artikel lama tanpa slot tercatat memakai slot baku (nomor halaman × slot). */
    private fun SourcePageEntity.slotStart(): Int =
        orderStart ?: ((pageOrder - 1) * ScrapeLimits.MAX_NODES_PER_PAGE + 1)
}
