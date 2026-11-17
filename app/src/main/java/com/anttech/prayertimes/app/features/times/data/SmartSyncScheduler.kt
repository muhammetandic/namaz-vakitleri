package com.anttech.prayertimes.features.times.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Akıllı senkronizasyon kontrolünü günlük WorkManager işi olarak planlar.
 * KEEP politikası sayesinde tekrar açılışta mevcut plan ezilmez.
 */
@Singleton
class SmartSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleDailyCheck() {
        val request = PeriodicWorkRequestBuilder<SmartSyncWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "smart_monthly_sync"
    }
}
