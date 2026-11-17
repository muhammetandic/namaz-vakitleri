package com.anttech.prayertimes.features.calendar.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ImsakiyeDayTest {

    private fun dayOf(date: LocalDate) = ImsakiyeDay(
        date = date,
        imsakFormatted = "05:00",
        gunesFormatted = "06:30",
        ogleFormatted = "12:00",
        ikindiFormatted = "15:00",
        aksamFormatted = "18:00",
        yatsiFormatted = "19:00",
        hijriDay = 15,
        hijriMonth = 3,
        hijriMonthName = "Rebiülevvel",
        hijriYear = 1448
    )

    @Test
    fun `day of week short names are unambiguous Turkish abbreviations`() {
        val cases = mapOf(
            LocalDate.of(2026, 11, 2) to "Pzt", // Pazartesi
            LocalDate.of(2026, 11, 3) to "Sal", // Salı
            LocalDate.of(2026, 11, 4) to "Çar", // Çarşamba
            LocalDate.of(2026, 11, 5) to "Per", // Perşembe
            LocalDate.of(2026, 11, 6) to "Cum", // Cuma
            LocalDate.of(2026, 11, 7) to "Cts", // Cumartesi
            LocalDate.of(2026, 11, 8) to "Paz"  // Pazar
        )

        cases.forEach { (date, expected) ->
            assertThat(dayOf(date).dayOfWeekShortTurkish).isEqualTo(expected)
        }
    }

    @Test
    fun `month short names use first three Turkish letters`() {
        val cases = mapOf(
            LocalDate.of(2026, 1, 15) to "Oca",
            LocalDate.of(2026, 2, 15) to "Şub",
            LocalDate.of(2026, 3, 15) to "Mar",
            LocalDate.of(2026, 4, 15) to "Nis",
            LocalDate.of(2026, 5, 15) to "May",
            LocalDate.of(2026, 6, 15) to "Haz",
            LocalDate.of(2026, 7, 15) to "Tem",
            LocalDate.of(2026, 8, 15) to "Ağu",
            LocalDate.of(2026, 9, 15) to "Eyl",
            LocalDate.of(2026, 10, 15) to "Eki",
            LocalDate.of(2026, 11, 15) to "Kas",
            LocalDate.of(2026, 12, 15) to "Ara"
        )

        cases.forEach { (date, expected) ->
            assertThat(dayOf(date).monthShortTurkish).isEqualTo(expected)
        }
    }

    @Test
    fun `hijri fields are correctly set`() {
        val day = dayOf(LocalDate.of(2026, 11, 5))
        assertThat(day.hijriDay).isEqualTo(15)
        assertThat(day.hijriMonth).isEqualTo(3)
        assertThat(day.hijriMonthName).isEqualTo("Rebiülevvel")
        assertThat(day.hijriYear).isEqualTo(1448)
    }
}
