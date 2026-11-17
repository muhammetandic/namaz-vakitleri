package com.anttech.prayertimes.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZoneId

class AndroidAlarmScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
) : AlarmScheduler {

    override fun schedule(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, item.id)
            putExtra(EXTRA_PRAYER_TYPE, item.prayerType.name)
            putExtra(EXTRA_TITLE, item.title)
            putExtra(EXTRA_MESSAGE, item.message)
            putExtra(EXTRA_SOUND_URI, item.soundUri)
            putExtra(EXTRA_IS_PRE_ALARM, item.isPreAlarm)
            putExtra(EXTRA_DELAYED_ALERT, item.isDelayedAlert)
            putExtra(EXTRA_TRIGGER_DATETIME, item.triggerDateTime.toString())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerEpoch = item.triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpoch,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpoch,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Wake device from deep sleep (Doze Mode) to ensure adhan sounds on time
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerEpoch,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerEpoch,
                pendingIntent
            )
        }
    }

    override fun cancel(item: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_IS_PRE_ALARM = "extra_is_pre_alarm"
        const val EXTRA_DELAYED_ALERT = "extra_delayed_alert"
        const val EXTRA_TRIGGER_DATETIME = "extra_trigger_datetime"
    }
}
