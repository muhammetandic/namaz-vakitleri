package com.anttech.prayertimes.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DefaultLocationTracker(
    private val context: Context,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val geocoderHelper: GeocoderHelper
) : LocationTracker {

    private val fineLocationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val coarseLocationPermission = android.Manifest.permission.ACCESS_COARSE_LOCATION

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): UserLocation? {
        // Check if we have location permissions
        val hasFinePermission = context.checkSelfPermission(fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = context.checkSelfPermission(coarseLocationPermission) == PackageManager.PERMISSION_GRANTED
        
        if (!hasFinePermission && !hasCoarsePermission) {
            // No location permission - return null (will use default city)
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null &&
                        location.time >= System.currentTimeMillis() - MAX_CACHED_LOCATION_AGE_MS
                    ) {
                        val (city, district) = geocoderHelper.getCityAndDistrict(
                            location.latitude,
                            location.longitude
                        )
                        if (continuation.isActive) {
                            continuation.resume(
                                UserLocation(
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    cityName = city,
                                    districtName = district
                                )
                            )
                        }
                    } else {
                        // Cached location is missing or stale: request a fresh fix.
                        fusedLocationProviderClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.token
                        ).addOnSuccessListener { freshLocation ->
                            if (continuation.isActive) {
                                if (freshLocation != null) {
                                    val (freshCity, freshDistrict) = geocoderHelper.getCityAndDistrict(
                                        freshLocation.latitude,
                                        freshLocation.longitude
                                    )
                                    continuation.resume(
                                        UserLocation(
                                            latitude = freshLocation.latitude,
                                            longitude = freshLocation.longitude,
                                            cityName = freshCity,
                                            districtName = freshDistrict
                                        )
                                    )
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        }.addOnFailureListener {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    private companion object {
        const val MAX_CACHED_LOCATION_AGE_MS = 60_000L
    }
}
