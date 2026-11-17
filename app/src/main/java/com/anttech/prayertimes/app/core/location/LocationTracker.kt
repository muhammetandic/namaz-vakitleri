package com.anttech.prayertimes.core.location

interface LocationTracker {
    suspend fun getCurrentLocation(): UserLocation?
}
