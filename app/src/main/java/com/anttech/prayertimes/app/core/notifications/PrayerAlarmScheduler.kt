package com.anttech.prayertimes.core.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.features.times.domain.TimesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

interface LastCityStore {
    suspend fun saveCity(city: String)
    suspend fun getCity(): String?
}

private val Context.alarmDataStore by preferencesDataStore(name = "alarm_preferences")

class DataStoreLastCityStore(
    private val context: Context
) : LastCityStore {

    override suspend fun saveCity(city: String) {
        context.alarmDataStore.edit { preferences ->
            preferences[LAST_CITY_KEY] = city
        }
    }

    override suspend fun getCity(): String? {
        return context.alarmDataStore.data.first()[LAST_CITY_KEY]
    }

    private companion object {
        val LAST_CITY_KEY = stringPreferencesKey("last_city")
    }
}

/**
 * Vakit alarmlarını yerel veritabanındaki vakitlere göre kurar.
 * Ses, alarm tetiklendiğinde güncel kullanıcı tercihinden çözülür.
 *
 * Alarmlar yalnızca bildirim için değil, ana ekran widget'larını vakit
 * değişiminde tazelemek için de kurulur. Bu yüzden bir widget ana ekranda
 * duruyorsa bildirimler kapalı olsa bile alarmlar korunur
 * ([hasActiveWidgets]); bildirim gösterimi `AlarmReceiver` tarafından
 * ayrıca tercihe göre süzülür.
 */
class PrayerAlarmScheduler(
    private val repository: TimesRepository,
    private val alarmScheduler: AlarmScheduler,
    private val lastCityStore: LastCityStore,
    private val notificationPreferences: NotificationPreferencesRepository? = null,
    private val fastingAlarmPreferences: FastingAlarmPreferencesRepository? = null,
    private val daysAhead: Int = 62,
    private val hasActiveWidgets: suspend () -> Boolean = { false }
) {

    suspend fun scheduleUpcoming(city: String) {
        lastCityStore.saveCity(city)
        if (!alarmsNeeded()) {
            // Ne bildirim ne widget gerekiyor: bekleyen alarmları temizle.
            cancelScheduled()
            return
        }
        val sahurMinutesBefore = sahurAlarmMinutesBefore()
        val start = LocalDate.now()
        for (offset in 0 until daysAhead) {
            val date = start.plusDays(offset.toLong())
            val times = repository.getPrayerTimesForDate(city, date).firstOrNull() ?: continue
            scheduleDay(times, sahurMinutesBefore)
        }
    }

    suspend fun rescheduleSaved() {
        val city = lastCityStore.getCity() ?: return
        scheduleUpcoming(city)
    }

    /** Alarm kurulması gerekiyor mu: bildirim açık, sahur alarmı açık veya widget var. */
    private suspend fun alarmsNeeded(): Boolean =
        notificationsEnabled() || hasActiveWidgets() || sahurAlarmMinutesBefore() != null

    private suspend fun notificationsEnabled(): Boolean {
        return notificationPreferences?.notificationsEnabled?.first() ?: true
    }

    /** Sahur alarmı açıksa imsaktan kaç dakika önce kurulacağını döner. */
    private suspend fun sahurAlarmMinutesBefore(): Int? {
        val preferences = fastingAlarmPreferences ?: return null
        if (!preferences.sahurAlarmEnabled.first()) return null
        return preferences.sahurAlarmMinutesBefore.first()
    }

    private fun cancelScheduled() {
        val start = LocalDate.now()
        for (offset in 0 until daysAhead) {
            val date = start.plusDays(offset.toLong())
            PrayerType.entries.forEach { prayerType ->
                alarmScheduler.cancel(
                    AlarmItem(
                        id = alarmId(date, prayerType),
                        triggerDateTime = LocalDateTime.of(date, LocalTime.MIN),
                        prayerType = prayerType,
                        title = "",
                        message = ""
                    )
                )
            }
            alarmScheduler.cancel(
                AlarmItem(
                    id = sahurAlarmId(date),
                    triggerDateTime = LocalDateTime.of(date, LocalTime.MIN),
                    prayerType = PrayerType.IMSAK,
                    title = "",
                    message = ""
                )
            )
        }
    }

    private fun scheduleDay(times: PrayerTimes, sahurMinutesBefore: Int? = null) {
        val slots = listOf(
            PrayerType.IMSAK to times.imsak,
            PrayerType.GUNES to times.gunes,
            PrayerType.OGLE to times.ogle,
            PrayerType.IKINDI to times.ikindi,
            PrayerType.AKSAM to times.aksam,
            PrayerType.YATSI to times.yatsi
        )
        val now = LocalDateTime.now()
        slots.forEach { (prayerType, time) ->
            val trigger = LocalDateTime.of(times.date, time)
            if (trigger.isBefore(now)) return@forEach
            alarmScheduler.schedule(
                AlarmItem(
                    id = alarmId(times.date, prayerType),
                    triggerDateTime = trigger,
                    prayerType = prayerType,
                    title = "${prayerType.turkishName} Vakti",
                    message = "${prayerType.turkishName} vakti girdi. ${prayerType.shortDescription}"
                )
            )
        }

        // Sahur uyandırma alarmı: imsak vaktinden belirtilen dakika önce.
        if (sahurMinutesBefore != null) {
            val trigger = LocalDateTime.of(times.date, times.imsak).minusMinutes(sahurMinutesBefore.toLong())
            if (!trigger.isBefore(now)) {
                alarmScheduler.schedule(
                    AlarmItem(
                        id = sahurAlarmId(times.date),
                        triggerDateTime = trigger,
                        prayerType = PrayerType.IMSAK,
                        title = "Sahur Uyandırma",
                        message = "İmsak vaktine $sahurMinutesBefore dakika kaldı.",
                        isPreAlarm = true
                    )
                )
            }
        }
    }

    private fun alarmId(date: LocalDate, prayerType: PrayerType): Int {
        return date.toEpochDay().toInt() * 10 + prayerType.ordinal
    }

    /** Vakit alarm kimlikleriyle çakışmayan ayrı kimlik. */
    private fun sahurAlarmId(date: LocalDate): Int {
        return date.toEpochDay().toInt() * 10 + SAHUR_ALARM_SLOT
    }

    companion object {
        /** Vakit ordinal'ları 0..5 kullanır; 6..9 boştur. */
        const val SAHUR_ALARM_SLOT = 7
    }
}
