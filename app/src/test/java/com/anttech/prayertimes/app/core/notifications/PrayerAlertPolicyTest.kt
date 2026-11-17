package com.anttech.prayertimes.core.notifications

import android.media.AudioManager
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.settings.domain.SoundOption
import org.junit.Test

class PrayerAlertPolicyTest {

    private val adhan = SoundOption.EZAN_TURKIYE
    private val custom = SoundOption.CustomFile(
        id = "uri",
        title = "Özel Ezan",
        uriString = "content://media/1"
    )

    private fun policyFor(ringerMode: Int) = PrayerAlertPolicy { ringerMode }

    @Test
    fun `normal ringer plays sound only without text notification`() {
        val policy = policyFor(AudioManager.RINGER_MODE_NORMAL)

        assertThat(policy.modeFor(adhan)).isEqualTo(PrayerAlertMode.SOUND_ONLY)
        assertThat(policy.modeFor(custom)).isEqualTo(PrayerAlertMode.SOUND_ONLY)
    }

    @Test
    fun `silent ringer posts text notification only`() {
        val policy = policyFor(AudioManager.RINGER_MODE_SILENT)

        assertThat(policy.modeFor(adhan)).isEqualTo(PrayerAlertMode.TEXT_ONLY)
    }

    @Test
    fun `vibrate ringer posts text notification only`() {
        val policy = policyFor(AudioManager.RINGER_MODE_VIBRATE)

        assertThat(policy.modeFor(adhan)).isEqualTo(PrayerAlertMode.TEXT_ONLY)
    }

    @Test
    fun `silent sound option always falls back to text even when phone is loud`() {
        val policy = policyFor(AudioManager.RINGER_MODE_NORMAL)

        assertThat(policy.modeFor(SoundOption.Silent)).isEqualTo(PrayerAlertMode.TEXT_ONLY)
    }

    @Test
    fun `vibrate-only option is text only in every ringer mode`() {
        listOf(
            AudioManager.RINGER_MODE_NORMAL,
            AudioManager.RINGER_MODE_SILENT,
            AudioManager.RINGER_MODE_VIBRATE
        ).forEach { ringer ->
            val policy = policyFor(ringer)

            assertThat(policy.modeFor(SoundOption.VibrateOnly)).isEqualTo(PrayerAlertMode.TEXT_ONLY)
        }
    }

    @Test
    fun `vibrate-only option vibrates even when the preference is off`() {
        val policy = policyFor(AudioManager.RINGER_MODE_SILENT)

        assertThat(policy.vibrationEnabledFor(SoundOption.VibrateOnly, preferenceEnabled = false))
            .isTrue()
    }

    @Test
    fun `silent option never vibrates`() {
        val policy = policyFor(AudioManager.RINGER_MODE_NORMAL)

        assertThat(policy.vibrationEnabledFor(SoundOption.Silent, preferenceEnabled = true))
            .isFalse()
    }

    @Test
    fun `regular sounds follow the vibration preference`() {
        val policy = policyFor(AudioManager.RINGER_MODE_SILENT)

        assertThat(policy.vibrationEnabledFor(adhan, preferenceEnabled = true)).isTrue()
        assertThat(policy.vibrationEnabledFor(adhan, preferenceEnabled = false)).isFalse()
    }
}
