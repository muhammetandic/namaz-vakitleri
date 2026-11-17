package com.anttech.prayertimes.core.location

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideGeocoderHelper(@ApplicationContext context: Context): GeocoderHelper {
        return GeocoderHelper(context)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        @ApplicationContext context: Context,
        fusedLocationProviderClient: FusedLocationProviderClient,
        geocoderHelper: GeocoderHelper
    ): LocationTracker {
        return DefaultLocationTracker(context, fusedLocationProviderClient, geocoderHelper)
    }

    @Provides
    @Singleton
    fun provideSelectedCityStore(@ApplicationContext context: Context): SelectedCityStore {
        return DataStoreSelectedCityStore(context)
    }
}
