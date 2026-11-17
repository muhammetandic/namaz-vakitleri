package com.anttech.prayertimes.features.settings.domain

import kotlinx.coroutines.flow.Flow

/**
 * Vakit bildirimleri tercihleri. Varsayılan olarak bildirimler ve titreşim açıktır.
 */
interface NotificationPreferencesRepository {
    val notificationsEnabled: Flow<Boolean>
    val vibrationEnabled: Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
}
