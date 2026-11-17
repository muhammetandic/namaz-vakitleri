package com.anttech.prayertimes.features.widget

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget ana ekrana eklendiğinde veya son widget kaldırıldığında alarm
 * ihtiyacını yeniden değerlendirir:
 * - Bildirimler kapalı + widget var  -> alarmlar yalnızca widget tazelemesi için kurulur.
 * - Bildirimler kapalı + widget yok  -> bekleyen alarmlar iptal edilir.
 */
internal fun rescheduleWidgetAlarms(context: Context) {
    val appContext = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runCatching {
            EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
                .prayerAlarmScheduler()
                .rescheduleSaved()
        }
    }
}
