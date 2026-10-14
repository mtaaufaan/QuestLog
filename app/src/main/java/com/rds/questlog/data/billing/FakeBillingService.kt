package com.rds.questlog.data.billing

import android.content.SharedPreferences
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import kotlinx.coroutines.delay

/**
 * Build `debug`: "toko" palsu yang mengingat pembelian di SharedPreferences, sehingga seluruh jalur nyata (use case,
 * app_config, Flow premium) bisa dicoba tanpa Play Store. Pembelian palsu bertahan walau app_config dihapus, jadi
 * Restore Purchase juga bisa diuji. [KEY_MODE] memaksa tiga keadaan verifikasi (premium, gratis, tidak terverifikasi)
 * untuk menguji guard fitur premium; lihat `BillingModeReceiver` (src/debug).
 */
class FakeBillingService(private val prefs: SharedPreferences) : BillingService {

    override suspend fun queryUnlimitedPurchase(): PurchaseQuery = when (prefs.getString(KEY_MODE, null)) {
        MODE_PREMIUM -> PurchaseQuery.Owned(FAKE_TOKEN)
        MODE_FREE -> PurchaseQuery.NotOwned
        MODE_UNVERIFIED -> PurchaseQuery.Unavailable("simulasi: toko tidak dapat dihubungi")
        else -> if (prefs.getBoolean(
                KEY_FAKE_PREMIUM,
                false,
            )
        ) {
            PurchaseQuery.Owned(FAKE_TOKEN)
        } else {
            PurchaseQuery.NotOwned
        }
    }

    override suspend fun purchaseUnlimited(): PurchaseResult {
        delay(PURCHASE_DELAY_MS)
        prefs.edit().putBoolean(KEY_FAKE_PREMIUM, true).remove(KEY_MODE).apply()
        return PurchaseResult.Success(FAKE_TOKEN)
    }

    companion object {
        const val KEY_FAKE_PREMIUM = "fake_premium"

        /** Memaksa hasil verifikasi ([MODE_PREMIUM], [MODE_FREE], [MODE_UNVERIFIED]); kosong = ikut pembelian palsu. */
        const val KEY_MODE = "fake_billing_mode"
        const val MODE_PREMIUM = "premium"
        const val MODE_FREE = "free"
        const val MODE_UNVERIFIED = "unverified"

        private const val FAKE_TOKEN = "debug-fake-token"
        private const val PURCHASE_DELAY_MS = 1_000L
    }
}
