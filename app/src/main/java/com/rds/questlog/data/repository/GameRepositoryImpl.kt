package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.GameDao
import com.rds.questlog.data.local.dao.ImageDao
import com.rds.questlog.data.local.entity.GameEntity
import com.rds.questlog.data.mapper.toDomain
import com.rds.questlog.data.scraper.ImageStore
import com.rds.questlog.domain.model.Game
import com.rds.questlog.domain.repository.GameRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val gameDao: GameDao,
    private val imageDao: ImageDao,
    private val imageStore: ImageStore,
) : GameRepository {

    override fun observeGames(): Flow<List<Game>> =
        gameDao.observeWithArticleCount().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findIdByName(name: String): Long? = gameDao.findByTitle(name)?.id

    override suspend fun count(): Int = gameDao.count()

    override suspend fun findOrCreate(name: String): Long = gameDao.findByTitle(name)?.id
        ?: gameDao.insert(GameEntity(title = name, createdAt = System.currentTimeMillis()))

    override suspend fun deleteIfEmpty(gameId: Long) {
        if (gameDao.deleteIfEmpty(gameId) > 0) imageStore.deleteLogos(gameId)
    }

    override suspend fun deleteGame(gameId: Long) {
        val filenames = imageDao.filenamesOfGame(gameId)
        db.withTransaction { gameDao.delete(gameId) }
        // File dibagi antar artikel lewat dedup MD5; hapus hanya bila sudah tidak ada yang merujuk.
        filenames.filter { imageDao.countByFilename(it) == 0 }.forEach(imageStore::delete)
        imageStore.deleteLogos(gameId)
    }
}
