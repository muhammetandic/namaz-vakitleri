package com.anttech.prayertimes.features.times.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CalculateNextPrayerUseCaseTest {

    private lateinit var useCase: CalculateNextPrayerUseCase

    private val testDate: LocalDate = LocalDate.of(2026, 11, 5)
    
    // Test prayer times:
    // İmsak: 05:45
    // Güneş: 07:10
    // Öğle:  12:50
    // İkindi: 15:40
    // Akşam: 18:05
    // Yatsı: 19:25
    // Tomorrow's İmsak: 05:46
    private val todayPrayerTimes = PrayerTimes(
        city = "İstanbul",
        date = testDate,
        imsak = LocalTime.of(5, 45),
        gunes = LocalTime.of(7, 10),
        ogle = LocalTime.of(12, 50),
        ikindi = LocalTime.of(15, 40),
        aksam = LocalTime.of(18, 5),
        yatsi = LocalTime.of(19, 25),
        tomorrowImsak = LocalTime.of(5, 46)
    )

    @Before
    fun setUp() {
        useCase = CalculateNextPrayerUseCase()
    }

    @Test
    fun `when current time is between Imsak and Gunes, next prayer is Gunes and active is Imsak`() {
        // Given current time is 06:15
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(6, 15, 0))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.IMSAK)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.GUNES)
        // 07:10 - 06:15 = 55 minutes = 3300 seconds
        assertThat(result.remainingSeconds).isEqualTo(55 * 60L)
        assertThat(result.formattedCountdown).isEqualTo("00:55:00")
    }

    @Test
    fun `when current time is between Gunes and Ogle, next prayer is Ogle and active is Gunes`() {
        // Given current time is 10:00
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(10, 0, 0))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.GUNES)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.OGLE)
        // 12:50 - 10:00 = 2 hours 50 minutes = 170 minutes = 10200 seconds
        assertThat(result.remainingSeconds).isEqualTo(2 * 3600L + 50 * 60L)
        assertThat(result.formattedCountdown).isEqualTo("02:50:00")
    }

    @Test
    fun `when current time is between Ogle and Ikindi, next prayer is Ikindi and active is Ogle`() {
        // Given current time is 13:00
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(13, 0, 0))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.OGLE)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.IKINDI)
        // 15:40 - 13:00 = 2 hours 40 minutes = 9600 seconds
        assertThat(result.remainingSeconds).isEqualTo(2 * 3600L + 40 * 60L)
        assertThat(result.formattedCountdown).isEqualTo("02:40:00")
    }

    @Test
    fun `when current time is between Ikindi and Aksam, next prayer is Aksam and active is Ikindi`() {
        // Given current time is 17:35:10
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(17, 35, 10))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.IKINDI)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.AKSAM)
        // 18:05:00 - 17:35:10 = 29 minutes 50 seconds = 1790 seconds
        assertThat(result.remainingSeconds).isEqualTo(29 * 60L + 50L)
        assertThat(result.formattedCountdown).isEqualTo("00:29:50")
    }

    @Test
    fun `when current time is between Aksam and Yatsi, next prayer is Yatsi and active is Aksam`() {
        // Given current time is 18:30
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(18, 30, 0))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.AKSAM)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.YATSI)
        // 19:25 - 18:30 = 55 minutes
        assertThat(result.remainingSeconds).isEqualTo(55 * 60L)
        assertThat(result.formattedCountdown).isEqualTo("00:55:00")
    }

    @Test
    fun `when current time is after Yatsi, next prayer is tomorrow Imsak and active is Yatsi`() {
        // Given current time is 22:00 at night
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(22, 0, 0))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.YATSI)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.IMSAK)
        // From 22:00 to 24:00 (2 hrs) + 05:46 (5 hrs 46 mins) = 7 hrs 46 mins = 27960 seconds
        val expectedSeconds = (7 * 3600L) + (46 * 60L)
        assertThat(result.remainingSeconds).isEqualTo(expectedSeconds)
        assertThat(result.formattedCountdown).isEqualTo("07:46:00")
    }

    @Test
    fun `when current time is early morning before Imsak, next prayer is today Imsak and active is Yatsi`() {
        // Given current time is 03:30 in the morning
        val currentTime = LocalDateTime.of(testDate, LocalTime.of(3, 30, 0))

        // When
        val result = useCase(todayPrayerTimes, currentTime)

        // Then
        assertThat(result.activePrayer).isEqualTo(PrayerType.YATSI)
        assertThat(result.nextPrayer).isEqualTo(PrayerType.IMSAK)
        // 05:45 - 03:30 = 2 hours 15 minutes = 8100 seconds
        assertThat(result.remainingSeconds).isEqualTo(2 * 3600L + 15 * 60L)
        assertThat(result.formattedCountdown).isEqualTo("02:15:00")
    }

    @Test
    fun `progress is calculated accurately between 0 and 1`() {
        // Given: Imsak is 05:45, Gunes is 07:10 (Total duration: 85 mins)
        // At 05:45 progress should be 0.0
        val startResult = useCase(todayPrayerTimes, LocalDateTime.of(testDate, LocalTime.of(5, 45)))
        assertThat(startResult.progress).isWithin(0.01f).of(0.0f)

        // Halfway at ~06:27:30 progress should be ~0.5
        val halfTime = LocalDateTime.of(testDate, LocalTime.of(6, 27, 30))
        val midResult = useCase(todayPrayerTimes, halfTime)
        assertThat(midResult.progress).isWithin(0.02f).of(0.5f)
    }
}
