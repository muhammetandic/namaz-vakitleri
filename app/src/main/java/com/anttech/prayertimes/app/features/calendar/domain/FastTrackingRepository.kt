package com.anttech.prayertimes.features.calendar.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FastTrackingRepository {
    suspend fun toggleFastedDay(city: String, date: LocalDate, hijriDay: Int, hijriMonth: Int, hijriMonthName: String, hijriYear: Int): Boolean
    fun getTrackingForDate(city: String, date: LocalDate): Flow<FastTrackingDay?>
    fun getMonthlyTracking(city: String, yearMonth: String): Flow<List<FastTrackingDay>>
    fun getAllMarkedDays(city: String): Flow<List<FastTrackingDay>>
    fun getForbiddenDays(city: String): Flow<List<FastTrackingDay>>
    fun getNonFastedRamadanDays(city: String): Flow<List<FastTrackingDay>>
    fun getFastedNonRamadanDays(city: String): Flow<List<FastTrackingDay>>
    suspend fun markForbiddenDays(city: String, year: Int)
}