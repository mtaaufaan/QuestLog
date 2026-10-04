package com.rds.questlog.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.repository.UserPreferencesRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Preferensi tampilan app-wide di DataStore (bukan Room); file rusak/tak terbaca diperlakukan sebagai nilai bawaan. */
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override fun observeDisplayPreferences(): Flow<DisplayPreferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            DisplayPreferences(
                fontSizeSp = prefs[FONT_SIZE] ?: DisplayPreferences.DEFAULT_FONT_SIZE_SP,
                darkMode = prefs[DARK_MODE] ?: false,
            )
        }

    override suspend fun setFontSize(sizeSp: Int) {
        dataStore.edit { it[FONT_SIZE] = sizeSp }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    private companion object {
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}
