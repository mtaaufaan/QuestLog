package com.rds.questlog.data.billing

import com.rds.questlog.domain.model.PremiumStatus
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Build `release`. ponytail: stub, selalu Free; integrasi Google Play Billing asli di sprint monetisasi. */
class GooglePlayBillingService : BillingService {
    override fun observePremiumStatus(): Flow<PremiumStatus> = flowOf(PremiumStatus.Free)
    override suspend fun purchaseUnlimited() = PurchaseResult.Failure("Google Play Billing belum diintegrasikan")
    override suspend fun restorePurchases() = PurchaseResult.Failure("Google Play Billing belum diintegrasikan")
}
