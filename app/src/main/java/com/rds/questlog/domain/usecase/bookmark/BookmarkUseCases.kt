package com.rds.questlog.domain.usecase.bookmark

import com.rds.questlog.domain.error.QuestLogError.ValidationError
import com.rds.questlog.domain.model.Bookmark
import com.rds.questlog.domain.model.MAX_BOOKMARK_LABEL
import com.rds.questlog.domain.model.MAX_BOOKMARK_SNIPPET
import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.repository.BookmarkRepository
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Label dipangkas spasi; kosong atau > 60 karakter ditolak (SRS-FR-19). */
internal fun validBookmarkLabel(label: String): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) throw ValidationError.EmptyBookmarkLabel
    if (trimmed.length > MAX_BOOKMARK_LABEL) throw ValidationError.BookmarkLabelTooLong
    return trimmed
}

/** Daftar bookmark semua artikel di satu game; selalu terbaca (juga saat premium hilang). */
class ObserveGameBookmarksUseCase @Inject constructor(private val repository: BookmarkRepository) {
    /** Emit ulang tiap ada bookmark ditambah, diubah, atau dihapus. */
    operator fun invoke(gameId: Long): Flow<List<Bookmark>> = repository.observeByGame(gameId)
}

/**
 * Menambah bookmark di elemen konten [nodeId] (anchor ke id node, bukan posisi scroll, sehingga tetap tepat saat
 * ukuran font berubah). Banyak per artikel; tidak menyentuh bookmark lain maupun checkpoint. Guard dulu: gratis ->
 * `PremiumError.Locked`, premium hilang -> `PremiumError.ReadOnly`, data tidak berubah.
 */
class AddBookmarkUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BookmarkRepository,
) {
    /** Mengembalikan id bookmark baru, atau kegagalan (guard premium, label tidak valid, node bukan milik artikel). */
    suspend operator fun invoke(articleId: Long, nodeId: Long, label: String, snippet: String): Result<Long> = writing {
        guard.requireWrite(PremiumFeature.BOOKMARK)
        repository.add(articleId, nodeId, validBookmarkLabel(label), snippet.trim().take(MAX_BOOKMARK_SNIPPET))
    }
}

class UpdateBookmarkLabelUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BookmarkRepository,
) {
    /** Mengganti label bookmark [id]; label divalidasi seperti saat menambah. */
    suspend operator fun invoke(id: Long, label: String): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.BOOKMARK)
        repository.updateLabel(id, validBookmarkLabel(label))
    }
}

class DeleteBookmarkUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: BookmarkRepository,
) {
    /** Menghapus bookmark [id] setelah guard premium. */
    suspend operator fun invoke(id: Long): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.BOOKMARK)
        repository.delete(id)
    }
}
