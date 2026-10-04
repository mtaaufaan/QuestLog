package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult

/** Pintu ke toko aplikasi; implementasi (BillingClient) ditentukan per build variant (tech-stack.md §11). */
interface BillingService {

    /**
     * Memeriksa pembelian "QuestLog Unlimited" yang aktif (queryPurchasesAsync). Tidak pernah melempar: kegagalan
     * menghubungi toko dikembalikan sebagai [PurchaseQuery.Unavailable].
     */
    suspend fun queryUnlimitedPurchase(): PurchaseQuery

    /** Menjalankan alur pembelian sekali-bayar "QuestLog Unlimited"; tidak pernah melempar. */
    suspend fun purchaseUnlimited(): PurchaseResult
}
