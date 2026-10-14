package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.ArticleDao
import com.rds.questlog.data.local.dao.BinderDao
import com.rds.questlog.data.local.dao.GameProgressDao
import com.rds.questlog.data.local.entity.ArticleBinderEntity
import com.rds.questlog.data.local.entity.BinderCategoryEntity
import com.rds.questlog.data.local.entity.GameProgressEntity
import com.rds.questlog.domain.model.Binder
import com.rds.questlog.domain.model.BinderCategory
import com.rds.questlog.domain.repository.BinderRepository
import com.rds.questlog.domain.repository.GameProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GameProgressRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val progressDao: GameProgressDao,
    private val articleDao: ArticleDao,
) : GameProgressRepository {

    override suspend fun setMainArticle(articleId: Long) = db.withTransaction {
        val gameId = checkNotNull(articleDao.gameIdOf(articleId)) { "Artikel $articleId tidak ada" }
        progressDao.upsert(GameProgressEntity(gameId = gameId, articleId = articleId))
    }

    override suspend fun mainArticleId(gameId: Long): Long? = progressDao.articleIdOf(gameId)
}

class BinderRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val binderDao: BinderDao,
    private val articleDao: ArticleDao,
) : BinderRepository {

    private val memberDao get() = db.binderMemberDao()

    override fun observe(gameId: Long): Flow<Binder> = combine(
        binderDao.observeCategories(gameId),
        memberDao.observeAssignments(gameId),
    ) { categories, rows ->
        Binder(
            categories = categories.map { it.toDomain() },
            assignments = rows.groupBy({ it.articleId }, { it.categoryId }).mapValues { it.value.toSet() },
        )
    }

    override suspend fun findByName(gameId: Long, name: String): BinderCategory? =
        binderDao.findByName(gameId, name)?.toDomain()

    override suspend fun category(id: Long): BinderCategory? = binderDao.category(id)?.toDomain()

    override suspend fun createCategory(gameId: Long, name: String): Long = db.withTransaction {
        binderDao.insert(
            BinderCategoryEntity(
                gameId = gameId,
                name = name,
                position = binderDao.maxPosition(gameId) + 1,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun renameCategory(id: Long, name: String) = binderDao.rename(id, name)

    override suspend fun deleteCategory(id: Long) = binderDao.delete(id)

    override suspend fun setMembership(articleId: Long, categoryId: Long, member: Boolean) = db.withTransaction {
        val gameId = checkNotNull(articleDao.gameIdOf(articleId)) { "Artikel $articleId tidak ada" }
        val category = checkNotNull(binderDao.category(categoryId)) { "Kategori $categoryId tidak ada" }
        check(category.gameId == gameId) { "Kategori bukan milik game artikel" }
        if (member) {
            memberDao.addMember(ArticleBinderEntity(articleId, categoryId))
        } else {
            memberDao.removeMember(articleId, categoryId)
        }
    }

    override suspend fun assign(articleId: Long, categoryIds: Set<Long>, newNames: List<String>) = db.withTransaction {
        val gameId = checkNotNull(articleDao.gameIdOf(articleId)) { "Artikel $articleId tidak ada" }
        memberDao.clearMembers(articleId, gameId)
        categoryIds.mapNotNull { binderDao.category(it) }
            .filter { it.gameId == gameId }
            .forEach { memberDao.addMember(ArticleBinderEntity(articleId, it.id)) }
        newNames.forEach { name ->
            val id = binderDao.findByName(gameId, name)?.id ?: createCategory(gameId, name)
            memberDao.addMember(ArticleBinderEntity(articleId, id))
        }
    }
}

private fun BinderCategoryEntity.toDomain() = BinderCategory(id = id, gameId = gameId, name = name, position = position)
