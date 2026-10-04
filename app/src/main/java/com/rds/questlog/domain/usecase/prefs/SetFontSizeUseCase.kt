package com.rds.questlog.domain.usecase.prefs

import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.repository.UserPreferencesRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SetFontSizeUseCase @Inject constructor(private val repository: UserPreferencesRepository) {

    /** Menyimpan ukuran teks app-wide; nilai dibatasi 12-24sp dan dibulatkan ke kelipatan 2 terdekat ke bawah. */
    suspend operator fun invoke(sizeSp: Int): Result<Unit> = writing {
        val min = DisplayPreferences.MIN_FONT_SIZE_SP
        val step = DisplayPreferences.FONT_SIZE_STEP_SP
        val clamped = sizeSp.coerceIn(min, DisplayPreferences.MAX_FONT_SIZE_SP)
        repository.setFontSize(min + (clamped - min) / step * step)
    }
}
