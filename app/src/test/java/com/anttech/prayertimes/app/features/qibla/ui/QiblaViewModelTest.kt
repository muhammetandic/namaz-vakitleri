package com.anttech.prayertimes.features.qibla.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.core.sensor.CompassSensorManager
import com.anttech.prayertimes.features.qibla.domain.CalculateQiblaAngleUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val compassSensorManager: CompassSensorManager = mockk()
    private val calculateQiblaAngleUseCase = CalculateQiblaAngleUseCase()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when StartCompass intent is sent, qibla bearing is computed and azimuth flow is collected`() = runTest {
        // Istanbul lat/lng -> bearing ~ 151.7f
        val lat = 41.0082
        val lng = 28.9784
        val azimuthFlow = flowOf(0f, 90f, 151.7f)
        every { compassSensorManager.isSensorAvailable() } returns true
        every { compassSensorManager.getAzimuthFlow() } returns azimuthFlow

        val viewModel = QiblaViewModel(
            compassSensorManager = compassSensorManager,
            calculateQiblaAngleUseCase = calculateQiblaAngleUseCase
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.isLoading).isTrue()

            viewModel.sendIntent(QiblaIntent.StartCompass(lat, lng, "İstanbul"))
            testDispatcher.scheduler.advanceUntilIdle()

            // State after collecting azimuths
            val finalState = expectMostRecentItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.cityName).isEqualTo("İstanbul")
            assertThat(finalState.qiblaBearing).isWithin(1.5f).of(151.7f)
            assertThat(finalState.currentAzimuth).isEqualTo(151.7f)
            assertThat(finalState.isAligned).isTrue()
        }
    }

    @Test
    fun `when StopCompass intent is sent, azimuth flow collection is cancelled`() = runTest {
        val azimuthUpdates = MutableStateFlow(0f)
        var collecting = false
        every { compassSensorManager.isSensorAvailable() } returns true
        every { compassSensorManager.getAzimuthFlow() } returns flow {
            collecting = true
            try {
                emitAll(azimuthUpdates)
            } finally {
                collecting = false
            }
        }

        val viewModel = QiblaViewModel(
            compassSensorManager = compassSensorManager,
            calculateQiblaAngleUseCase = calculateQiblaAngleUseCase
        )

        viewModel.sendIntent(QiblaIntent.StartCompass(41.0082, 28.9784, "İstanbul"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(collecting).isTrue()

        viewModel.sendIntent(QiblaIntent.StopCompass)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(collecting).isFalse()
    }

    @Test
    fun `when compass sensor is not available, isSensorAvailable is false and static bearing is shown`() = runTest {
        val lat = 41.0082
        val lng = 28.9784
        every { compassSensorManager.isSensorAvailable() } returns false

        val viewModel = QiblaViewModel(
            compassSensorManager = compassSensorManager,
            calculateQiblaAngleUseCase = calculateQiblaAngleUseCase
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.isLoading).isTrue()

            viewModel.sendIntent(QiblaIntent.StartCompass(lat, lng, "İstanbul"))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isSensorAvailable).isFalse()
            assertThat(state.displayMode).isEqualTo(QiblaDisplayMode.MAP)
            assertThat(state.qiblaBearing).isWithin(1.5f).of(151.7f)
            assertThat(state.needleAngle).isEqualTo(state.qiblaBearing)
        }
    }

    @Test
    fun `when SetDisplayMode intent is sent, displayMode is toggled`() = runTest {
        every { compassSensorManager.isSensorAvailable() } returns true
        every { compassSensorManager.getAzimuthFlow() } returns flowOf(0f)

        val viewModel = QiblaViewModel(
            compassSensorManager = compassSensorManager,
            calculateQiblaAngleUseCase = calculateQiblaAngleUseCase
        )

        viewModel.uiState.test {
            awaitItem()
            viewModel.sendIntent(QiblaIntent.StartCompass(41.0082, 28.9784, "İstanbul"))
            testDispatcher.scheduler.advanceUntilIdle()

            val state1 = expectMostRecentItem()
            assertThat(state1.displayMode).isEqualTo(QiblaDisplayMode.COMPASS)

            viewModel.sendIntent(QiblaIntent.SetDisplayMode(QiblaDisplayMode.MAP))
            testDispatcher.scheduler.advanceUntilIdle()

            val state2 = expectMostRecentItem()
            assertThat(state2.displayMode).isEqualTo(QiblaDisplayMode.MAP)
        }
    }
}
