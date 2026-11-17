package com.anttech.prayertimes.features.settings.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fastingDataStore by preferencesDataStore(name = "fasting_preferences")

class DataStoreFastingAlarmPreferencesRepository(
    private val context: Context
) : FastingAlarmPreferencesRepository {

    override val sahurAlarmEnabled: Flow<Boolean> = context.fastingDataStore.data.map { preferences ->
        preferences[SAHUR_ALARM_ENABLED_KEY] ?: FastingAlarmPreferencesRepository.DEFAULT_ENABLED
    }

    override val sahurAlarmMinutesBefore: Flow<Int> = context.fastingDataStore.data.map { preferences ->
        val stored = preferences[SAHUR_ALARM_MINUTES_KEY]
            ?: FastingAlarmPreferencesRepository.DEFAULT_MINUTES_BEFORE
        stored.coerceIn(
            FastingAlarmPreferencesRepository.MINUTE_OPTIONS.first(),
            FastingAlarmPreferencesRepository.MINUTE_OPTIONS.last()
        )
    }

    override suspend fun setSahurAlarmEnabled(enabled: Boolean) {
        context.fastingDataStore.edit { preferences ->
            preferences[SAHUR_ALARM_ENABLED_KEY] = enabled
        }
    }

    override suspend fun setSahurAlarmMinutesBefore(minutes: Int) {
        val coerceIn = minutes.coerceIn(
            FastingAlarmPreferencesRepository.MINUTE_OPTIONS.first(),
            FastingAlarmPreferencesRepository.MINUTE_OPTIONS.last()
        )
        context.fastingDataStore.edit { preferences ->
            preferences[SAHUR_ALARM_MINUTES_KEY] = coerceIn
        }
    }

    private companion object {
        val SAHUR_ALARM_ENABLED_KEY = booleanPreferencesKey("sahur_alarm_enabled")
        val SAHUR_ALARM_MINUTES_KEY = intPreferencesKey("sahur_alarm_minutes_before")
    }
}
