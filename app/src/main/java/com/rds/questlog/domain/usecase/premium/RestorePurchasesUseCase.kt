package com.rds.questlog.domain.usecase.premium

import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.UnlockOutcome
import com.rds.questlog.domain.repository.AppConfigRepository
import com.rds.questlog.domain.repository.BillingService
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class RestorePurchasesUseCase @Inject constructor(
    private val billing: BillingService,
    private val appConfig: AppConfigRepository,
) {

    /**
     * Restore Purchase: memanggil queryPurchasesAsync ulang. Ditemukan -> premium disimpan (ACTIVATED); tidak ada ->
     * NOT_FOUND tanpa mengubah status tersimpan; toko tak terjangkau -> UNAVAILABLE.
     * Gagal menulis -> Result.failure(DatabaseError.WriteFailed).
     */
    suspend operator fun invoke(): Result<UnlockOutcome> = writing {
        when (val query = billing.queryUnlimitedPurchase()) {
            is PurchaseQuery.Owned -> {
                appConfig.setPremium(true, query.purchaseToken, System.currentTimeMillis())
                UnlockOutcome.ACTIVATED
            }
            PurchaseQuery.NotOwned -> UnlockOutcome.NOT_FOUND
            is PurchaseQuery.Unavailable -> UnlockOutcome.UNAVAILABLE
        }
    }
}
