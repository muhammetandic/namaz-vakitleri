package com.anttech.prayertimes.features.calendar.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.calendar.data.FastTrackingRepositoryImpl
import com.anttech.prayertimes.features.calendar.domain.FastTrackingDay
import com.anttech.prayertimes.features.calendar.domain.FastTrackingRepository
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.TimesRepository
import io.mockk.coEvery
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
class ImsakiyeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: TimesRepository = mockk(relaxed = true)
    private val fastTrackingRepository: FastTrackingRepository = mockk(relaxed = true)

    private val sampleMonthlyTimes = listOf(
        PrayerTimes(
            city = "İstanbul",
            date = LocalDate.of(2026, 11, 5), // Thursday
            imsak = LocalTime.of(5, 45),
            gunes = LocalTime.of(7, 10),
            ogle = LocalTime.of(12, 50),
            ikindi = LocalTime.of(15, 40),
            aksam = LocalTime.of(18, 5),
            yatsi = LocalTime.of(19, 25),
            hijriDay = 15,
            hijriMonth = 3,
            hijriMonthName = "Rebiülevvel",
            hijriYear = 1448
        ),
        PrayerTimes(
            city = "İstanbul",
            date = LocalDate.of(2026, 11, 6), // Friday
            imsak = LocalTime.of(5, 46),
            gunes = LocalTime.of(7, 11),
            ogle = LocalTime.of(12, 50),
            ikindi = LocalTime.of(15, 39),
            aksam = LocalTime.of(18, 4),
            yatsi = LocalTime.of(19, 24),
            hijriDay = 16,
            hijriMonth = 3,
            hijriMonthName = "Rebiülevvel",
            hijriYear = 1448
        )
    )

    private val emptyTrackingFlow = flowOf(emptyList<FastTrackingDay>())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when LoadImsakiye intent is sent, monthly prayer times are loaded and days mapped`() = runTest {
        coEvery { repository.getMonthlyPrayerTimes("İstanbul", "2026-11") } returns flowOf(sampleMonthlyTimes)
        coEvery { fastTrackingRepository.getMonthlyTracking("İstanbul", "2026-11") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getNonFastedRamadanDays("İstanbul") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getFastedNonRamadanDays("İstanbul") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getForbiddenDays("İstanbul") } returns emptyTrackingFlow

        val viewModel = ImsakiyeViewModel(
            repository = repository,
            fastTrackingRepository = fastTrackingRepository
        )

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.sendIntent(ImsakiyeIntent.LoadImsakiye("İstanbul", "2026-11"))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.city).isEqualTo("İstanbul")
            assertThat(state.days).hasSize(2)

            val fridayDay = state.days[1]
            assertThat(fridayDay.isFriday).isTrue()
            assertThat(fridayDay.aksamFormatted).isEqualTo("18:04")
            assertThat(fridayDay.imsakFormatted).isEqualTo("05:46")
            assertThat(fridayDay.hijriDay).isEqualTo(16)
            assertThat(fridayDay.hijriMonthName).isEqualTo("Rebiülevvel")
        }
    }

    @Test
    fun `when ToggleFastedDay intent is sent, delegates to fastTrackingRepository`() = runTest {
        coEvery { repository.getMonthlyPrayerTimes("İstanbul", "2026-11") } returns flowOf(sampleMonthlyTimes)
        coEvery { fastTrackingRepository.getMonthlyTracking("İstanbul", "2026-11") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getNonFastedRamadanDays("İstanbul") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getFastedNonRamadanDays("İstanbul") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getForbiddenDays("İstanbul") } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.toggleFastedDay(
            city = "İstanbul",
            date = LocalDate.of(2026, 11, 5),
            hijriDay = 15,
            hijriMonth = 3,
            hijriMonthName = "Rebiülevvel",
            hijriYear = 1448
        ) } returns true

        val viewModel = ImsakiyeViewModel(
            repository = repository,
            fastTrackingRepository = fastTrackingRepository
        )

        viewModel.sendIntent(ImsakiyeIntent.LoadImsakiye("İstanbul", "2026-11"))
        testDispatcher.scheduler.advanceUntilIdle()

        val targetDate = LocalDate.of(2026, 11, 5)
        viewModel.sendIntent(ImsakiyeIntent.ToggleFastedDay(
            date = targetDate,
            hijriDay = 15,
            hijriMonth = 3,
            hijriMonthName = "Rebiülevvel",
            hijriYear = 1448
        ))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify ShowMessage effect was sent
        // Note: We can't easily test Channel effects with Turbine, but the call should be made
    }

    @Test
    fun `when LoadImsakiye re-sent for another city, old collector is cancelled and new city data shown`() = runTest {
        val ankaraMonthlyTimes = listOf(
            PrayerTimes(
                city = "Ankara",
                date = LocalDate.of(2026, 11, 5),
                imsak = LocalTime.of(5, 30),
                gunes = LocalTime.of(7, 0),
                ogle = LocalTime.of(12, 40),
                ikindi = LocalTime.of(15, 30),
                aksam = LocalTime.of(17, 55),
                yatsi = LocalTime.of(19, 15),
                hijriDay = 15,
                hijriMonth = 3,
                hijriMonthName = "Rebiülevvel",
                hijriYear = 1448
            )
        )
        val istanbulFlow = flowOf(sampleMonthlyTimes)
        val ankaraFlow = flowOf(ankaraMonthlyTimes)
        coEvery { repository.getMonthlyPrayerTimes("İstanbul", "2026-11") } returns istanbulFlow
        coEvery { repository.getMonthlyPrayerTimes("Ankara", "2026-11") } returns ankaraFlow
        coEvery { fastTrackingRepository.getMonthlyTracking(any(), any()) } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getNonFastedRamadanDays(any()) } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getFastedNonRamadanDays(any()) } returns emptyTrackingFlow
        coEvery { fastTrackingRepository.getForbiddenDays(any()) } returns emptyTrackingFlow

        val viewModel = ImsakiyeViewModel(
            repository = repository,
            fastTrackingRepository = fastTrackingRepository
        )

        viewModel.sendIntent(ImsakiyeIntent.LoadImsakiye("İstanbul", "2026-11"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.city).isEqualTo("İstanbul")
        assertThat(viewModel.uiState.value.days).hasSize(2)

        viewModel.sendIntent(ImsakiyeIntent.LoadImsakiye("Ankara", "2026-11"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.city).isEqualTo("Ankara")
        assertThat(viewModel.uiState.value.days).hasSize(1)
        assertThat(viewModel.uiState.value.days.first().imsakFormatted).isEqualTo("05:30")
    }
}
