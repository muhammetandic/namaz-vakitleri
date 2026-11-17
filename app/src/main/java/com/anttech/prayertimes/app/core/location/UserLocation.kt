package com.anttech.prayertimes.core.location

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val districtName: String? = null
) {
    val displayName: String
        get() = if (!districtName.isNullOrBlank() && districtName != cityName) {
            "$cityName, $districtName"
        } else {
            cityName
        }
}
