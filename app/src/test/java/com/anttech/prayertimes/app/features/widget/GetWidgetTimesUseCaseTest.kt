package com.anttech.prayertimes.features.widget

import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Widget'ın gösterdiği vakit modeli:
 * Sıradaki vakit + vakti, aktif vakit vurgusu ve kalan süre etiketi
 * CalculateNextPrayerUseCase ile tutarlı hesaplanmalı.
 */
class GetWidgetTimesUseCaseTest {

    private val useCase = GetWidgetTimesUseCase()

    private val times = PrayerTimes(
        city = "İstanbul",
        date = LocalDate.of(2026, 11, 12),
        imsak = LocalTime.of(5, 53),
        gunes = LocalTime.of(7, 25),
        ogle = LocalTime.of(12, 47),
        ikindi = LocalTime.of(15, 34),
        aksam = LocalTime.of(17, 57),
        yatsi = LocalTime.of(19, 14),
        tomorrowImsak = LocalTime.of(5, 54)
    )

    @Test
    fun `mid afternoon computes next prayer with its time, active prayer and remaining label`() {
        val model = useCase(times, LocalDateTime.of(2026, 11, 12, 13, 0))

        assertThat(model).isNotNull()
        assertThat(model!!.city).isEqualTo("İstanbul")
        assertThat(model.nextPrayer).isEqualTo(PrayerType.IKINDI)
        assertThat(model.nextPrayerTime).isEqualTo(LocalTime.of(15, 34))
        assertThat(model.activePrayer).isEqualTo(PrayerType.OGLE)
        assertThat(model.remainingLabel).isEqualTo("2 sa 34 dk")

        assertThat(model.entries).hasSize(6)
        assertThat(model.entries.first { it.type == PrayerType.OGLE }.isActive).isTrue()
        assertThat(model.entries.first { it.type == PrayerType.IKINDI }.isActive).isFalse()
        assertThat(model.entries.first { it.type == PrayerType.YATSI }.time)
            .isEqualTo(LocalTime.of(19, 14))
    }

    @Test
    fun `before imsak points to today's imsak`() {
        val model = useCase(times, LocalDateTime.of(2026, 11, 12, 3, 0))

        assertThat(model!!.nextPrayer).isEqualTo(PrayerType.IMSAK)
        assertThat(model.nextPrayerTime).isEqualTo(LocalTime.of(5, 53))
        assertThat(model.activePrayer).isEqualTo(PrayerType.YATSI)
    }

    @Test
    fun `after yatsi points to tomorrow's imsak`() {
        val model = useCase(times, LocalDateTime.of(2026, 11, 12, 21, 0))

        assertThat(model!!.nextPrayer).isEqualTo(PrayerType.IMSAK)
        assertThat(model.nextPrayerTime).isEqualTo(LocalTime.of(5, 54))
        assertThat(model.activePrayer).isEqualTo(PrayerType.YATSI)
    }

    @Test
    fun `remaining under a minute reads az kaldı`() {
        val model = useCase(times, LocalDateTime.of(2026, 11, 12, 15, 33, 30))

        assertThat(model!!.remainingLabel).isEqualTo("az kaldı")
    }

    @Test
    fun `remaining minutes only label omits hours`() {
        val model = useCase(times, LocalDateTime.of(2026, 11, 12, 17, 40))

        assertThat(model!!.remainingLabel).isEqualTo("17 dk")
    }

    @Test
    fun `null times returns null model`() {
        val model = useCase(null, LocalDateTime.of(2026, 11, 12, 13, 0))

        assertThat(model).isNull()
    }
}
