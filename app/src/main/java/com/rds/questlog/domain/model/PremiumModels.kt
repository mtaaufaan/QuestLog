package com.rds.questlog.domain.model

/**
 * Keadaan premium yang dipakai guard fitur (SRS-FR-16): [PREMIUM] = is_premium; [LOST] = bukan premium sekarang tetapi
 * pernah premium (Billing mengonfirmasi tidak ada pembelian aktif, mis. refund); [FREE] = belum pernah premium.
 * "Tidak terverifikasi" (offline) bukan keadaan sendiri: status terakhir dipertahankan.
 */
enum class PremiumState { FREE, PREMIUM, LOST }

/** Fitur premium v1 (SRS-FR-16); [hasUserData] = menyimpan data pengguna yang tetap terbaca bila premium hilang. */
enum class PremiumFeature(val hasUserData: Boolean) {
    BOOKMARK(hasUserData = true),
    NOTE(hasUserData = true),
    GAME_BINDER(hasUserData = true),
    AUTO_DETECT(hasUserData = false),
}

/** Akses ke fitur premium: lengkap, hanya-baca (data lama terbaca, ubah dikunci), atau terkunci (ajakan upgrade). */
enum class FeatureAccess { FULL, READ_ONLY, LOCKED }

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
