package com.rds.questlog.domain.model

/** Hasil memeriksa pembelian "QuestLog Unlimited" di toko (BillingClient.queryPurchasesAsync). */
sealed interface PurchaseQuery {
    /** Ada pembelian aktif; [purchaseToken] disimpan sebagai cache verifikasi. */
    data class Owned(val purchaseToken: String) : PurchaseQuery

    /** Pemeriksaan berhasil dan tidak ada pembelian aktif. */
    data object NotOwned : PurchaseQuery

    /** Toko tidak bisa dihubungi (offline, layanan tidak tersedia); status tersimpan tidak boleh diubah. */
    data class Unavailable(val reason: String) : PurchaseQuery
}

/** Hasil alur pembelian one-time "QuestLog Unlimited". */
sealed interface PurchaseResult {
    data class Success(val purchaseToken: String) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failure(val reason: String) : PurchaseResult
}

/** Hasil aksi Unlock / Restore Purchase yang ditampilkan di Unlock Sheet. */
enum class UnlockOutcome {
    /** Pembelian aktif ditemukan atau berhasil dibeli; premium sudah tersimpan. */
    ACTIVATED,

    /** Restore: tidak ada pembelian di akun ini. */
    NOT_FOUND,

    /** Pembeli membatalkan alur pembelian. */
    CANCELLED,

    /** Toko tidak tersedia atau pembelian gagal/tertunda. */
    UNAVAILABLE,
}
