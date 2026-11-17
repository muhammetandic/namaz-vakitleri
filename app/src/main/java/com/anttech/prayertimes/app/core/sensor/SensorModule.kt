package com.anttech.prayertimes.core.sensor

import android.content.Context
import com.anttech.prayertimes.features.qibla.domain.CalculateQiblaAngleUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideCompassSensorManager(@ApplicationContext context: Context): CompassSensorManager {
        return DefaultCompassSensorManager(context)
    }

    @Provides
    @Singleton
    fun provideCalculateQiblaAngleUseCase(): CalculateQiblaAngleUseCase {
        return CalculateQiblaAngleUseCase()
    }
}
