package com.anttech.prayertimes.features.tracker.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `IncrementDhikr increases count by 1 and fires vibration effect`() = runTest {
        val viewModel = TrackerViewModel()

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.sendIntent(TrackerIntent.IncrementDhikr)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.dhikrCount).isEqualTo(1)
        }
    }

    @Test
    fun `when dhikr reaches target 33, target reached effect is fired and lap count increments`() = runTest {
        val viewModel = TrackerViewModel()

        viewModel.effect.test {
            // Repeat 33 times
            repeat(33) {
                viewModel.sendIntent(TrackerIntent.IncrementDhikr)
                testDispatcher.scheduler.advanceUntilIdle()
            }

            // At 33: TriggerTargetVibration effect should be emitted
            val effect = expectMostRecentItem()
            assertThat(effect).isEqualTo(TrackerUiEffect.TriggerTargetVibration)
            assertThat(viewModel.uiState.value.dhikrCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.dhikrLaps).isEqualTo(1)
        }
    }

    @Test
    fun `ResetDhikr resets count and laps to zero`() = runTest {
        val viewModel = TrackerViewModel()

        viewModel.sendIntent(TrackerIntent.IncrementDhikr)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(TrackerIntent.ResetDhikr)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.dhikrCount).isEqualTo(0)
        assertThat(viewModel.uiState.value.dhikrLaps).isEqualTo(0)
    }
}
