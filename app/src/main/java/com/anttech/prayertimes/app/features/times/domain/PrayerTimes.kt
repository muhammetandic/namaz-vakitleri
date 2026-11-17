package com.anttech.prayertimes.features.times.domain

import java.time.LocalDate
import java.time.LocalTime

data class PrayerTimes(
    val city: String,
    val date: LocalDate,
    val imsak: LocalTime,
    val gunes: LocalTime,
    val ogle: LocalTime,
    val ikindi: LocalTime,
    val aksam: LocalTime,
    val yatsi: LocalTime,
    val tomorrowImsak: LocalTime? = null,
    val hijriDay: Int = 1,
    val hijriMonth: Int = 1,
    val hijriMonthName: String = "",
    val hijriYear: Int = 1448
)
