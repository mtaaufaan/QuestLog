package com.rds.questlog.domain.usecase.prefs

import com.rds.questlog.domain.repository.UserPreferencesRepository
import com.rds.questlog.domain.usecase.writing
import javax.inject.Inject

class SetDarkModeUseCase @Inject constructor(private val repository: UserPreferencesRepository) {

    /** Menyimpan mode gelap Reader (app-wide). */
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = writing { repository.setDarkMode(enabled) }
}
