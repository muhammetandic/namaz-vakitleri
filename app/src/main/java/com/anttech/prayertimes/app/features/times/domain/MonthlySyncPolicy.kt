package com.anttech.prayertimes.features.times.domain

/**
 * Akıllı aylık senkronizasyon kararı.
 *
 * Yönerge kuralları:
 * - Ay sonuna 10 gün kala sonraki ayın verisi kontrol edilir.
 * - Bu dönemde indirme yalnızca Wi-Fi ile yapılır.
 * - Wi-Fi yoksa ve ay sonuna 2 gün veya daha az kaldıysa mobil veri denenir.
 * - Sonraki ay zaten önbellekteyse indirme yapılmaz.
 */
class MonthlySyncPolicy @javax.inject.Inject constructor() {

    fun shouldDownloadNextMonth(
        daysRemainingInMonth: Int,
        isNextMonthCached: Boolean,
        isWifiAvailable: Boolean
    ): Boolean {
        if (isNextMonthCached) return false
        if (daysRemainingInMonth > 10) return false
        // Son 10 gün: yalnızca Wi-Fi; son 2 gün: mobil veriye de izin ver.
        return isWifiAvailable || daysRemainingInMonth <= 2
    }
}
