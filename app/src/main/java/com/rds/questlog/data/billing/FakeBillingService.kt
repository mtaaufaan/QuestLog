package com.rds.questlog.data.billing

import android.content.SharedPreferences
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import kotlinx.coroutines.delay

/**
 * Build `debug`: "toko" palsu yang mengingat pembelian di SharedPreferences, sehingga seluruh jalur nyata (use case,
 * app_config, Flow premium) bisa dicoba tanpa Play Store. Pembelian palsu bertahan walau app_config dihapus, jadi
 * Restore Purchase juga bisa diuji.
 */
class FakeBillingService(private val prefs: SharedPreferences) : BillingService {

    override suspend fun queryUnlimitedPurchase(): PurchaseQuery =
        if (prefs.getBoolean(KEY_FAKE_PREMIUM, false)) PurchaseQuery.Owned(FAKE_TOKEN) else PurchaseQuery.NotOwned

    override suspend fun purchaseUnlimited(): PurchaseResult {
        delay(PURCHASE_DELAY_MS)
        prefs.edit().putBoolean(KEY_FAKE_PREMIUM, true).apply()
        return PurchaseResult.Success(FAKE_TOKEN)
    }

    private companion object {
        const val KEY_FAKE_PREMIUM = "fake_premium"
        const val FAKE_TOKEN = "debug-fake-token"
        const val PURCHASE_DELAY_MS = 1_000L
    }
}
