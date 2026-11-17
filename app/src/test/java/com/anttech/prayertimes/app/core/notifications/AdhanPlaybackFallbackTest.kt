package com.anttech.prayertimes.core.notifications

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class AdhanPlaybackFallbackTest {

    private val intent = mockk<Intent>(relaxed = true)

    @Test
    fun `fallback uses alarm details from playback intent`() {
        every { intent.getIntExtra(AdhanPlaybackService.EXTRA_ALARM_ID, 4712) } returns 88
        every { intent.getStringExtra(AdhanPlaybackService.EXTRA_MESSAGE) } returns "İmsak vakti yaklaştı"
        every { intent.getBooleanExtra(AdhanPlaybackService.EXTRA_VIBRATION_ENABLED, true) } returns false

        assertThat(AdhanPlaybackFallback.notificationId(intent, 4712)).isEqualTo(3088)
        assertThat(AdhanPlaybackFallback.message(intent)).isEqualTo("İmsak vakti yaklaştı")
        assertThat(AdhanPlaybackFallback.vibrationEnabled(intent)).isFalse()
    }

    @Test
    fun `fallback has safe defaults when extras are missing`() {
        every { intent.getIntExtra(AdhanPlaybackService.EXTRA_ALARM_ID, 4712) } returns 4712
        every { intent.getStringExtra(AdhanPlaybackService.EXTRA_MESSAGE) } returns null
        every { intent.getBooleanExtra(AdhanPlaybackService.EXTRA_VIBRATION_ENABLED, true) } returns true

        assertThat(AdhanPlaybackFallback.notificationId(intent, 4712))
            .isEqualTo(4712 + AdhanPlaybackFallback.NOTIFICATION_ID_OFFSET)
        assertThat(AdhanPlaybackFallback.message(intent)).isNotEmpty()
        assertThat(AdhanPlaybackFallback.vibrationEnabled(intent)).isTrue()
    }
}
