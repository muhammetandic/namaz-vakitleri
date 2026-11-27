package com.anttech.prayertimes.features.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Widget ana ekrana eklendiğinde veya son widget kaldırıldığında alarm
 * ihtiyacını yeniden değerlendirir:
 * - Bildirimler kapalı + widget var  -> alarmlar yalnızca widget tazelemesi için kurulur.
 * - Bildirimler kapalı + widget yok  -> bekleyen alarmlar iptal edilir.
 *
 * İş WorkManager'a devredilir; receiver döndükten sonra süreç öldürülse bile
 * yeniden planlama tamamlanır.
 */
internal fun rescheduleWidgetAlarms(context: Context) {
    val request = OneTimeWorkRequestBuilder<WidgetAlarmSyncWorker>().build()
    WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
        WidgetAlarmSyncWorker.UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request
    )
}
