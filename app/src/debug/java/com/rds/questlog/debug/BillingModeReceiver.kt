package com.rds.questlog.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.rds.questlog.data.billing.FakeBillingService
import com.rds.questlog.domain.usecase.premium.SyncPremiumStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Hanya build debug: memaksa hasil verifikasi `FakeBillingService` lalu langsung menyinkronkan status premium, untuk
 * menguji tiga keadaan guard fitur premium tanpa Play Store:
 *
 *     adb shell am broadcast -n com.rds.questlog.debug/com.rds.questlog.debug.BillingModeReceiver \
 *         -a com.rds.questlog.debug.SET_BILLING_MODE --es mode premium|free|unverified|clear
 *
 * premium = pembelian aktif; free = toko mengonfirmasi tidak ada pembelian (premium turun menjadi "hilang" bila
 * sebelumnya premium); unverified = toko tidak terjangkau (status terakhir dipertahankan); clear = ikuti pembelian palsu.
 */
@AndroidEntryPoint
class BillingModeReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: SharedPreferences

    @Inject lateinit var syncPremium: SyncPremiumStatusUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val mode = intent.getStringExtra(EXTRA_MODE)
        val edit = prefs.edit()
        if (mode in MODES) {
            edit.putString(
                FakeBillingService.KEY_MODE,
                mode,
            )
        } else {
            edit.remove(FakeBillingService.KEY_MODE)
        }
        edit.apply()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val result = syncPremium()
            Log.i(TAG, "mode=$mode sync=${if (result.isSuccess) "ok" else "gagal"}")
            pending.finish()
        }
    }

    private companion object {
        const val TAG = "BillingMode"
        const val EXTRA_MODE = "mode"
        val MODES = setOf(
            FakeBillingService.MODE_PREMIUM,
            FakeBillingService.MODE_FREE,
            FakeBillingService.MODE_UNVERIFIED,
        )
    }
}
