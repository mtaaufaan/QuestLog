package com.rds.questlog.domain.usecase.note

import com.rds.questlog.domain.model.PremiumFeature
import com.rds.questlog.domain.repository.NoteRepository
import com.rds.questlog.domain.usecase.premium.PremiumGuard
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Isi catatan artikel; selalu terbaca (juga saat premium hilang). */
class ObserveNoteUseCase @Inject constructor(private val repository: NoteRepository) {
    /** Emit ulang tiap catatan disimpan atau dihapus. */
    operator fun invoke(articleId: Long): Flow<String> = repository.observe(articleId)
}

/**
 * Menyimpan catatan umum artikel (autosave dipanggil berulang). Catatan kosong menghapus barisnya. Tidak terikat
 * posisi konten, jadi memindahkan artikel ke game lain tidak mengubahnya. Lewat [PremiumGuard] seperti bookmark.
 */
class SaveNoteUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: NoteRepository,
) {
    /** Menyimpan [content] sebagai catatan [articleId] setelah guard premium. */
    suspend operator fun invoke(articleId: Long, content: String): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.NOTE)
        if (content.isEmpty()) repository.delete(articleId) else repository.save(articleId, content)
    }
}

class DeleteNoteUseCase @Inject constructor(
    private val guard: PremiumGuard,
    private val repository: NoteRepository,
) {
    /** Menghapus catatan [articleId] setelah guard premium. */
    suspend operator fun invoke(articleId: Long): Result<Unit> = writing {
        guard.requireWrite(PremiumFeature.NOTE)
        repository.delete(articleId)
    }
}
