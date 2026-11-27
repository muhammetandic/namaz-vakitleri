package com.anttech.prayertimes.core.notifications

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Debug derlemesinde gerçek test bildirimi göndericisini bağlar. */
@Module
@InstallIn(SingletonComponent::class)
object DebugTestNotificationModule {

    @Provides
    @Singleton
    fun provideTestNotificationSender(
        sender: DebugTestNotificationSender
    ): TestNotificationSender = sender
}
