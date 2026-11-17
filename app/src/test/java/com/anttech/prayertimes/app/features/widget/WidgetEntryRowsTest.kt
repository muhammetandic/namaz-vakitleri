package com.anttech.prayertimes.features.widget

import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.times.domain.PrayerType
import org.junit.Test
import java.time.LocalTime

/**
 * Büyük widget vakitleri iki sıra halinde gösterir: 3 + 3.
 */
class WidgetEntryRowsTest {

    private val entries = PrayerType.entries.mapIndexed { index, type ->
        PrayerTimeEntry(
            type = type,
            time = LocalTime.of(5 + index, 0),
            isActive = index == 0
        )
    }

    @Test
    fun `six entries split into two rows of three`() {
        val rows = entries.toRows(3)

        assertThat(rows).hasSize(2)
        assertThat(rows[0]).hasSize(3)
        assertThat(rows[1]).hasSize(3)
        assertThat(rows[0].map { it.type }).containsExactly(
            PrayerType.IMSAK, PrayerType.GUNES, PrayerType.OGLE
        ).inOrder()
        assertThat(rows[1].map { it.type }).containsExactly(
            PrayerType.IKINDI, PrayerType.AKSAM, PrayerType.YATSI
        ).inOrder()
    }

    @Test
    fun `every entry is preserved exactly once`() {
        val rows = entries.toRows(3)

        assertThat(rows.flatten()).containsExactlyElementsIn(entries).inOrder()
    }

    @Test
    fun `row size is clamped to at least one`() {
        val rows = entries.toRows(0)

        assertThat(rows).hasSize(6)
        assertThat(rows.all { it.size == 1 }).isTrue()
    }

    @Test
    fun `empty list yields no rows`() {
        assertThat(emptyList<PrayerTimeEntry>().toRows(3)).isEmpty()
    }
}
