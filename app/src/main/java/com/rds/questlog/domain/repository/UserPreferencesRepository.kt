package com.rds.questlog.domain.repository

import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.model.ReaderTheme
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    /** Stream preferensi tampilan app-wide; nilai bawaan dipakai selama belum pernah diubah. */
    fun observeDisplayPreferences(): Flow<DisplayPreferences>

    /** Menyimpan ukuran teks (sp); pemanggil sudah membatasi rentangnya. */
    suspend fun setFontSize(sizeSp: Int)

    /** Menyimpan Mode Baca (Light/Dark/Sepia/Muted). */
    suspend fun setReaderTheme(theme: ReaderTheme)

    /** true tepat sekali per instalasi (tip bookmark Reader); sesudahnya false. */
    suspend fun consumeReaderTip(): Boolean
}
