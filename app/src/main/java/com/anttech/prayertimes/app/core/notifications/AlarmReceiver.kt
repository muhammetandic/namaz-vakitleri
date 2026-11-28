package com.anttech.prayertimes.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.anttech.prayertimes.MainActivity
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.features.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDateTime
import javax.inject.Inject

private const val RECEIVER_TIMEOUT_MS = 10_000L

/**
 * Vakit alarmı alıcısı.
 *
 * Kurallar:
 * - Telefon sesli (RINGER_MODE_NORMAL) ve ses seçili: yalnızca ses çalınır,
 *   metin bildirimi gönderilmez.
 * - Telefon sessiz/titreşimde ya da ses "Sessiz": yalnızca metin bildirimi.
 * - Vakit başına gecikme tanımlıysa ses, gecikme kadar sonra çalınır
 *   (widget yine vakit girişinde tazelenir).
 * - Ses, [AdhanPlaybackService] üzerinden uygulama içi ses seviyesiyle çalınır.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var soundPreferencesRepository: SoundPreferencesRepository

    @Inject
    lateinit var notificationPreferencesRepository: NotificationPreferencesRepository

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var prayerAlertPolicy: PrayerAlertPolicy

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(RECEIVER_TIMEOUT_MS) {
                    deliverAlert(appContext, intent)
                    // Vakit değişti: ana ekran widget'larını yenile.
                    widgetUpdater.updateAll()
                }
            } catch (exception: Exception) {
                Log.e("AlarmReceiver", "Alarm handling failed", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun deliverAlert(context: Context, intent: Intent) {
        val isPreAlarm = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_IS_PRE_ALARM, false)

        // Kullanıcı bildirimleri kapattıysa yalnızca widget tazelenir.
        // Sahur uyandırma alarmı ayrı bir tercih olduğundan bu süzgeçten muaf.
        if (!isPreAlarm && !notificationPreferencesRepository.notificationsEnabled.first()) return

        val prayerType = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_PRAYER_TYPE)
            ?.let { name -> PrayerType.entries.firstOrNull { it.name == name } }

        val sound = if (isPreAlarm) resolveSahurAlarmSound(context, prayerType)
        else resolveSound(context, prayerType)
        val vibrationPreference = notificationPreferencesRepository.vibrationEnabled.first()
        val vibrationEnabled = prayerAlertPolicy.vibrationEnabledFor(sound, vibrationPreference)
        val title = if (isPreAlarm) {
            intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TITLE) ?: "Sahur Uyandırma"
        } else {
            "${prayerType?.turkishName ?: "Namaz"} Vakti"
        }
        val message = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MESSAGE)
            ?: "${prayerType?.turkishName ?: "Vakit"} vakti girdi."

        val mode = prayerAlertPolicy.modeFor(sound)

        // Ses çalınacaksa ve vakit için gecikme tanımlıysa ikinci aşama alarm kur.
        if (mode == PrayerAlertMode.SOUND_ONLY &&
            !isPreAlarm &&
            !intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_DELAYED_ALERT, false)
        ) {
            val delayMinutes = prayerType
                ?.let { soundPreferencesRepository.getDelayMinutesForPrayer(it).first() }
                ?: 0
            if (delayMinutes > 0) {
                alarmScheduler.schedule(
                    AlarmItem(
                        id = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, 1),
                        triggerDateTime = LocalDateTime.now().plusMinutes(delayMinutes.toLong()),
                        prayerType = prayerType ?: PrayerType.OGLE,
                        title = title,
                        message = message,
                        isDelayedAlert = true
                    )
                )
                return
            }
        }

        when (mode) {
            PrayerAlertMode.SOUND_ONLY -> {
                if (isPreAlarm) {
                    playSoundWithFullScreenNotification(
                        context = context,
                        sound = sound,
                        title = title,
                        message = message,
                        alarmId = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, 1),
                        vibrationEnabled = vibrationEnabled,
                        originalTrigger = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_TRIGGER_DATETIME)
                    )
                } else {
                    playSound(
                        context = context,
                        sound = sound,
                        title = title,
                        message = message,
                        alarmId = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, 1),
                        vibrationEnabled = vibrationEnabled
                    )
                }
            }
            PrayerAlertMode.TEXT_ONLY -> postTextNotification(
                context = context,
                alarmId = intent.getIntExtra(AndroidAlarmScheduler.EXTRA_ALARM_ID, 1),
                title = title,
                message = message,
                vibrationEnabled = vibrationEnabled
            )
        }
    }

    private suspend fun playSoundWithFullScreenNotification(
        context: Context,
        sound: SoundOption,
        title: String,
        message: String,
        alarmId: Int,
        vibrationEnabled: Boolean,
        originalTrigger: String?
    ) {
        val uri = playbackUri(context, sound) ?: return
        val volume = soundPreferencesRepository.globalVolume.first()

        // 1. Ses çalma servisini başlat (ön plan servisi)
        val playbackIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            putExtra(AdhanPlaybackService.EXTRA_URI, uri)
            putExtra(AdhanPlaybackService.EXTRA_VOLUME, volume)
            putExtra(AdhanPlaybackService.EXTRA_TITLE, title)
            putExtra(AdhanPlaybackService.EXTRA_MESSAGE, message)
            putExtra(AdhanPlaybackService.EXTRA_ALARM_ID, alarmId)
            putExtra(AdhanPlaybackService.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
        }
        val playbackStarted = runCatching {
            ContextCompat.startForegroundService(context, playbackIntent)
        }.isSuccess
        if (!playbackStarted) {
            postTextNotification(context, alarmId, title, message, vibrationEnabled)
            return
        }

        // Bildirim izni yoksa tam ekran alarm gösterilemez; ses yine çalar.
        if (!canPostNotifications(context)) return

        // 2. CPU'yu açık tut; ekran açma [SahurAlarmActivity] tarafından yapılır.
        SahurWakeLockManager.acquire(context)

        // 3. Aksiyon butonları için PendingIntent'ler
        val stopIntent = Intent(context, SahurAlarmActionReceiver::class.java).apply {
            action = SahurAlarmActionReceiver.ACTION_STOP
            putExtra(SahurAlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(SahurAlarmActionReceiver.EXTRA_PRAYER_TYPE, "IMSAK")
            putExtra(SahurAlarmActionReceiver.EXTRA_TITLE, title)
            putExtra(SahurAlarmActionReceiver.EXTRA_MESSAGE, message)
            putExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER, originalTrigger)
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
            putExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER, originalTrigger)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Full-screen intent (uygulama açma)
        val fullScreenIntent = Intent(context, SahurAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SahurAlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(SahurAlarmActionReceiver.EXTRA_TITLE, title)
            putExtra(SahurAlarmActionReceiver.EXTRA_MESSAGE, message)
            putExtra(SahurAlarmActionReceiver.EXTRA_ORIGINAL_TRIGGER, originalTrigger)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 2000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Titreşim tercihi kanal kimliğine yansır; kanal ayarları sonradan değişmez.
        val channelId = sahurAlarmChannelId(vibrationEnabled)
        createSahurAlarmChannel(context, channelId, vibrationEnabled)

        // 5. Full-screen notification oluştur (BigTextStyle ile genişletilmiş)
        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(message)
        val notification = NotificationCompat.Builder(context, channelId)
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

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId, notification)
    }

    private fun createSahurAlarmChannel(
        context: Context,
        channelId: String,
        vibrationEnabled: Boolean
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) != null) return
        val channel = NotificationChannel(
            channelId,
            if (vibrationEnabled) "Sahur Alarmı" else "Sahur Alarmı (titreşimsiz)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sahur uyandırma alarmı (kilit ekranında gösterilir)"
            enableVibration(vibrationEnabled)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            // Ses servis tarafından ayrı çalınır, bildirim sesi yok
            setSound(null, null)
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean =
        PrayerTextNotifier.canPost(context)

    private suspend fun resolveSahurAlarmSound(context: Context, prayerType: PrayerType?): SoundOption {
        val sahurSound = soundPreferencesRepository.getSahurAlarmSound().first()
        if (sahurSound != null) {
            // Sahur için özel ses seçilmiş, geçerliliğini kontrol et.
            if (sahurSound is SoundOption.BuiltIn &&
                rawSoundResId(sahurSound.resName) == 0
            ) {
                // Sahur sesi bozuksa imsak sesine düş.
            } else {
                return sahurSound
            }
        }
        // Sahur sesi yok veya bozuk → imsak sesini kullan.
        return resolveSound(context, PrayerType.IMSAK)
    }

    private suspend fun resolveSound(context: Context, prayerType: PrayerType?): SoundOption {
        val stored = prayerType
            ?.let { soundPreferencesRepository.getSoundForPrayer(it).first() }
            ?: SoundOption.Silent

        // Kayıtlı ses dosyası uygulamada yoksa (ör. eski sürümden kalan tercih)
        // varsayılan ezana düş; o da yoksa sessiz kabul et.
        if (stored is SoundOption.BuiltIn &&
            rawSoundResId(stored.resName) == 0
        ) {
            val fallback = prayerType?.let { SoundOption.defaultFor(it) } ?: SoundOption.Silent
            return if (fallback is SoundOption.BuiltIn &&
                rawSoundResId(fallback.resName) == 0
            ) {
                SoundOption.Silent
            } else {
                fallback
            }
        }
        return stored
    }

    private suspend fun playSound(
        context: Context,
        sound: SoundOption,
        title: String,
        message: String,
        alarmId: Int,
        vibrationEnabled: Boolean
    ) {
        val uri = playbackUri(context, sound) ?: return
        val volume = soundPreferencesRepository.globalVolume.first()

        val intent = Intent(context, AdhanPlaybackService::class.java).apply {
            putExtra(AdhanPlaybackService.EXTRA_URI, uri)
            putExtra(AdhanPlaybackService.EXTRA_VOLUME, volume)
            putExtra(AdhanPlaybackService.EXTRA_TITLE, title)
            putExtra(AdhanPlaybackService.EXTRA_MESSAGE, message)
            putExtra(AdhanPlaybackService.EXTRA_ALARM_ID, alarmId)
            putExtra(AdhanPlaybackService.EXTRA_VIBRATION_ENABLED, vibrationEnabled)
        }

        val started = runCatching {
            ContextCompat.startForegroundService(context, intent)
        }.isSuccess
        if (!started) {
            // Ön plan servisi başlatılamazsa (ör. sistem kısıtı) sessizce yutma:
            // en azından metin bildirimi göster.
            postTextNotification(context, alarmId, title, message, vibrationEnabled)
        }
    }

    private fun playbackUri(context: Context, sound: SoundOption): String? = when (sound) {
        is SoundOption.BuiltIn -> {
            val resId = rawSoundResId(sound.resName)
            if (resId == 0) null else "android.resource://${context.packageName}/$resId"
        }
        is SoundOption.CustomFile -> sound.uriString
        is SoundOption.Silent, SoundOption.VibrateOnly -> null
    }

    private fun postTextNotification(
        context: Context,
        alarmId: Int,
        title: String,
        message: String,
        vibrationEnabled: Boolean
    ) {
        PrayerTextNotifier.post(context, alarmId, title, message, vibrationEnabled)
    }

    private fun sahurAlarmChannelId(vibrationEnabled: Boolean): String =
        if (vibrationEnabled) SAHUR_ALARM_CHANNEL_VIBRATE else SAHUR_ALARM_CHANNEL_QUIET

    companion object {
        const val TEXT_CHANNEL_VIBRATE = PrayerTextNotifier.CHANNEL_VIBRATE
        const val TEXT_CHANNEL_QUIET = PrayerTextNotifier.CHANNEL_QUIET
        const val SAHUR_ALARM_CHANNEL_VIBRATE = "namaz_sahur_alarm_v1"
        const val SAHUR_ALARM_CHANNEL_QUIET = "namaz_sahur_alarm_q1"
        // Uygulama açılışında da temizlenir (bkz. NamazVaktiApplication).
        const val LEGACY_CHANNEL_PREFIX = "namaz_vakti_"
    }
}
