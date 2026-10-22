package com.rds.questlog.domain.usecase.prefs

import com.rds.questlog.domain.model.ReaderTheme
import com.rds.questlog.domain.repository.UserPreferencesRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SetReaderThemeUseCase @Inject constructor(private val repository: UserPreferencesRepository) {

    /** Menyimpan Mode Baca (app-wide, berlaku untuk semua artikel). */
    suspend operator fun invoke(theme: ReaderTheme): Result<Unit> = writing { repository.setReaderTheme(theme) }
}
