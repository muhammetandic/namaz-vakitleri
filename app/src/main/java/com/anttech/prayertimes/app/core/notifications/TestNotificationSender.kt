package com.anttech.prayertimes.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.anttech.prayertimes.MainActivity
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ayarlar ekranındaki "Test bildirimi gönder" butonu için anlık bildirim gönderir.
 * Android 13+ üzerinde bildirim izni yoksa sessizce hiçbir şey yapmaz.
 */
interface TestNotificationSender {
    fun send()
    fun sendSahurTest()
}

@Singleton
class AndroidTestNotificationSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundPreferencesRepository: SoundPreferencesRepository
) : TestNotificationSender {

    override fun send() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_CHANNEL_ID,
                "Test Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Bildirimlerin çalıştığını denetlemek için gönderilen deneme bildirimi"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Test bildirimi")
            .setContentText("Vakit bildirimleri çalışıyor. Vakit girdiğinde sizi uyaracağız.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TEST_NOTIFICATION_ID, notification)
    }

    override fun sendSahurTest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Test, gerçek sahur kanalının titreşim ayarını değiştirmemelidir.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_SAHUR_CHANNEL_ID,
                "Sahur Alarmı (Test)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sahur alarmı test bildirimi"
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmId = TEST_SAHUR_CHANNEL_ID.hashCode()
        val title = "Sahur Uyandırma (Test)"
        val message = "İmsak vakti yaklaştı — test alarmı"

        // CPU'yu açık tut; ekran açma Activity tarafından yapılır.
        SahurWakeLockManager.acquire(context)

        // Bildirimi göster (ses olmadan test için)
        showSahurNotification(context, alarmId, title, message)
    }

    private fun showSahurNotification(
        context: Context,
        alarmId: Int,
        title: String,
        message: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Aksiyon butonları
        val stopIntent = Intent(context, SahurAlarmActionReceiver::class.java).apply {
            action = SahurAlarmActionReceiver.ACTION_STOP
            putExtra(SahurAlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(SahurAlarmActionReceiver.EXTRA_PRAYER_TYPE, "IMSAK")
            putExtra(SahurAlarmActionReceiver.EXTRA_TITLE, title)
            putExtra(SahurAlarmActionReceiver.EXTRA_MESSAGE, message)
            putExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER, java.time.LocalDateTime.now().toString())
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SahurAlarmActionReceiver::class.java).apply {
            action = SahurAlarmActionReceiver.ACTION_SNOOZE
            putExtra(SahurAlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(SahurAlarmActionReceiver.EXTRA_PRAYER_TYPE, "IMSAK")
            putExtra(SahurAlarmActionReceiver.EXTRA_TITLE, title)
            putExtra(SahurAlarmActionReceiver.EXTRA_MESSAGE, message)
            putExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER, java.time.LocalDateTime.now().toString())
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full-screen intent
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 2000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(message)
        val notification = NotificationCompat.Builder(context, TEST_SAHUR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Durdur", stopPendingIntent)
            .addAction(android.R.drawable.ic_media_next, "5 dk Ertele", snoozePendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(alarmId, notification)
    }

    private fun playbackUri(context: Context, sound: SoundOption): String? = when (sound) {
        is SoundOption.BuiltIn -> {
            val resId = context.resources.getIdentifier(sound.resName, "raw", context.packageName)
            if (resId != 0) "android.resource://${context.packageName}/$resId" else null
        }
        is SoundOption.CustomFile -> sound.uriString
        is SoundOption.Silent -> null
        else -> null
    }

    companion object {
        const val TEST_CHANNEL_ID = "namaz_test_v1"
        const val TEST_SAHUR_CHANNEL_ID = "namaz_sahur_test_v1"
        const val TEST_NOTIFICATION_ID = 4242
    }
}
