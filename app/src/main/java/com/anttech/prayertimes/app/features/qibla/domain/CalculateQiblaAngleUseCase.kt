package com.anttech.prayertimes.features.qibla.domain

import kotlin.math.*

class CalculateQiblaAngleUseCase {

    companion object {
        // Kabe Koordinatları (Mecca - Kaaba)
        const val KAABA_LATITUDE = 21.422487
        const val KAABA_LONGITUDE = 39.826206
        private const val ALIGNMENT_TOLERANCE_DEGREES = 3.0f
    }

    /**
     * Kullanıcının enlem ve boylamına göre Kabe'nin yön açısını (Kuzeye göre 0-360 derece) hesaplar.
     */
    fun calculateQiblaBearing(userLatitude: Double, userLongitude: Double): Float {
        val lat1Rad = Math.toRadians(userLatitude)
        val lat2Rad = Math.toRadians(KAABA_LATITUDE)
        val deltaLngRad = Math.toRadians(KAABA_LONGITUDE - userLongitude)

        val y = sin(deltaLngRad) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(deltaLngRad)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0

        return bearing.toFloat()
    }

    /**
     * Cihazın tutulduğu açıya (Azimuth) göre pusula ibresinin kaç derece dönmesi gerektiğini hesaplar.
     */
    fun calculateNeedleAngle(qiblaBearing: Float, currentAzimuth: Float): Float {
        val angle = (qiblaBearing - currentAzimuth + 360f) % 360f
        return angle
    }

    /**
     * Cihazın Kabe yönüne dönük olup olmadığını kontrol eder (±3 derece tolerans).
     */
    fun isAlignedWithQibla(qiblaBearing: Float, currentAzimuth: Float): Boolean {
        val diff = abs((qiblaBearing - currentAzimuth + 180f + 360f) % 360f - 180f)
        return diff <= ALIGNMENT_TOLERANCE_DEGREES
    }
}
