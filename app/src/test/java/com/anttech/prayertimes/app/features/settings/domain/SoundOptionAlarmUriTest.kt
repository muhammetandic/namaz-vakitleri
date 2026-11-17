package com.anttech.prayertimes.features.settings.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SoundOptionAlarmUriTest {

    private val packageName = "com.anttech.prayertimes"

    @Test
    fun `built-in sound maps to android resource uri`() {
        val uri = SoundOption.EZAN_ANVER_DUMAN.alarmSoundUri(packageName)

        assertThat(uri).isEqualTo("android.resource://com.anttech.prayertimes/raw/ezan2")
    }

    @Test
    fun `custom file keeps its content uri`() {
        val custom = SoundOption.CustomFile(
            id = "content://media/1",
            title = "Özel Ezan",
            uriString = "content://media/1"
        )

        assertThat(custom.alarmSoundUri(packageName)).isEqualTo("content://media/1")
    }

    @Test
    fun `silent option has no alarm sound`() {
        assertThat(SoundOption.Silent.alarmSoundUri(packageName)).isNull()
        assertThat(SoundOption.VibrateOnly.alarmSoundUri(packageName)).isNull()
    }
}
