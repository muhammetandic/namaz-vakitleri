package com.anttech.prayertimes.features.settings.data

import android.content.Context
import com.anttech.prayertimes.features.settings.domain.DisplayPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DisplayPreferencesModule {

    @Provides
    @Singleton
    fun provideDisplayPreferencesRepository(
        @ApplicationContext context: Context
    ): DisplayPreferencesRepository {
        return DataStoreDisplayPreferencesRepository(context)
    }
}
