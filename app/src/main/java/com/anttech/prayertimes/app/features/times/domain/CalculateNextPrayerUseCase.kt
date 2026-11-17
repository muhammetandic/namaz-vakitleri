package com.anttech.prayertimes.features.times.domain

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

class CalculateNextPrayerUseCase @Inject constructor() {

    operator fun invoke(
        prayerTimes: PrayerTimes,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): CountdownResult {
        val today = currentDateTime.toLocalDate()
        val currentTime = currentDateTime.toLocalTime()

        val imsakToday = LocalDateTime.of(today, prayerTimes.imsak)
        val gunesToday = LocalDateTime.of(today, prayerTimes.gunes)
        val ogleToday = LocalDateTime.of(today, prayerTimes.ogle)
        val ikindiToday = LocalDateTime.of(today, prayerTimes.ikindi)
        val aksamToday = LocalDateTime.of(today, prayerTimes.aksam)
        val yatsiToday = LocalDateTime.of(today, prayerTimes.yatsi)

        val tomorrow = today.plusDays(1)
        val nextImsakTime = prayerTimes.tomorrowImsak ?: prayerTimes.imsak
        val imsakTomorrow = LocalDateTime.of(tomorrow, nextImsakTime)

        val yesterday = today.minusDays(1)
        val yatsiYesterday = LocalDateTime.of(yesterday, prayerTimes.yatsi)

        val activePrayer: PrayerType
        val nextPrayer: PrayerType
        val periodStart: LocalDateTime
        val periodEnd: LocalDateTime

        when {
            currentDateTime.isBefore(imsakToday) -> {
                // Between yesterday's Yatsı and today's İmsak
                activePrayer = PrayerType.YATSI
                nextPrayer = PrayerType.IMSAK
                periodStart = yatsiYesterday
                periodEnd = imsakToday
            }
            currentDateTime.isBefore(gunesToday) -> {
                // Between İmsak and Güneş
                activePrayer = PrayerType.IMSAK
                nextPrayer = PrayerType.GUNES
                periodStart = imsakToday
                periodEnd = gunesToday
            }
            currentDateTime.isBefore(ogleToday) -> {
                // Between Güneş and Öğle
                activePrayer = PrayerType.GUNES
                nextPrayer = PrayerType.OGLE
                periodStart = gunesToday
                periodEnd = ogleToday
            }
            currentDateTime.isBefore(ikindiToday) -> {
                // Between Öğle and İkindi
                activePrayer = PrayerType.OGLE
                nextPrayer = PrayerType.IKINDI
                periodStart = ogleToday
                periodEnd = ikindiToday
            }
            currentDateTime.isBefore(aksamToday) -> {
                // Between İkindi and Akşam
                activePrayer = PrayerType.IKINDI
                nextPrayer = PrayerType.AKSAM
                periodStart = ikindiToday
                periodEnd = aksamToday
            }
            currentDateTime.isBefore(yatsiToday) -> {
                // Between Akşam and Yatsı
                activePrayer = PrayerType.AKSAM
                nextPrayer = PrayerType.YATSI
                periodStart = aksamToday
                periodEnd = yatsiToday
            }
            else -> {
                // After Yatsı until tomorrow's İmsak
                activePrayer = PrayerType.YATSI
                nextPrayer = PrayerType.IMSAK
                periodStart = yatsiToday
                periodEnd = imsakTomorrow
            }
        }

        val remainingDuration = Duration.between(currentDateTime, periodEnd)
        val totalPeriodDuration = Duration.between(periodStart, periodEnd)

        val remainingSeconds = maxOf(0L, remainingDuration.seconds)
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60

        val elapsedSeconds = maxOf(0L, Duration.between(periodStart, currentDateTime).seconds)
        val totalSeconds = maxOf(1L, totalPeriodDuration.seconds)
        val progress = (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0.0f, 1.0f)

        val formattedCountdown = String.format(
            Locale.US,
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )

        return CountdownResult(
            activePrayer = activePrayer,
            nextPrayer = nextPrayer,
            remainingSeconds = remainingSeconds,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            formattedCountdown = formattedCountdown,
            progress = progress
        )
    }
}
