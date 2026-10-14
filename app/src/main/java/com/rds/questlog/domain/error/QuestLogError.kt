package com.rds.questlog.domain.error

import com.rds.questlog.domain.model.PremiumFeature

/**
 * Error domain (error_state_model.md §2). Hanya cabang yang dipakai saat ini; NetworkError/ParseError dicakup
 * `PageFailure` per halaman, BillingError menyusul bersama fiturnya.
 */
sealed class QuestLogError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    sealed class DatabaseError(cause: Throwable?) : QuestLogError(cause = cause) {
        class ReadFailed(cause: Throwable? = null) : DatabaseError(cause)
        class WriteFailed(cause: Throwable? = null) : DatabaseError(cause)
    }

    sealed class ValidationError : QuestLogError() {
        /** Bukan format https://. */
        data object InvalidUrl : ValidationError()
        data object EmptyGameName : ValidationError()
        data object GameNameTooLong : ValidationError()
        data object EmptyTitle : ValidationError()
        data object TooManyUrls : ValidationError()
        data object NoUrlsProvided : ValidationError()

        /** Label bookmark kosong (wajib diisi). */
        data object EmptyBookmarkLabel : ValidationError()

        /** Label bookmark lebih dari 60 karakter. */
        data object BookmarkLabelTooLong : ValidationError()

        /** Nama kategori Binder kosong (wajib diisi). */
        data object EmptyCategoryName : ValidationError()

        /** Nama kategori Binder lebih dari 100 karakter. */
        data object CategoryNameTooLong : ValidationError()

        /** Kategori [name] sudah ada di game ini (tanpa peduli huruf besar-kecil). */
        data class DuplicateCategory(val name: String) : ValidationError()

        /** Kategori tidak ditemukan (mis. sudah terhapus). */
        data object CategoryNotFound : ValidationError()

        /** [url] muncul lebih dari sekali dalam input yang sama. */
        data class DuplicateUrl(val url: String) : ValidationError()

        /** [url] sudah tersimpan di salah satu artikel. */
        data class UrlAlreadySaved(val url: String) : ValidationError()
    }

    /** Pengelolaan halaman artikel (QL-19/QL-20). */
    sealed class ArticleError : QuestLogError() {
        /** Halaman sedang diunduh (PENDING/IN_PROGRESS); tidak boleh dihapus. */
        data object Busy : ArticleError()

        /** Halaman terakhir artikel tidak boleh dihapus; hapus artikelnya. */
        data object LastPage : ArticleError()

        /** Halaman (atau daftar halaman) tidak cocok dengan artikel; mis. sudah terhapus lebih dulu. */
        data object PageNotFound : ArticleError()
    }

    /** Deteksi halaman lanjutan gagal mengambil halaman (QL-34); tidak menghalangi penyimpanan manual. */
    data class DetectionFailed(val failure: com.rds.questlog.domain.model.PageFailure) : QuestLogError()

    /** Penguncian fitur premium (QL-26). */
    sealed class PremiumError : QuestLogError() {
        /** Pengguna gratis: fitur terkunci, tampilkan ajakan upgrade (Unlock Sheet) tanpa mengubah data. */
        data class Locked(val feature: PremiumFeature) : PremiumError()

        /** Premium hilang: data lama tetap terbaca, tetapi membuat/mengubah/menghapus dikunci (hanya-baca). */
        data class ReadOnly(val feature: PremiumFeature) : PremiumError()
    }

    /** Batas tier gratis (QL-12): maks. 2 game dan 5 artikel per game. */
    sealed class TierError : QuestLogError() {
        data object GameLimitReached : TierError()
        data object ArticleLimitReached : TierError()
    }
}
