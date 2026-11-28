package com.anttech.prayertimes.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import com.anttech.prayertimes.features.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val RECEIVER_TIMEOUT_MS = 10_000L

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prayerAlarmScheduler: PrayerAlarmScheduler

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(context: Context, intent: Intent) {
        // Yeniden başlatma ve uygulama güncellemesi: AlarmManager alarmları
        // sıfırlanabilir, bu yüzden ikisinde de vakitleri yeniden kur.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        // AlarmManager alarmları yeniden başlatmada silinir; son şehirden yeniden kur.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(RECEIVER_TIMEOUT_MS) {
                    prayerAlarmScheduler.rescheduleSaved()
                    // Widget'lar da yeniden başlatma sonrası taze veriyle çizilsin.
                    widgetUpdater.updateAll()
                }
            } catch (exception: Exception) {
                Log.e("BootCompletedReceiver", "Alarm rescheduling failed", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
