package com.anttech.prayertimes.features.widget

import android.content.Context
import com.anttech.prayertimes.core.notifications.LastCityStore
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.features.times.domain.TimesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Widget'lar Hilt görünümü olmadığından bağımlılıklar EntryPoint ile alınır.
 * Şehir kaynağı alarmla aynı: önce DataStore'daki son şehir, yoksa Room'dan
 * son kullanılan (GPS modu).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun timesRepository(): TimesRepository
    fun lastCityStore(): LastCityStore
    fun prayerAlarmScheduler(): PrayerAlarmScheduler
}

object WidgetDataLoader {

    suspend fun load(context: Context): WidgetTimesModel? {
        val appContext = context.applicationContext
        val entryPoint =
            EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val repository = entryPoint.timesRepository()

        val city = entryPoint.lastCityStore().getCity()
            ?: repository.getCachedCity()
            ?: return null

        val times = repository.getPrayerTimesForDate(city, LocalDate.now()).firstOrNull()
            ?: return null

        return GetWidgetTimesUseCase()(times, LocalDateTime.now())
    }
}
