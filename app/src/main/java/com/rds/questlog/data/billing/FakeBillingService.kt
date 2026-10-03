package com.rds.questlog.data.billing

import android.content.SharedPreferences
import com.rds.questlog.domain.model.PremiumStatus
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Build `debug`: toggle premium manual via SharedPreferences. */
class FakeBillingService(private val prefs: SharedPreferences) : BillingService {
    override fun observePremiumStatus(): Flow<PremiumStatus> = flow {
        emit(if (prefs.getBoolean(KEY_FAKE_PREMIUM, false)) PremiumStatus.Unlimited else PremiumStatus.Free)
    }

    override suspend fun purchaseUnlimited(): PurchaseResult {
        prefs.edit().putBoolean(KEY_FAKE_PREMIUM, true).apply()
        return PurchaseResult.Success
    }

    override suspend fun restorePurchases() = purchaseUnlimited()

    private companion object {
        const val KEY_FAKE_PREMIUM = "fake_premium"
    }
}
