package com.anttech.prayertimes.core.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

open class GeocoderHelper(private val context: Context) {

    open fun getCityAndDistrict(latitude: Double, longitude: Double): Pair<String, String?> {
        val geocoder = Geocoder(context, Locale("tr", "TR"))
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.adminArea ?: address.subAdminArea ?: "İstanbul"
                val district = address.subAdminArea ?: address.locality
                city to district
            } else {
                "İstanbul" to null
            }
        } catch (e: Exception) {
            "İstanbul" to null
        }
    }

    /**
     * Elle seçilen şehir/ilçe için enlem-boylam döndürür (ileri geocode).
     * Sonuç bulunamazsa veya hata olursa null döner; çağıran eski konumu korur.
     */
    open fun getCoordinatesForCity(city: String, district: String?): Pair<Double, Double>? {
        return try {
            val geocoder = Geocoder(context, Locale("tr", "TR"))
            val query = if (!district.isNullOrBlank() && district != city) {
                "$district, $city, Türkiye"
            } else {
                "$city, Türkiye"
            }
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].latitude to addresses[0].longitude
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
