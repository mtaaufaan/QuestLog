package com.rds.questlog.data.billing

import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService

/** Build `full` (APK sideload): selalu memiliki Unlimited, tanpa Play Store. */
class FullVersionBillingService : BillingService {
    override suspend fun queryUnlimitedPurchase(): PurchaseQuery = PurchaseQuery.Owned(FULL_TOKEN)
    override suspend fun purchaseUnlimited(): PurchaseResult = PurchaseResult.Success(FULL_TOKEN)

    private companion object {
        const val FULL_TOKEN = "full-version"
    }
}
