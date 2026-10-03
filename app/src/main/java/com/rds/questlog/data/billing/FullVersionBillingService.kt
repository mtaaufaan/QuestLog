package com.rds.questlog.data.billing

import com.rds.questlog.domain.model.PremiumStatus
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Build `full` (APK sideload): selalu Unlimited, tanpa Play Store. */
class FullVersionBillingService : BillingService {
    override fun observePremiumStatus(): Flow<PremiumStatus> = flowOf(PremiumStatus.Unlimited)
    override suspend fun purchaseUnlimited() = PurchaseResult.Success
    override suspend fun restorePurchases() = PurchaseResult.Success
}
