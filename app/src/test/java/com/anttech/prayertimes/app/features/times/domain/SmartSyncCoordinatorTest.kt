package com.anttech.prayertimes.features.times.domain

import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.core.location.SelectedCityStore
import com.anttech.prayertimes.core.network.NetworkMonitor
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Akıllı senkronizasyon orkestrasyonu:
 * - İçinde bulunulan ayın verisi eksikse her koşulda tamamlanmalı.
 * - İndirme yapılmasa bile alarm penceresi her çalıştırmada ileri kaydırılmalı.
 * - İndirme başarısızsa alarmlara dokunulmamalı.
 */
class SmartSyncCoordinatorTest {

    private val repository: TimesRepository = mockk()
    private val selectedCityStore: SelectedCityStore = mockk()
    private val networkMonitor: NetworkMonitor = mockk()
    private val monthlySyncPolicy: MonthlySyncPolicy = mockk()
    private val prayerAlarmScheduler: PrayerAlarmScheduler = mockk(relaxed = true)
    private lateinit var coordinator: SmartSyncCoordinator

    private val currentMonth = YearMonth.from(LocalDate.now()).toString()
    private val nextMonth = YearMonth.from(LocalDate.now()).plusMonths(1).toString()

    @Before
    fun setUp() {
        coordinator = SmartSyncCoordinator(
            repository = repository,
            selectedCityStore = selectedCityStore,
            networkMonitor = networkMonitor,
            monthlySyncPolicy = monthlySyncPolicy,
            prayerAlarmScheduler = prayerAlarmScheduler
        )
    }

    @Test
    fun `successful download reschedules upcoming alarms for the city`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "İstanbul"
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", currentMonth) } returns true
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", nextMonth) } returns false
        every { networkMonitor.isWifiAvailable() } returns true
        every { monthlySyncPolicy.shouldDownloadNextMonth(any(), any(), any()) } returns true
        coEvery { repository.syncMonthlyPrayerTimesFor("İstanbul", nextMonth) } returns Result.success(Unit)

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.Synced)
        coVerify(exactly = 1) { prayerAlarmScheduler.scheduleUpcoming("İstanbul") }
    }

    @Test
    fun `city resolved from room cache also reschedules alarms after download`() = runTest {
        coEvery { selectedCityStore.getCity() } returns null
        coEvery { repository.getCachedCity() } returns "Artvin, Hopa"
        coEvery { repository.hasMonthlyPrayerTimes("Artvin, Hopa", currentMonth) } returns true
        coEvery { repository.hasMonthlyPrayerTimes("Artvin, Hopa", nextMonth) } returns false
        every { networkMonitor.isWifiAvailable() } returns true
        every { monthlySyncPolicy.shouldDownloadNextMonth(any(), any(), any()) } returns true
        coEvery { repository.syncMonthlyPrayerTimesFor("Artvin, Hopa", nextMonth) } returns Result.success(Unit)

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.Synced)
        coVerify(exactly = 1) { prayerAlarmScheduler.scheduleUpcoming("Artvin, Hopa") }
    }

    @Test
    fun `skipped download still rolls the alarm window forward`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "İstanbul"
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", currentMonth) } returns true
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", nextMonth) } returns true
        every { networkMonitor.isWifiAvailable() } returns false
        every { monthlySyncPolicy.shouldDownloadNextMonth(any(), any(), any()) } returns false

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.Skipped)
        coVerify(exactly = 1) { prayerAlarmScheduler.scheduleUpcoming("İstanbul") }
    }

    @Test
    fun `missing current month data is downloaded before deciding on next month`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "İstanbul"
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", currentMonth) } returns false
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", nextMonth) } returns true
        coEvery { repository.syncMonthlyPrayerTimesFor("İstanbul", currentMonth) } returns Result.success(Unit)
        every { networkMonitor.isWifiAvailable() } returns false
        every { monthlySyncPolicy.shouldDownloadNextMonth(any(), any(), any()) } returns false

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.Skipped)
        coVerify(exactly = 1) { repository.syncMonthlyPrayerTimesFor("İstanbul", currentMonth) }
        coVerify(exactly = 1) { prayerAlarmScheduler.scheduleUpcoming("İstanbul") }
    }

    @Test
    fun `failed current month download reports failure without touching alarms`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "İstanbul"
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", currentMonth) } returns false
        coEvery { repository.syncMonthlyPrayerTimesFor("İstanbul", currentMonth) } returns
            Result.failure(RuntimeException("ağ hatası"))

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.Failed)
        coVerify(exactly = 0) { prayerAlarmScheduler.scheduleUpcoming(any()) }
    }

    @Test
    fun `failed download does not reschedule and reports failure`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "İstanbul"
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", currentMonth) } returns true
        coEvery { repository.hasMonthlyPrayerTimes("İstanbul", nextMonth) } returns false
        every { networkMonitor.isWifiAvailable() } returns true
        every { monthlySyncPolicy.shouldDownloadNextMonth(any(), any(), any()) } returns true
        coEvery { repository.syncMonthlyPrayerTimesFor("İstanbul", nextMonth) } returns
            Result.failure(RuntimeException("ağ hatası"))

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.Failed)
        coVerify(exactly = 0) { prayerAlarmScheduler.scheduleUpcoming(any()) }
    }

    @Test
    fun `no saved city returns NoCity without scheduling`() = runTest {
        coEvery { selectedCityStore.getCity() } returns null
        coEvery { repository.getCachedCity() } returns null

        val outcome = coordinator.syncNextMonthIfDue()

        assertThat(outcome).isEqualTo(SmartSyncCoordinator.Outcome.NoCity)
        coVerify(exactly = 0) { prayerAlarmScheduler.scheduleUpcoming(any()) }
        coVerify(exactly = 0) { repository.syncMonthlyPrayerTimesFor(any(), any()) }
    }
}
