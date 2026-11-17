package com.anttech.prayertimes.core.notifications

import com.anttech.prayertimes.features.times.domain.PrayerType
import java.time.LocalDateTime

data class AlarmItem(
    val id: Int,
    val triggerDateTime: LocalDateTime,
    val prayerType: PrayerType,
    val title: String,
    val message: String,
    val soundUri: String? = null,
    val isPreAlarm: Boolean = false,
    /** Kullanıcı gecikmesiyle kurulan ikinci aşama alarmı. */
    val isDelayedAlert: Boolean = false
)

interface AlarmScheduler {
    fun schedule(item: AlarmItem)
    fun cancel(item: AlarmItem)
}
