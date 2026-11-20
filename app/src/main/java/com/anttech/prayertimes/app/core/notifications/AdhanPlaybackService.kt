package com.anttech.prayertimes.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Vakit sesini (ezan/melodi/özel MP3) uygulama içi ses seviyesiyle çalar.
 *
 * Neden servis: alarm tetiklendiğinde uygulama süreci kapalı olabilir. Sesin
 * kesilmemesi için `mediaPlayback` türünde ön plan servisi kullanılır ve
 * çalma süresince küçük, sessiz bir "çalıyor" bildirimi gösterilir.
 *
 * Ses seviyesi sistem bildirim/alarm sesinden bağımsızdır: kullanıcı
 * Ayarlar > Ezan ve Sesler ekranındaki kaydırıcı ile belirler. Cihaz zil
 * modu kontrolü [PrayerAlertPolicy] tarafında yapılır; bu servis yalnızca
 * çalma anında devreye girer.
 */
class AdhanPlaybackService : Service() {

    private var player: ExoPlayer? = null
    private var playbackIntent: Intent? = null
    private var volumeChangeReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            stopSelf()
            return START_NOT_STICKY
        }

        val uri = intent?.getStringExtra(EXTRA_URI)
        if (uri.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Namaz Vakti"
        val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.8f).coerceIn(0f, 1f)
        playbackIntent = intent

        startForeground(NOTIFICATION_ID, buildNotification(title))
        play(uri, volume)

        return START_NOT_STICKY
    }

    private fun play(uriString: String, volume: Float) {
        stopPlayback()

        val newPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                    .build(),
                // USAGE_ALARM için otomatik odak yönetimi desteklenmez; açılırsa
                // IllegalArgumentException ile servis çöker.
                /* handleAudioFocus = */ false
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player = newPlayer
        newPlayer.volume = Media3AudioPlayerManager.perceptualVolume(volume)
        newPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopPlayback()
                    stopSelf()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val sourceIntent = playbackIntent ?: return
                val title = sourceIntent.getStringExtra(EXTRA_TITLE) ?: "Namaz Vakti"
                stopPlayback()
                // SOUND_ONLY modunda metin bildirimi yoktur; ses hatasını görünür kıl.
                PrayerTextNotifier.post(
                    context = this@AdhanPlaybackService,
                    alarmId = AdhanPlaybackFallback.notificationId(sourceIntent, NOTIFICATION_ID),
                    title = title,
                    message = AdhanPlaybackFallback.message(sourceIntent),
                    vibrationEnabled = AdhanPlaybackFallback.vibrationEnabled(sourceIntent)
                )
                stopSelf()
            }
        })
        newPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
        newPlayer.prepare()
        newPlayer.play()
        registerVolumeChangeReceiver()
    }

    private fun stopPlayback() {
        unregisterVolumeChangeReceiver()
        player?.stop()
        player?.release()
        player = null
    }

    private fun registerVolumeChangeReceiver() {
        if (volumeChangeReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val prevVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                    val newVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                    if (prevVolume != -1 && newVolume != -1 && prevVolume != newVolume) {
                        stopPlayback()
                        stopSelf()
                    }
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        try {
            ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            volumeChangeReceiver = receiver
        } catch (_: Exception) {}
    }

    private fun unregisterVolumeChangeReceiver() {
        volumeChangeReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
            volumeChangeReceiver = null
        }
    }

    private fun buildNotification(title: String): android.app.Notification {
        createChannel()

        val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, com.anttech.prayertimes.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Ses çalıyor")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Durdur", stopPendingIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vakit Sesi Çalma",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Vakit sesi çalarken görünen sessiz bildirim"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.anttech.prayertimes.action.STOP_ADHAN"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_VIBRATION_ENABLED = "extra_vibration_enabled"
        private const val CHANNEL_ID = "adhan_playback_v1"
        private const val NOTIFICATION_ID = 4712
    }
}
