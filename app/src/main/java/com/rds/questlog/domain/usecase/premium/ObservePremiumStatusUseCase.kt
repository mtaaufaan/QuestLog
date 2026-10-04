package com.rds.questlog.domain.usecase.premium

import com.rds.questlog.domain.repository.AppConfigRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObservePremiumStatusUseCase @Inject constructor(private val appConfig: AppConfigRepository) {

    /**
     * Stream apakah pengguna Unlimited, dibaca dari is_premium di app_config (QL-17). Berupa Flow, bukan sekali baca,
     * sehingga batas tier gratis langsung hilang setelah pembelian tanpa restart aplikasi.
     */
    operator fun invoke(): Flow<Boolean> = appConfig.observeIsPremium()
}
