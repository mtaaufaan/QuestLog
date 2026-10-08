package com.rds.questlog.domain.error

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

    /** Batas tier gratis (QL-12): maks. 2 game dan 5 artikel per game. */
    sealed class TierError : QuestLogError() {
        data object GameLimitReached : TierError()
        data object ArticleLimitReached : TierError()
    }
}
