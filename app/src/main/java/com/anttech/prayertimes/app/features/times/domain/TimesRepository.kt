package com.anttech.prayertimes.features.times.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TimesRepository {
    fun getPrayerTimesForDate(city: String, date: LocalDate): Flow<PrayerTimes?>
    fun getMonthlyPrayerTimes(city: String, yearMonth: String): Flow<List<PrayerTimes>>

    /** [start] ile [end] (dahil) arasındaki vakitleri tek sorguda getirir. */
    suspend fun getPrayerTimesBetween(city: String, start: LocalDate, end: LocalDate): List<PrayerTimes>

    suspend fun syncMonthlyPrayerTimes(city: String): Result<Unit>
    suspend fun syncMonthlyPrayerTimesFor(city: String, yearMonth: String): Result<Unit>
    suspend fun hasMonthlyPrayerTimes(city: String, yearMonth: String): Boolean
    suspend fun getCachedCity(): String?
}
