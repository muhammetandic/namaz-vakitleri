package com.anttech.prayertimes.features.calendar.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class FastTrackingDayTest {

    @Test
    fun `ramadan non-fasted day shows tutulmadı label`() {
        val day = FastTrackingDay(
            date = LocalDate.of(2026, 3, 15),
            hijriDay = 15,
            hijriMonth = 9,
            hijriMonthName = "Ramazan",
            hijriYear = 1448,
            isMarked = true,
            isForbidden = false,
            city = "İstanbul"
        )

        assertThat(day.isRamadan).isTrue()
        assertThat(day.isNonFastedRamadan).isTrue()
        assertThat(day.isFastedNonRamadan).isFalse()
        assertThat(day.displayText).contains("tutulmadı")
    }

    @Test
    fun `ramadan fasted day shows oruç tutuldu`() {
        val day = FastTrackingDay(
            date = LocalDate.of(2026, 3, 15),
            hijriDay = 15,
            hijriMonth = 9,
            hijriMonthName = "Ramazan",
            hijriYear = 1448,
            isMarked = false,
            isForbidden = false,
            city = "İstanbul"
        )

        assertThat(day.isRamadan).isTrue()
        assertThat(day.isNonFastedRamadan).isFalse()
        assertThat(day.displayText).contains("Oruç tutuldu")
    }

    @Test
    fun `non-ramadan marked day shows nafile label`() {
        val day = FastTrackingDay(
            date = LocalDate.of(2026, 11, 5),
            hijriDay = 15,
            hijriMonth = 3,
            hijriMonthName = "Rebiülevvel",
            hijriYear = 1448,
            isMarked = true,
            isForbidden = false,
            city = "İstanbul"
        )

        assertThat(day.isRamadan).isFalse()
        assertThat(day.isFastedNonRamadan).isTrue()
        assertThat(day.isNonFastedRamadan).isFalse()
        assertThat(day.displayText).contains("(nafile)")
    }

    @Test
    fun `eid-al-fitr is forbidden`() {
        val day = FastTrackingDay(
            date = LocalDate.of(2026, 4, 20),
            hijriDay = 1,
            hijriMonth = 10,
            hijriMonthName = "Şevval",
            hijriYear = 1448,
            isMarked = false,
            isForbidden = true,
            city = "İstanbul"
        )

        assertThat(day.isForbidden).isTrue()
        assertThat(day.forbiddenReason).contains("Ramazan Bayramı")
    }

    @Test
    fun `eid-al-adha is forbidden`() {
        val day = FastTrackingDay(
            date = LocalDate.of(2026, 6, 27),
            hijriDay = 10,
            hijriMonth = 12,
            hijriMonthName = "Zilhicce",
            hijriYear = 1448,
            isMarked = false,
            isForbidden = true,
            city = "İstanbul"
        )

        assertThat(day.isForbidden).isTrue()
        assertThat(day.forbiddenReason).contains("Kurban Bayramı")
    }

    @Test
    fun `tashriq-days are forbidden`() {
        val day = FastTrackingDay(
            date = LocalDate.of(2026, 6, 28),
            hijriDay = 11,
            hijriMonth = 12,
            hijriMonthName = "Zilhicce",
            hijriYear = 1448,
            isMarked = false,
            isForbidden = true,
            city = "İstanbul"
        )

        assertThat(day.isForbidden).isTrue()
        assertThat(day.forbiddenReason).contains("Teşrik")
    }
}