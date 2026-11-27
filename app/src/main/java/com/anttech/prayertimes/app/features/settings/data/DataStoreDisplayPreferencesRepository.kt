package com.anttech.prayertimes.features.settings.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anttech.prayertimes.features.settings.domain.DisplayPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.displayDataStore by preferencesDataStore(name = "display_preferences")

class DataStoreDisplayPreferencesRepository(
    private val context: Context
) : DisplayPreferencesRepository {

    override val fontSizeMultiplier: Flow<Float> = context.displayDataStore.data.map { preferences ->
        val percent = preferences[FONT_SIZE_KEY]
            ?: (DisplayPreferencesRepository.DEFAULT_FONT_SCALE * 100).toInt()
        (percent / 100f).coerceIn(
            DisplayPreferencesRepository.MIN_FONT_SCALE,
            DisplayPreferencesRepository.MAX_FONT_SCALE
        )
    }

    override val isSimpleMode: Flow<Boolean> = context.displayDataStore.data.map { preferences ->
        preferences[SIMPLE_MODE_KEY] ?: false
    }

    override suspend fun setFontSizeMultiplier(multiplier: Float) {
        val clamped = multiplier.coerceIn(
            DisplayPreferencesRepository.MIN_FONT_SCALE,
            DisplayPreferencesRepository.MAX_FONT_SCALE
        )
        context.displayDataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = (clamped * 100).toInt()
        }
    }

    override suspend fun setSimpleMode(enabled: Boolean) {
        context.displayDataStore.edit { preferences ->
            preferences[SIMPLE_MODE_KEY] = enabled
        }
    }

    private companion object {
        val FONT_SIZE_KEY = intPreferencesKey("font_size_percent")
        val SIMPLE_MODE_KEY = booleanPreferencesKey("simple_mode")
    }
}
