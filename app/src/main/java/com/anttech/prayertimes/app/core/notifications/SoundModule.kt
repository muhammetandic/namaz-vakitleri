package com.anttech.prayertimes.core.notifications

import android.content.Context
import com.anttech.prayertimes.features.settings.data.DataStoreFastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.data.DataStoreSoundPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.AudioPlayerManager
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SoundModule {

    @Provides
    @Singleton
    fun provideSoundPreferencesRepository(@ApplicationContext context: Context): SoundPreferencesRepository {
        return DataStoreSoundPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideFastingAlarmPreferencesRepository(
        @ApplicationContext context: Context
    ): FastingAlarmPreferencesRepository {
        return DataStoreFastingAlarmPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideAudioPlayerManager(@ApplicationContext context: Context): AudioPlayerManager {
        return Media3AudioPlayerManager(context)
    }

    @Provides
    @Singleton
    fun providePrayerAlertPolicy(@ApplicationContext context: Context): PrayerAlertPolicy {
        return PrayerAlertPolicy(context)
    }
}
