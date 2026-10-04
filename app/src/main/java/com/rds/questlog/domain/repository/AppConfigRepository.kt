package com.rds.questlog.domain.repository

import kotlinx.coroutines.flow.Flow

/** Konfigurasi aplikasi lokal (tabel app_config): cache status premium hasil verifikasi toko. */
interface AppConfigRepository {

    /** Stream status premium (is_premium); emit ulang tiap berubah sehingga batas tier ikut berubah tanpa restart. */
    fun observeIsPremium(): Flow<Boolean>

    /**
     * Menyimpan hasil verifikasi dalam satu transaksi: is_premium, purchase_token ([purchaseToken] null menghapusnya),
     * dan purchase_verified_at ([verifiedAt], epoch millis).
     */
    suspend fun setPremium(isPremium: Boolean, purchaseToken: String?, verifiedAt: Long)
}
