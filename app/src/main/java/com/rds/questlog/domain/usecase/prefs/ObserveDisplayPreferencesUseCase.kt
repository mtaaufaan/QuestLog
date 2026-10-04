package com.rds.questlog.domain.usecase.prefs

import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDisplayPreferencesUseCase @Inject constructor(private val repository: UserPreferencesRepository) {

    /** Stream preferensi tampilan app-wide (ukuran teks dan mode gelap). */
    operator fun invoke(): Flow<DisplayPreferences> = repository.observeDisplayPreferences()
}
