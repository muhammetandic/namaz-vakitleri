package com.anttech.prayertimes.features.times.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CalculateKerahaStatusUseCaseTest {

    private lateinit var useCase: CalculateKerahaStatusUseCase

    private val testDate: LocalDate = LocalDate.of(2026, 11, 5)

    private val prayerTimes = PrayerTimes(
        city = "İstanbul",
        date = testDate,
        imsak = LocalTime.of(5, 45),
        gunes = LocalTime.of(7, 10),  // Sunrise
        ogle = LocalTime.of(12, 50),
        ikindi = LocalTime.of(15, 40),
        aksam = LocalTime.of(18, 5),   // Sunset
        yatsi = LocalTime.of(19, 25)
    )

    @Before
    fun setUp() {
        useCase = CalculateKerahaStatusUseCase()
    }

    @Test
    fun `when time is within 45 minutes after sunrise, keraha status is SUNRISE_KERAHA`() {
        // Güneş 07:10, so until 07:55 it is Sunrise Keraha
        val time = LocalDateTime.of(testDate, LocalTime.of(7, 30))

        val result = useCase(prayerTimes, time)

        assertThat(result.isKeraha).isTrue()
        assertThat(result.type).isEqualTo(KerahaType.SUNRISE)
        assertThat(result.minutesRemaining).isEqualTo(25) // 07:55 - 07:30
    }

    @Test
    fun `when time is 45 minutes before sunset, keraha status is SUNSET_KERAHA`() {
        // Akşam 18:05, so from 17:20 to 18:05 it is Sunset Keraha
        val time = LocalDateTime.of(testDate, LocalTime.of(17, 45))

        val result = useCase(prayerTimes, time)

        assertThat(result.isKeraha).isTrue()
        assertThat(result.type).isEqualTo(KerahaType.SUNSET)
        assertThat(result.minutesRemaining).isEqualTo(20) // 18:05 - 17:45
    }

    @Test
    fun `when time is 30 minutes before noon, keraha status is NOON_ZEVAL`() {
        // Öğle 12:50, so from 12:05 (45 mins before) it is Noon Keraha
        val time = LocalDateTime.of(testDate, LocalTime.of(12, 20))

        val result = useCase(prayerTimes, time)

        assertThat(result.isKeraha).isTrue()
        assertThat(result.type).isEqualTo(KerahaType.MIDDAY)
    }

    @Test
    fun `when time is during normal prayer time, isKeraha is false`() {
        // Öğle 12:50 -> time 14:00 (completely fine)
        val time = LocalDateTime.of(testDate, LocalTime.of(14, 0))

        val result = useCase(prayerTimes, time)

        assertThat(result.isKeraha).isFalse()
        assertThat(result.type).isEqualTo(KerahaType.NONE)
    }
}
