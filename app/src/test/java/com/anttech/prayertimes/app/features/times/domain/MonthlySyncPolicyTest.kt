package com.anttech.prayertimes.features.times.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Akıllı senkronizasyon kuralı (yönerge):
 * - Ay sonuna 10 gün kala sonraki ayın verisi kontrol edilir.
 * - Bu dönemde indirme yalnızca Wi-Fi ile yapılır.
 * - Wi-Fi yoksa ve ay sonuna 2 gün veya daha az kaldıysa mobil veri denenir.
 * - Sonraki ay zaten önbellekteyse indirme yapılmaz.
 */
class MonthlySyncPolicyTest {

    private val policy = MonthlySyncPolicy()

    @Test
    fun `more than 10 days remaining never downloads even on wifi`() {
        assertThat(policy.shouldDownloadNextMonth(
            daysRemainingInMonth = 11,
            isNextMonthCached = false,
            isWifiAvailable = true
        )).isFalse()
    }

    @Test
    fun `10 days remaining downloads when wifi is available`() {
        assertThat(policy.shouldDownloadNextMonth(
            daysRemainingInMonth = 10,
            isNextMonthCached = false,
            isWifiAvailable = true
        )).isTrue()
    }

    @Test
    fun `10 days remaining does not download without wifi`() {
        assertThat(policy.shouldDownloadNextMonth(
            daysRemainingInMonth = 10,
            isNextMonthCached = false,
            isWifiAvailable = false
        )).isFalse()
    }

    @Test
    fun `2 days remaining downloads over mobile data when wifi is absent`() {
        assertThat(policy.shouldDownloadNextMonth(
            daysRemainingInMonth = 2,
            isNextMonthCached = false,
            isWifiAvailable = false
        )).isTrue()
    }

    @Test
    fun `last day of month downloads over mobile data when wifi is absent`() {
        assertThat(policy.shouldDownloadNextMonth(
            daysRemainingInMonth = 0,
            isNextMonthCached = false,
            isWifiAvailable = false
        )).isTrue()
    }

    @Test
    fun `never downloads when next month is already cached`() {
        assertThat(policy.shouldDownloadNextMonth(
            daysRemainingInMonth = 2,
            isNextMonthCached = true,
            isWifiAvailable = false
        )).isFalse()
    }
}
