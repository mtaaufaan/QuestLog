package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.DisplayPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    /** Stream preferensi tampilan app-wide; nilai bawaan dipakai selama belum pernah diubah. */
    fun observeDisplayPreferences(): Flow<DisplayPreferences>

    /** Menyimpan ukuran teks (sp); pemanggil sudah membatasi rentangnya. */
    suspend fun setFontSize(sizeSp: Int)

    /** Menyimpan mode gelap Reader. */
    suspend fun setDarkMode(enabled: Boolean)
}
