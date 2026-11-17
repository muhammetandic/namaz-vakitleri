package com.anttech.prayertimes.features.settings.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notification_preferences")

class DataStoreNotificationPreferencesRepository(
    private val context: Context
) : NotificationPreferencesRepository {

    override val notificationsEnabled: Flow<Boolean> =
        context.notificationDataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
        }

    override val vibrationEnabled: Flow<Boolean> =
        context.notificationDataStore.data.map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] ?: true
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        context.notificationDataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    private companion object {
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    }
}
