package com.anttech.prayertimes.features.times.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anttech.prayertimes.features.times.domain.SmartSyncCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Akıllı aylık senkronizasyon (yönerge): günlük WorkManager işi.
 * Karar mantığı [SmartSyncCoordinator] içinde; yeni ay indirildiğinde
 * vakit alarmları da yeniden kurulur.
 */
@HiltWorker
class SmartSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: SmartSyncCoordinator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (coordinator.syncNextMonthIfDue()) {
            SmartSyncCoordinator.Outcome.Synced,
            SmartSyncCoordinator.Outcome.Skipped,
            SmartSyncCoordinator.Outcome.NoCity -> Result.success()

            SmartSyncCoordinator.Outcome.Failed -> Result.retry()
        }
    }
}
