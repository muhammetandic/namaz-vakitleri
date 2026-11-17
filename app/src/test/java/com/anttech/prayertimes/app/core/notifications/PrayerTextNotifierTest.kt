package com.anttech.prayertimes.core.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerTextNotifierTest {

    @Test
    fun `vibration preference selects a dedicated immutable channel`() {
        assertThat(PrayerTextNotifier.channelId(true)).isEqualTo(AlarmReceiver.TEXT_CHANNEL_VIBRATE)
        assertThat(PrayerTextNotifier.channelId(false)).isEqualTo(AlarmReceiver.TEXT_CHANNEL_QUIET)
    }
}
