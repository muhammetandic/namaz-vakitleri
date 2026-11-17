package com.anttech.prayertimes.core.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.settings.data.DataStoreSoundPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.times.domain.PrayerType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmReceiverTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var soundPreferences: DataStoreSoundPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        soundPreferences = DataStoreSoundPreferencesRepository(context)
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
    }

    @Test
    fun postsPrayerNotificationWithSavedSound() = runBlocking {
        soundPreferences.saveSoundForPrayer(PrayerType.AKSAM, SoundOption.EZAN_ANVER_DUMAN)

        sendAlarmBroadcast(PrayerType.AKSAM)

        val record = awaitNotification()
        assertThat(record.notification.channelId).isNotEmpty()
        val channel = notificationManager.getNotificationChannel(record.notification.channelId)
        assertThat(channel.sound.toString())
            .isEqualTo("android.resource://${context.packageName}/raw/ezan2")
    }

    @Test
    fun fallsBackToDefaultEzanWhenSavedSoundResourceMissing() = runBlocking {
        soundPreferences.saveSoundForPrayer(
            PrayerType.AKSAM,
            SoundOption.BuiltIn("eski_ses", "Eski Ses", "adhan_saba")
        )

        sendAlarmBroadcast(PrayerType.AKSAM)

        val record = awaitNotification()
        val channel = notificationManager.getNotificationChannel(record.notification.channelId)
        assertThat(channel.sound.toString())
            .isEqualTo("android.resource://${context.packageName}/raw/ezan2")
    }

    private fun sendAlarmBroadcast(prayerType: PrayerType) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, 9876)
            putExtra(AndroidAlarmScheduler.EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(AndroidAlarmScheduler.EXTRA_TITLE, "${prayerType.turkishName} Vakti")
            putExtra(AndroidAlarmScheduler.EXTRA_MESSAGE, "${prayerType.turkishName} vakti girdi.")
        }
        context.sendBroadcast(intent)
    }

    private suspend fun awaitNotification(): android.service.notification.StatusBarNotification {
        repeat(100) {
            val match = notificationManager.activeNotifications
                .firstOrNull { it.packageName == context.packageName && it.id == 9876 }
            if (match != null) return match
            kotlinx.coroutines.delay(100)
        }
        throw AssertionError("Vakit bildirimi gönderilmedi")
    }
}
