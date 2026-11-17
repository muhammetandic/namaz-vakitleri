package com.anttech.prayertimes.features.times.domain

import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

enum class KerahaType(val turkishTitle: String, val message: String) {
    NONE("Kerahat Vakti Değil", "Namaz kılınabilir vakit"),
    SUNRISE("Sabah Kerahati", "Güneş doğduktan sonraki 45 dakika (kerahet vakti)"),
    MIDDAY("Öğle Kerahati", "Öğle namazından önceki 45 dakika (kerahet vakti)"),
    SUNSET("İkindi Kerahati", "Akşam ezanından önceki 45 dakika (kerahet vakti)")
}

data class KerahaResult(
    val isKeraha: Boolean,
    val type: KerahaType,
    val minutesRemaining: Long = 0
)

class CalculateKerahaStatusUseCase @Inject constructor() {

    operator fun invoke(
        prayerTimes: PrayerTimes,
        currentDateTime: LocalDateTime = LocalDateTime.now()
    ): KerahaResult {
        val today = currentDateTime.toLocalDate()
        val gunesTime = LocalDateTime.of(today, prayerTimes.gunes)
        val ogleTime = LocalDateTime.of(today, prayerTimes.ogle)
        val aksamTime = LocalDateTime.of(today, prayerTimes.aksam)

        val kerahaDurationMinutes = 45L

        // 1. Sunrise Keraha: from Güneş to Güneş + 45 min
        val sunriseKerahaEnd = gunesTime.plusMinutes(kerahaDurationMinutes)
        if (!currentDateTime.isBefore(gunesTime) && currentDateTime.isBefore(sunriseKerahaEnd)) {
            val remaining = Duration.between(currentDateTime, sunriseKerahaEnd).toMinutes()
            return KerahaResult(
                isKeraha = true,
                type = KerahaType.SUNRISE,
                minutesRemaining = remaining
            )
        }

        // 2. Midday (Zeval) Keraha: from Öğle - 45 min to Öğle
        val middayKerahaStart = ogleTime.minusMinutes(kerahaDurationMinutes)
        if (!currentDateTime.isBefore(middayKerahaStart) && currentDateTime.isBefore(ogleTime)) {
            val remaining = Duration.between(currentDateTime, ogleTime).toMinutes()
            return KerahaResult(
                isKeraha = true,
                type = KerahaType.MIDDAY,
                minutesRemaining = remaining
            )
        }

        // 3. Sunset Keraha: from Akşam - 45 min to Akşam
        val sunsetKerahaStart = aksamTime.minusMinutes(kerahaDurationMinutes)
        if (!currentDateTime.isBefore(sunsetKerahaStart) && currentDateTime.isBefore(aksamTime)) {
            val remaining = Duration.between(currentDateTime, aksamTime).toMinutes()
            return KerahaResult(
                isKeraha = true,
                type = KerahaType.SUNSET,
                minutesRemaining = remaining
            )
        }

        return KerahaResult(isKeraha = false, type = KerahaType.NONE, minutesRemaining = 0)
    }
}
