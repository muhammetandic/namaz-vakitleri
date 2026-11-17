package com.anttech.prayertimes.core.notifications

import android.Manifest
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

/** Vakit metin bildirimlerini ve titreşime göre ayrılan kanalları yönetir. */
internal object PrayerTextNotifier {
    const val CHANNEL_VIBRATE = "namaz_text_v1"
    const val CHANNEL_QUIET = "namaz_text_q1"

    private const val LEGACY_CHANNEL_PREFIX = "namaz_vakti_"

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun post(
        context: Context,
        alarmId: Int,
        title: String,
        message: String,
        vibrationEnabled: Boolean
    ) {
        if (!canPost(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = channelId(vibrationEnabled)
        createTextChannel(notificationManager, channelId, vibrationEnabled)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(alarmId, notification)
    }

    internal fun channelId(vibrationEnabled: Boolean): String =
        if (vibrationEnabled) CHANNEL_VIBRATE else CHANNEL_QUIET

    private fun createTextChannel(
        notificationManager: NotificationManager,
        channelId: String,
        vibrationEnabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Eski sürümlerde her ses için ayrı kanal açılıyordu; artık ses uygulama
        // içi seviyeyle çalındığı için o kanalları temizle.
        notificationManager.notificationChannels
            .filter { it.id.startsWith(LEGACY_CHANNEL_PREFIX) }
            .forEach { notificationManager.deleteNotificationChannel(it.id) }

        if (notificationManager.getNotificationChannel(channelId) != null) return

        val channel = NotificationChannel(
            channelId,
            if (vibrationEnabled) "Vakit Bildirimleri" else "Vakit Bildirimleri (titreşimsiz)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sessiz/titreşim modunda gösterilen metin vakit bildirimleri"
            setSound(null, null)
            enableVibration(vibrationEnabled)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
