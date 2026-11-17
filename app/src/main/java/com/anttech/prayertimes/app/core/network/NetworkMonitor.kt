package com.anttech.prayertimes.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cihazın ağ durumunu sorgular. Akıllı senkronizasyon, büyük indirmeleri
 * yalnızca Wi-Fi'de yapmak için bu arayüzü kullanır.
 */
interface NetworkMonitor {
    fun isWifiAvailable(): Boolean
}

@Singleton
class ConnectivityManagerNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {

    override fun isWifiAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java) ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
