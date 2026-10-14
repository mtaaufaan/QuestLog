package com.rds.questlog.domain.usecase.prefs

import com.rds.questlog.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class ConsumeReaderTipUseCase @Inject constructor(private val repository: UserPreferencesRepository) {

    /** true hanya sekali per instalasi: saat tip "tekan-tahan paragraf" belum pernah ditampilkan. */
    suspend operator fun invoke(): Boolean = runCatching { repository.consumeReaderTip() }.getOrDefault(false)
}
