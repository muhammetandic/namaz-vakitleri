package com.anttech.prayertimes.core.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.core.notifications.AlarmScheduler
import com.anttech.prayertimes.core.notifications.AlarmItem
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.features.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Sahur alarmı bildirim aksiyonlarını işler (Durdur, 5 dk Ertele).
 * Sadece sahur alarmı (isPreAlarm=true) için kullanılır.
 */
@AndroidEntryPoint
class SahurAlarmActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    companion object {
        const val ACTION_STOP = "com.anttech.prayertimes.action.SAHUR_STOP"
        const val ACTION_SNOOZE = "com.anttech.prayertimes.action.SAHUR_SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_ORIGINAL_TRIGGER = "extra_original_trigger"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val prayerTypeName = intent.getStringExtra(EXTRA_PRAYER_TYPE)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Sahur Uyandırma"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "İmsak vakti yaklaştı"

        if (alarmId == -1 || action == null) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_STOP -> {
                        // Durdur: Ses servisini durdur, tekrar kurma
                        stopSahurPlayback(context)
                        // Bildirimi kapat
                        cancelNotification(context, alarmId)
                    }
                    ACTION_SNOOZE -> {
                        // Ertele: Mevcut çalmayı durdur, 5 dakika sonra yeni alarm kur
                        stopSahurPlayback(context)
                        cancelNotification(context, alarmId)
                        scheduleSnoozeAlarm(prayerTypeName, title, message)
                    }
                }
                // Widget güncelle
                widgetUpdater.updateAll()
            } catch (exception: Exception) {
                Log.e("SahurAlarmActionReceiver", "Alarm action failed", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun stopSahurPlayback(context: Context) {
        // AdhanPlaybackService'i durdur
        val stopIntent = Intent(context, AdhanPlaybackService::class.java).apply {
            action = AdhanPlaybackService.ACTION_STOP
        }
        context.stopService(stopIntent)

        // Alarm sırasında alınan wake lock'u serbest bırak (ekran kapanabilir).
        SahurWakeLockManager.release()
    }

    private fun cancelNotification(context: Context, alarmId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(alarmId)
    }

    private suspend fun scheduleSnoozeAlarm(
        prayerTypeName: String?,
        title: String,
        message: String
    ) {
        val prayerType = prayerTypeName?.let { name ->
            PrayerType.entries.firstOrNull { it.name == name }
        } ?: PrayerType.IMSAK

        // 5 dakika sonra tetikle
        val snoozeTrigger = LocalDateTime.now().plusMinutes(5)

        alarmScheduler.schedule(
            AlarmItem(
                id = PrayerAlarmScheduler.SAHUR_ALARM_SLOT + 1000,
                triggerDateTime = snoozeTrigger,
                prayerType = prayerType,
                title = title,
                message = message,
                isPreAlarm = true
            )
        )
    }
}
