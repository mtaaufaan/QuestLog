package com.rds.questlog.domain.usecase.premium

import com.rds.questlog.domain.model.PremiumStatus
import com.rds.questlog.domain.repository.BillingService
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePremiumStatusUseCase @Inject constructor(private val billingService: BillingService) {

    /** Stream apakah pengguna Unlimited; implementasi `BillingService` ditentukan per build variant. */
    operator fun invoke(): Flow<Boolean> = billingService.observePremiumStatus().map { it == PremiumStatus.Unlimited }
}
