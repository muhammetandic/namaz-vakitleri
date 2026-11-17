package com.anttech.prayertimes.core.notifications

import android.content.Context
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import com.anttech.prayertimes.features.times.domain.TimesRepository
import com.anttech.prayertimes.features.widget.WidgetUpdater
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler {
        return AndroidAlarmScheduler(context)
    }

    @Provides
    @Singleton
    fun provideLastCityStore(@ApplicationContext context: Context): LastCityStore {
        return DataStoreLastCityStore(context)
    }

    @Provides
    @Singleton
    fun providePrayerAlarmScheduler(
        repository: TimesRepository,
        alarmScheduler: AlarmScheduler,
        lastCityStore: LastCityStore,
        notificationPreferences: NotificationPreferencesRepository,
        fastingAlarmPreferences: FastingAlarmPreferencesRepository,
        widgetUpdater: WidgetUpdater
    ): PrayerAlarmScheduler {
        // Bildirimler kapalı ama ana ekranda widget varsa alarmlar korunur;
        // böylece widget vakit değişiminde uygulama kapalıyken de tazelenir.
        return PrayerAlarmScheduler(
            repository = repository,
            alarmScheduler = alarmScheduler,
            lastCityStore = lastCityStore,
            notificationPreferences = notificationPreferences,
            fastingAlarmPreferences = fastingAlarmPreferences,
            hasActiveWidgets = { widgetUpdater.hasWidgets() }
        )
    }
}
