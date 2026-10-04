package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.ImageDao
import com.rds.questlog.data.local.dao.SourcePageDao
import com.rds.questlog.data.local.entity.ArticleEntity
import com.rds.questlog.data.local.entity.SourcePageEntity
import com.rds.questlog.data.local.escapeLike
import com.rds.questlog.data.mapper.toDomain
import com.rds.questlog.data.scraper.ImageStore
import com.rds.questlog.domain.model.Article
import com.rds.questlog.domain.model.ScrapeLimits
import com.rds.questlog.domain.repository.ArticleRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArticleRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val articleDao: ArticleDao,
    private val pageDao: SourcePageDao,
    private val imageDao: ImageDao,
    private val imageStore: ImageStore,
) : ArticleRepository {

    override fun observeArticles(query: String, gameId: Long?): Flow<List<Article>> =
        articleDao.observeArticles(escapeLike(query), gameId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun insertArticle(gameId: Long, title: String, urls: List<String>): Long = db.withTransaction {
        val articleId = articleDao.insert(
            ArticleEntity(gameId = gameId, title = title, createdAt = System.currentTimeMillis()),
        )
        val slot = ScrapeLimits.MAX_NODES_PER_PAGE
        pageDao.insertAll(
            urls.mapIndexed { index, url ->
                SourcePageEntity(
                    articleId = articleId,
                    sourceUrl = url,
                    pageOrder = index + 1,
                    orderStart = index * slot + 1,
                    orderEnd = (index + 1) * slot,
                )
            },
        )
        articleId
    }

    override suspend fun deleteArticle(articleId: Long) {
        val filenames = imageDao.filenamesOf(articleId)
        db.withTransaction { articleDao.delete(articleId) }
        // File dibagi antar artikel lewat dedup MD5; hapus hanya bila sudah tidak ada yang merujuk.
        filenames.filter { imageDao.countByFilename(it) == 0 }.forEach(imageStore::delete)
    }

    override suspend fun retryFailedPages(articleId: Long): Int = db.withTransaction {
        val reset = pageDao.resetFailed(articleId)
        if (reset > 0) articleDao.setScrapingDone(articleId, false)
        reset
    }
}
