package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.domain.model.GameTarget
import com.rds.questlog.domain.repository.ArticleManagementRepository
import javax.inject.Inject

class ArticleManagementRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val articleDao: ArticleDao,
    private val gameDao: GameDao,
) : ArticleManagementRepository {

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
}
