package com.anttech.prayertimes.features.times.domain

import com.anttech.prayertimes.core.location.SelectedCityStore
import com.anttech.prayertimes.core.network.NetworkMonitor
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Akıllı aylık senkronizasyon orkestrasyonu:
 * - İçinde bulunulan ayın verisi eksikse (uygulama uzun süre açılmadıysa) önce o tamamlanır.
 * - Ay sonuna 10 gün kala sonraki ayın verisi kontrol edilir ([MonthlySyncPolicy]).
 * - Sonraki ayın verisi yalnızca Wi-Fi varsa indirilir.
 * - Wi-Fi yoksa ve ay sonuna 2 gün veya daha az kaldıysa mobil veri denenir.
 * - Sonraki ay zaten önbellekteyse indirme yapılmaz.
 * - Her çalıştırmada vakit alarmları yeniden kurulur; böylece uygulama hiç
 *   açılmasa ve reboot olmasa bile alarm penceresi (62 gün) sürekli ileri kayar.
 */
class SmartSyncCoordinator @Inject constructor(
    private val repository: TimesRepository,
    private val selectedCityStore: SelectedCityStore,
    private val networkMonitor: NetworkMonitor,
    private val monthlySyncPolicy: MonthlySyncPolicy,
    private val prayerAlarmScheduler: PrayerAlarmScheduler
) {

    sealed interface Outcome {
        /** Sonraki ay indirildi ve vakit alarmları yeniden kuruldu. */
        data object Synced : Outcome

        /** Kural gereği indirme yapılmadı; alarm penceresi yine de yenilendi. */
        data object Skipped : Outcome

        /** İndirme denendi ama başarısız oldu. */
        data object Failed : Outcome

        /** Kayıtlı şehir yok; işlem yapılmadı. */
        data object NoCity : Outcome
    }

    suspend fun syncNextMonthIfDue(): Outcome {
        // Elle seçilmiş şehir yoksa GPS modundaki son kullanılan şehri (Room) dene.
        val city = selectedCityStore.getCity() ?: repository.getCachedCity()
            ?: return Outcome.NoCity

        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today).toString()

        // Uzun süre açılmayan uygulamada içinde bulunulan ayın verisi eksik kalır;
        // widget ve alarmlar bu veriye dayanır, önce onu tamamla.
        if (!repository.hasMonthlyPrayerTimes(city, currentMonth)) {
            if (repository.syncMonthlyPrayerTimesFor(city, currentMonth).isFailure) {
                return Outcome.Failed
            }
        }

        val daysRemainingInMonth = today.lengthOfMonth() - today.dayOfMonth
        val nextMonth = YearMonth.from(today).plusMonths(1).toString() // "YYYY-MM"

        val isNextMonthCached = repository.hasMonthlyPrayerTimes(city, nextMonth)
        val isWifiAvailable = networkMonitor.isWifiAvailable()

        if (!monthlySyncPolicy.shouldDownloadNextMonth(
                daysRemainingInMonth = daysRemainingInMonth,
                isNextMonthCached = isNextMonthCached,
                isWifiAvailable = isWifiAvailable
            )
        ) {
            // Veri hazır: alarm penceresini her gün ileri kaydır.
            prayerAlarmScheduler.scheduleUpcoming(city)
            return Outcome.Skipped
        }

        val syncResult = repository.syncMonthlyPrayerTimesFor(city, nextMonth)
        if (syncResult.isFailure) {
            return Outcome.Failed
        }

        // Yeni ayın verisi geldi: alarmları hemen yeniden kur.
        prayerAlarmScheduler.scheduleUpcoming(city)
        return Outcome.Synced
    }
}
