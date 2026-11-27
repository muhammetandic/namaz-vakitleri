package com.anttech.prayertimes.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.anttech.prayertimes.core.common.IoDispatcher
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

class DefaultLocationTracker @Inject constructor(
    private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val geocoderHelper: GeocoderHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LocationTracker {

    private val fineLocationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val coarseLocationPermission = android.Manifest.permission.ACCESS_COARSE_LOCATION

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): UserLocation? = withContext(ioDispatcher) {
        val hasFinePermission = context.checkSelfPermission(fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = context.checkSelfPermission(coarseLocationPermission) == PackageManager.PERMISSION_GRANTED

        if (!hasFinePermission && !hasCoarsePermission) {
            // No location permission - return null (will use default city)
            return@withContext null
        }

        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { awaitLocation() }
            ?: return@withContext null

        // Geocoder ağ/disk erişimi yapar; IO dispatcher üzerinde çalışır.
        val (city, district) = geocoderHelper.getCityAndDistrict(
            location.latitude,
            location.longitude
        )
        UserLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            cityName = city,
            districtName = district
        )
    }

    private suspend fun awaitLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()

        fun complete(location: Location?) {
            if (continuation.isActive) {
                continuation.resume(location)
            }
        }

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null &&
                    location.time >= System.currentTimeMillis() - MAX_CACHED_LOCATION_AGE_MS
                ) {
                    complete(location)
                } else {
                    // Cached location is missing or stale: request a fresh fix.
                    fusedLocationProviderClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { freshLocation ->
                        complete(freshLocation)
                    }.addOnFailureListener {
                        complete(null)
                    }
                }
            }
            .addOnFailureListener {
                complete(null)
            }

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }

    private companion object {
        const val MAX_CACHED_LOCATION_AGE_MS = 60_000L
        const val LOCATION_TIMEOUT_MS = 15_000L
    }
}
