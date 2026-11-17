package com.anttech.prayertimes.core.notifications

import android.content.Context
import com.anttech.prayertimes.features.settings.data.DataStoreNotificationPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationSettingsModule {

    @Provides
    @Singleton
    fun provideNotificationPreferencesRepository(
        @ApplicationContext context: Context
    ): NotificationPreferencesRepository {
        return DataStoreNotificationPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideTestNotificationSender(
        sender: AndroidTestNotificationSender
    ): TestNotificationSender {
        return sender
    }
}
