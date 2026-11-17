package com.anttech.prayertimes.features.times.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.core.location.GeocoderHelper
import com.anttech.prayertimes.core.location.SelectedCityStore
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.features.times.domain.CalculateKerahaStatusUseCase
import com.anttech.prayertimes.features.times.domain.CalculateNextPrayerUseCase
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.TimesRepository
import com.anttech.prayertimes.features.widget.WidgetUpdater
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: TimesRepository = mockk()
    private val locationTracker: com.anttech.prayertimes.core.location.LocationTracker = mockk(relaxed = true)
    private val calculateNextPrayerUseCase = CalculateNextPrayerUseCase()
    private val calculateKerahaStatusUseCase = CalculateKerahaStatusUseCase()
    private val prayerAlarmScheduler: PrayerAlarmScheduler = mockk(relaxed = true)
    private val selectedCityStore: SelectedCityStore = mockk(relaxed = true)
    private val geocoderHelper: GeocoderHelper = mockk()
    private val widgetUpdater: WidgetUpdater = mockk(relaxed = true)

    private val samplePrayerTimes = PrayerTimes(
        city = "İstanbul",
        date = LocalDate.of(2026, 11, 5),
        imsak = LocalTime.of(5, 45),
        gunes = LocalTime.of(7, 10),
        ogle = LocalTime.of(12, 50),
        ikindi = LocalTime.of(15, 40),
        aksam = LocalTime.of(18, 5),
        yatsi = LocalTime.of(19, 25)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.syncMonthlyPrayerTimes(any()) } returns Result.success(Unit)
        coEvery { selectedCityStore.getCity() } returns null
        every { geocoderHelper.getCoordinatesForCity(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when LoadTimes intent is sent, state updates to loaded with calculated countdown`() = runTest {
        coEvery { repository.getPrayerTimesForDate("İstanbul", any()) } returns flowOf(samplePrayerTimes)

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertThat(initial.isLoading).isTrue()

            // Trigger intent
            viewModel.sendIntent(TimesIntent.LoadTimes("İstanbul"))
            testDispatcher.scheduler.advanceUntilIdle()

            // Loaded state
            val loadedState = awaitItem()
            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.city).isEqualTo("İstanbul")
            assertThat(loadedState.prayerTimes).isEqualTo(samplePrayerTimes)
            assertThat(loadedState.countdown).isNotNull()
        }
    }

    @Test
    fun `when times load, home screen widgets are refreshed`() = runTest {
        coEvery { repository.getPrayerTimesForDate("İstanbul", any()) } returns flowOf(samplePrayerTimes)

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.sendIntent(TimesIntent.LoadTimes("İstanbul"))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { widgetUpdater.updateAll() }
    }

    @Test
    fun `when ToggleSimpleMode intent is sent, isSimpleMode flips`() = runTest {
        coEvery { repository.getPrayerTimesForDate(any(), any()) } returns flowOf(samplePrayerTimes)

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.ToggleSimpleMode)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertThat(state.isSimpleMode).isTrue()
        }
    }

    @Test
    fun `when RefreshLocation intent is sent and GPS succeeds, city updates in state`() = runTest {
        coEvery { locationTracker.getCurrentLocation() } returns com.anttech.prayertimes.core.location.UserLocation(
            latitude = 39.9334,
            longitude = 32.8597,
            cityName = "Ankara",
            districtName = "Çankaya"
        )
        coEvery { repository.syncMonthlyPrayerTimes("Ankara") } returns Result.success(Unit)
        coEvery { repository.getPrayerTimesForDate(any(), any()) } returns flowOf(samplePrayerTimes.copy(city = "Ankara, Çankaya"))

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.sendIntent(TimesIntent.RefreshLocation)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.city).isEqualTo("Ankara, Çankaya")
            assertThat(state.latitude).isEqualTo(39.9334)
            assertThat(state.longitude).isEqualTo(32.8597)
        }
    }

    @Test
    fun `when ChangeCity intent is sent, city is saved and times load for it`() = runTest {
        coEvery { repository.getPrayerTimesForDate("Ankara", any()) } returns flowOf(
            samplePrayerTimes.copy(city = "Ankara")
        )

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.ChangeCity("Ankara"))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { selectedCityStore.saveCity("Ankara") }
            assertThat(expectMostRecentItem().city).isEqualTo("Ankara")
        }
    }

    @Test
    fun `RefreshLocation uses saved manual city without GPS lookup`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "Ankara"
        coEvery { repository.getPrayerTimesForDate("Ankara", any()) } returns flowOf(
            samplePrayerTimes.copy(city = "Ankara")
        )

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.RefreshLocation)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { locationTracker.getCurrentLocation() }
            assertThat(expectMostRecentItem().city).isEqualTo("Ankara")
        }
    }

    @Test
    fun `ChangeCity with district saves district and updates coordinates from geocoder`() = runTest {
        coEvery { repository.getPrayerTimesForDate(any(), any()) } returns flowOf(
            samplePrayerTimes.copy(city = "Artvin, Hopa")
        )
        every { geocoderHelper.getCoordinatesForCity("Artvin", "Hopa") } returns (41.18 to 41.83)

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.ChangeCity("Artvin, Hopa"))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { selectedCityStore.saveCity("Artvin", "Hopa") }
            val state = expectMostRecentItem()
            assertThat(state.city).isEqualTo("Artvin, Hopa")
            assertThat(state.latitude).isEqualTo(41.18)
            assertThat(state.longitude).isEqualTo(41.83)
        }
    }

    @Test
    fun `RefreshLocation with saved manual city updates coordinates from geocoder`() = runTest {
        coEvery { selectedCityStore.getCity() } returns "Artvin, Hopa"
        coEvery { repository.getPrayerTimesForDate(any(), any()) } returns flowOf(
            samplePrayerTimes.copy(city = "Artvin, Hopa")
        )
        every { geocoderHelper.getCoordinatesForCity("Artvin", "Hopa") } returns (41.18 to 41.83)

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.RefreshLocation)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { locationTracker.getCurrentLocation() }
            val state = expectMostRecentItem()
            assertThat(state.city).isEqualTo("Artvin, Hopa")
            assertThat(state.latitude).isEqualTo(41.18)
            assertThat(state.longitude).isEqualTo(41.83)
        }
    }

    @Test
    fun `ChangeCity keeps previous coordinates when geocoder fails`() = runTest {
        coEvery { repository.getPrayerTimesForDate(any(), any()) } returns flowOf(
            samplePrayerTimes.copy(city = "Artvin")
        )
        every { geocoderHelper.getCoordinatesForCity("Artvin", null) } returns null

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.ChangeCity("Artvin"))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.city).isEqualTo("Artvin")
            assertThat(state.latitude).isEqualTo(TimesUiState().latitude)
            assertThat(state.longitude).isEqualTo(TimesUiState().longitude)
        }
    }

    @Test
    fun `UseLocation clears saved city and falls back to GPS`() = runTest {
        coEvery { locationTracker.getCurrentLocation() } returns com.anttech.prayertimes.core.location.UserLocation(
            latitude = 36.9,
            longitude = 30.7,
            cityName = "Antalya",
            districtName = "Kepez"
        )
        coEvery { repository.getPrayerTimesForDate(any(), any()) } returns flowOf(
            samplePrayerTimes.copy(city = "Antalya, Kepez")
        )

        val viewModel = TimesViewModel(
            repository = repository,
            calculateNextPrayerUseCase = calculateNextPrayerUseCase,
            calculateKerahaStatusUseCase = calculateKerahaStatusUseCase,
            locationTracker = locationTracker,
            prayerAlarmScheduler = prayerAlarmScheduler,
            selectedCityStore = selectedCityStore,
            geocoderHelper = geocoderHelper,
            widgetUpdater = widgetUpdater
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(TimesIntent.UseLocation)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { selectedCityStore.clearCity() }
            coVerify { locationTracker.getCurrentLocation() }
            val state = expectMostRecentItem()
            assertThat(state.city).isEqualTo("Antalya, Kepez")
            assertThat(state.latitude).isEqualTo(36.9)
            assertThat(state.longitude).isEqualTo(30.7)
        }
    }
}
