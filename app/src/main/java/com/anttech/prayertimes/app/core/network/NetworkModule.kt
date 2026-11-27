package com.anttech.prayertimes.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import android.content.pm.ApplicationInfo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ALADHAN_BASE_URL = "https://api.aladhan.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        // Ağ günlükleri yalnızca debuggable derlemede ve yalnızca özet düzeyde.
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideAladhanApiService(okHttpClient: OkHttpClient): AladhanApiService {
        return Retrofit.Builder()
            .baseUrl(ALADHAN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return ConnectivityManagerNetworkMonitor(context)
    }
}
