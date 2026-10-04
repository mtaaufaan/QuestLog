package com.rds.questlog.domain.usecase.premium

import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.model.UnlockOutcome
import com.rds.questlog.domain.repository.AppConfigRepository
import com.rds.questlog.domain.repository.BillingService
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class PurchaseUnlimitedUseCase @Inject constructor(
    private val billing: BillingService,
    private val appConfig: AppConfigRepository,
) {

    /**
     * Menjalankan alur pembelian "QuestLog Unlimited" (one-time). Berhasil -> premium disimpan di app_config sehingga
     * batas tier gratis langsung hilang lewat Flow. Dibatalkan/gagal -> status tidak berubah.
     * Gagal menulis -> Result.failure(DatabaseError.WriteFailed).
     */
    suspend operator fun invoke(): Result<UnlockOutcome> = writing {
        when (val result = billing.purchaseUnlimited()) {
            is PurchaseResult.Success -> {
                appConfig.setPremium(true, result.purchaseToken, System.currentTimeMillis())
                UnlockOutcome.ACTIVATED
            }
            PurchaseResult.Cancelled -> UnlockOutcome.CANCELLED
            is PurchaseResult.Failure -> UnlockOutcome.UNAVAILABLE
        }
    }
}
