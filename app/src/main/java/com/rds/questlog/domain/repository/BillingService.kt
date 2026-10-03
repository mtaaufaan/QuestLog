package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.PremiumStatus
import com.rds.questlog.domain.model.PurchaseResult
import kotlinx.coroutines.flow.Flow

interface BillingService {
    /** Status premium saat ini; implementasi ditentukan per build variant (tech-stack.md §11). */
    fun observePremiumStatus(): Flow<PremiumStatus>

    /** Memulai pembelian sekali-bayar "QuestLog Unlimited". */
    suspend fun purchaseUnlimited(): PurchaseResult

    /** Memulihkan pembelian sebelumnya (Restore Purchase). */
    suspend fun restorePurchases(): PurchaseResult
}
