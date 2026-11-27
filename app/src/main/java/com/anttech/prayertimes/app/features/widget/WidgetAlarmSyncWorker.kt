package com.anttech.prayertimes.features.widget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Widget eklendiğinde/kaldırıldığında vakit alarmlarını yeniden değerlendirir.
 *
 * BroadcastReceiver içinden serbest coroutine başlatmak yerine WorkManager
 * kullanılır: süreç öldürülse bile iş yeniden denenir.
 */
@HiltWorker
class WidgetAlarmSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prayerAlarmScheduler: PrayerAlarmScheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            prayerAlarmScheduler.rescheduleSaved()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "widget_alarm_sync"
    }
}
