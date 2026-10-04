package com.rds.questlog.domain.usecase.premium

import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.repository.AppConfigRepository
import com.rds.questlog.domain.repository.BillingService
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SyncPremiumStatusUseCase @Inject constructor(
    private val billing: BillingService,
    private val appConfig: AppConfigRepository,
) {

    /**
     * Dipanggil setiap aplikasi dibuka: memeriksa pembelian di toko (queryPurchasesAsync) lalu menyimpan hasilnya di
     * app_config. Ada pembelian -> premium + token + waktu verifikasi; tidak ada -> bukan premium. Bila toko tidak
     * bisa dihubungi (offline) cache lama dibiarkan agar pembeli tidak turun ke tier gratis hanya karena tanpa koneksi.
     * Gagal menulis -> Result.failure(DatabaseError.WriteFailed).
     */
    suspend operator fun invoke(): Result<Unit> = writing {
        when (val query = billing.queryUnlimitedPurchase()) {
            is PurchaseQuery.Owned -> appConfig.setPremium(true, query.purchaseToken, System.currentTimeMillis())
            PurchaseQuery.NotOwned -> appConfig.setPremium(false, null, System.currentTimeMillis())
            is PurchaseQuery.Unavailable -> Unit
        }
    }
}
