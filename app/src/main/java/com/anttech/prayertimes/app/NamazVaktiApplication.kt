package com.anttech.prayertimes

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.anttech.prayertimes.core.notifications.AlarmReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NamazVaktiApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Eski sürümlerde her ses için açılan bildirim kanalları artık kullanılmıyor.
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notificationChannels
            ?.filter { it.id.startsWith(AlarmReceiver.LEGACY_CHANNEL_PREFIX) }
            ?.forEach { manager.deleteNotificationChannel(it.id) }
    }
}
