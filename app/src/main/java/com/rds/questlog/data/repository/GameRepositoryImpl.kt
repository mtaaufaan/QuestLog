package com.rds.questlog.data.repository

import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.mapper.toDomain
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.repository.GameRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepositoryImpl @Inject constructor(private val gameDao: GameDao) : GameRepository {

    override fun observeGames(): Flow<List<Game>> =
        gameDao.observeWithArticleCount().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findIdByName(name: String): Long? = gameDao.findByTitle(name)?.id

    override suspend fun count(): Int = gameDao.count()

    override suspend fun findOrCreate(name: String): Long = gameDao.findByTitle(name)?.id
        ?: gameDao.insert(GameEntity(title = name, createdAt = System.currentTimeMillis()))
}
