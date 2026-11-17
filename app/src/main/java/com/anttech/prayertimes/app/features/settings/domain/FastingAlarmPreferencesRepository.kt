package com.anttech.prayertimes.features.settings.domain

import kotlinx.coroutines.flow.Flow

/**
 * Sahur uyandırma alarmı tercihleri.
 *
 * Alarm, imsak vaktinden [sahurAlarmMinutesBefore] dakika önce kurulur.
 * Ses olarak imsak vakti için seçilen ses kullanılır.
 */
interface FastingAlarmPreferencesRepository {

    val sahurAlarmEnabled: Flow<Boolean>
    val sahurAlarmMinutesBefore: Flow<Int>

    suspend fun setSahurAlarmEnabled(enabled: Boolean)
    suspend fun setSahurAlarmMinutesBefore(minutes: Int)

    companion object {
        const val DEFAULT_ENABLED = false
        const val DEFAULT_MINUTES_BEFORE = 30

        /** İmsaktan önce seçilebilen dakika seçenekleri. */
        val MINUTE_OPTIONS: List<Int> = listOf(15, 30, 45, 60, 75, 90)
    }
}
