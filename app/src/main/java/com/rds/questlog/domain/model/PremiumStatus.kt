package com.rds.questlog.domain.model

sealed interface PremiumStatus {
    data object Free : PremiumStatus
    data object Unlimited : PremiumStatus
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failure(val reason: String) : PurchaseResult
}
