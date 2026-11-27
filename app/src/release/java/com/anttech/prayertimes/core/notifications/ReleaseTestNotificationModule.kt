package com.anttech.prayertimes.core.notifications

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Release derlemesinde test bildirimi göndericisini no-op olarak bağlar. */
@Module
@InstallIn(SingletonComponent::class)
object ReleaseTestNotificationModule {

    @Provides
    @Singleton
    fun provideTestNotificationSender(
        sender: NoOpTestNotificationSender
    ): TestNotificationSender = sender
}
