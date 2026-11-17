package com.anttech.prayertimes.features.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Geri sayım hedefi: sıradaki vakit bugün geçmediyse bugün, geçtiyse yarın.
 * Chronometer bu süreyle kurulur ve sistem saatiyle aynı saniyede akar.
 */
class WidgetCountdownTest {

    @Test
    fun `bugun icindeki vakit icin kalan sure hesaplanir`() {
        val now = LocalDateTime.of(2026, 11, 12, 21, 0)

        val millis = WidgetCountdown.remainingMillis(now, LocalTime.of(5, 53))

        assertThat(millis).isEqualTo(8L * 60 * 60 * 1000 + 53L * 60 * 1000)
    }

    @Test
    fun `gecmis vakit yarina tasinir`() {
        val now = LocalDateTime.of(2026, 11, 12, 23, 30)

        val millis = WidgetCountdown.remainingMillis(now, LocalTime.of(19, 14))

        assertThat(millis).isEqualTo(19L * 60 * 60 * 1000 + 44L * 60 * 1000)
    }

    @Test
    fun `tam vakit aninda hedef yarina tasinir`() {
        val now = LocalDateTime.of(2026, 11, 12, 5, 53)

        val millis = WidgetCountdown.remainingMillis(now, LocalTime.of(5, 53))

        assertThat(millis).isEqualTo(24L * 60 * 60 * 1000)
    }

    @Test
    fun `kalan sure negatif olmaz`() {
        val now = LocalDateTime.of(2026, 11, 12, 12, 0)

        assertThat(WidgetCountdown.remainingMillis(now, LocalTime.of(12, 0))).isAtLeast(0L)
    }
}
