package com.rds.questlog.data.repository

import androidx.room.withTransaction
import com.rds.questlog.data.local.QuestLogDatabase
import com.rds.questlog.data.local.dao.AppConfigDao
import com.rds.questlog.data.local.entity.AppConfigEntity
import com.rds.questlog.domain.repository.AppConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppConfigRepositoryImpl @Inject constructor(
    private val db: QuestLogDatabase,
    private val dao: AppConfigDao,
) : AppConfigRepository {

    override fun observeIsPremium(): Flow<Boolean> = dao.observe(KEY_IS_PREMIUM).map { it == PREMIUM }

    override suspend fun setPremium(isPremium: Boolean, purchaseToken: String?, verifiedAt: Long) {
        db.withTransaction {
            dao.upsertAll(
                listOf(
                    AppConfigEntity(KEY_IS_PREMIUM, if (isPremium) PREMIUM else NOT_PREMIUM),
                    AppConfigEntity(KEY_VERIFIED_AT, verifiedAt.toString()),
                ),
            )
            if (purchaseToken != null) {
                dao.upsertAll(listOf(AppConfigEntity(KEY_PURCHASE_TOKEN, purchaseToken)))
            } else {
                dao.delete(KEY_PURCHASE_TOKEN)
            }
        }
    }

    private companion object {
        const val KEY_IS_PREMIUM = "is_premium"
        const val KEY_PURCHASE_TOKEN = "purchase_token"
        const val KEY_VERIFIED_AT = "purchase_verified_at"
        const val PREMIUM = "1"
        const val NOT_PREMIUM = "0"
    }
}
