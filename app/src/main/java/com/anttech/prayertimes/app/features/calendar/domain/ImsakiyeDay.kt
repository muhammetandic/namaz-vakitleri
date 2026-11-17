package com.anttech.prayertimes.features.calendar.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter

data class ImsakiyeDay(
    val date: LocalDate,
    val dayNumber: Int = date.dayOfMonth,
    val dayOfWeekTurkish: String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Pazartesi"
        DayOfWeek.TUESDAY -> "Salı"
        DayOfWeek.WEDNESDAY -> "Çarşamba"
        DayOfWeek.THURSDAY -> "Perşembe"
        DayOfWeek.FRIDAY -> "Cuma"
        DayOfWeek.SATURDAY -> "Cumartesi"
        DayOfWeek.SUNDAY -> "Pazar"
        else -> ""
    },
    val dayOfWeekShortTurkish: String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Pzt"
        DayOfWeek.TUESDAY -> "Sal"
        DayOfWeek.WEDNESDAY -> "Çar"
        DayOfWeek.THURSDAY -> "Per"
        DayOfWeek.FRIDAY -> "Cum"
        DayOfWeek.SATURDAY -> "Cts"
        DayOfWeek.SUNDAY -> "Paz"
        else -> ""
    },
    val monthShortTurkish: String = when (date.month) {
        Month.JANUARY -> "Oca"
        Month.FEBRUARY -> "Şub"
        Month.MARCH -> "Mar"
        Month.APRIL -> "Nis"
        Month.MAY -> "May"
        Month.JUNE -> "Haz"
        Month.JULY -> "Tem"
        Month.AUGUST -> "Ağu"
        Month.SEPTEMBER -> "Eyl"
        Month.OCTOBER -> "Eki"
        Month.NOVEMBER -> "Kas"
        Month.DECEMBER -> "Ara"
        else -> ""
    },
    val imsakFormatted: String,
    val gunesFormatted: String,
    val ogleFormatted: String,
    val ikindiFormatted: String,
    val aksamFormatted: String,
    val yatsiFormatted: String,
    val isToday: Boolean = date == LocalDate.now(),
    val isFriday: Boolean = date.dayOfWeek == DayOfWeek.FRIDAY,
    val isFasted: Boolean = false,
    // Hijri date fields
    val hijriDay: Int = 1,
    val hijriMonth: Int = 1,
    val hijriMonthName: String = "",
    val hijriYear: Int = 1448,
    val isForbidden: Boolean = false,
    val forbiddenReason: String = ""
)
