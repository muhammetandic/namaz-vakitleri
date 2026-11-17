package com.anttech.prayertimes.features.settings.domain

import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.times.domain.PrayerType
import org.junit.Test

class SoundOptionTest {

    @Test
    fun `ezan sounds are exposed as built-in options with matching raw resources`() {
        val ezans = SoundOption.DEFAULT_BUILT_IN_SOUNDS
            .filterIsInstance<SoundOption.BuiltIn>()
            .filter { it.resName.startsWith("ezan") }

        assertThat(ezans.map { it.resName })
            .containsExactly("ezan1", "ezan2", "ezan3", "ezan4")
    }

    @Test
    fun `all prayers default to ezan2`() {
        PrayerType.entries.forEach { prayerType ->
            val default = SoundOption.defaultFor(prayerType)

            assertThat(default).isEqualTo(SoundOption.EZAN_ANVER_DUMAN)
            assertThat((default as SoundOption.BuiltIn).resName).isEqualTo("ezan2")
        }
    }

    @Test
    fun `silent and vibrate-only are separate options`() {
        val options = SoundOption.DEFAULT_BUILT_IN_SOUNDS

        assertThat(options).contains(SoundOption.Silent)
        assertThat(options).contains(SoundOption.VibrateOnly)
        assertThat(SoundOption.Silent.id).isNotEqualTo(SoundOption.VibrateOnly.id)
        assertThat(SoundOption.Silent.title).isNotEqualTo(SoundOption.VibrateOnly.title)
    }
}
