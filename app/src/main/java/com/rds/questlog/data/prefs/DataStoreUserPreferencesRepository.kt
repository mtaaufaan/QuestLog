package com.rds.questlog.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rds.questlog.domain.model.DisplayPreferences
import com.rds.questlog.domain.model.ReaderTheme
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
                theme = themeOf(prefs),
            )
        }

    override suspend fun setFontSize(sizeSp: Int) {
        dataStore.edit { it[FONT_SIZE] = sizeSp }
    }

    override suspend fun setReaderTheme(theme: ReaderTheme) {
        dataStore.edit { it[READER_THEME] = theme.name }
    }

    /** `reader_theme`; bila belum pernah dipilih, nilai lama `dark_mode = true` menjadi DARK, selain itu LIGHT. */
    private fun themeOf(prefs: Preferences): ReaderTheme =
        prefs[READER_THEME]?.let { name -> ReaderTheme.entries.firstOrNull { it.name == name } }
            ?: if (prefs[DARK_MODE] == true) ReaderTheme.DARK else ReaderTheme.LIGHT

    override suspend fun consumeReaderTip(): Boolean {
        var first = false
        dataStore.edit {
            first = it[READER_TIP_SEEN] != true
            it[READER_TIP_SEEN] = true
        }
        return first
    }

    private companion object {
        val READER_TIP_SEEN = booleanPreferencesKey("reader_tip_seen")
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val READER_THEME = stringPreferencesKey("reader_theme")
    }
}
