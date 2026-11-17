package com.anttech.prayertimes.features.settings.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.settings.domain.AudioPlayerManager
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.core.notifications.TestNotificationSender
import io.mockk.*
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

@OptIn(ExperimentalCoroutinesApi::class)
class SoundSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: SoundPreferencesRepository = mockk(relaxed = true)
    private val fastingAlarmPreferences: FastingAlarmPreferencesRepository = mockk(relaxed = true)
    private val alarmScheduler: PrayerAlarmScheduler = mockk(relaxed = true)
    private val audioPlayerManager: AudioPlayerManager = mockk(relaxed = true)
    private val testNotificationSender: TestNotificationSender = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { fastingAlarmPreferences.sahurAlarmEnabled } returns flowOf(false)
        every { fastingAlarmPreferences.sahurAlarmMinutesBefore } returns flowOf(30)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when SelectSound intent is sent, repository saves choice and state updates`() = runTest {
        val soundOption = SoundOption.BuiltIn(
            id = "ney_taksim",
            title = "Ney Ezgisi & Taksim",
            resName = "melody_ney"
        )
        coEvery { repository.getSoundForPrayer(PrayerType.IMSAK) } returns flowOf(soundOption)

        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.sendIntent(SoundSettingsIntent.SelectSound(PrayerType.IMSAK, soundOption))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repository.saveSoundForPrayer(PrayerType.IMSAK, soundOption) }
            val state = awaitItem()
            assertThat(state.prayerSounds[PrayerType.IMSAK]).isEqualTo(soundOption)
        }
    }

    @Test
    fun `when PlayPreview intent is sent, audio player starts playing`() = runTest {
        val soundOption = SoundOption.BuiltIn(
            id = "crystal_alarm",
            title = "Kristal Vakit Melodisi",
            resName = "tone_crystal_alarm"
        )

        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.PlayPreview(soundOption))
            testDispatcher.scheduler.advanceUntilIdle()

            verify { audioPlayerManager.play(soundOption, 0.8f, any()) }
            val state = awaitItem()
            assertThat(state.currentlyPlayingSoundId).isEqualTo("crystal_alarm")
        }
    }

    @Test
    fun `when StopPreview intent is sent, audio player stops and state clears playing id`() = runTest {
        val soundOption = SoundOption.BuiltIn(
            id = "crystal_alarm",
            title = "Kristal Vakit Melodisi",
            resName = "tone_crystal_alarm"
        )

        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            // Play first
            viewModel.sendIntent(SoundSettingsIntent.PlayPreview(soundOption))
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Playing state

            // Stop
            viewModel.sendIntent(SoundSettingsIntent.StopPreview)
            testDispatcher.scheduler.advanceUntilIdle()

            verify { audioPlayerManager.stop() }
            val state = awaitItem()
            assertThat(state.currentlyPlayingSoundId).isNull()
        }
    }

    @Test
    fun `when CustomAudioPicked intent is sent, custom sound option is saved`() = runTest {
        val customUri = "content://media/external/audio/media/12345"
        val customName = "Guzel_Ezan.mp3"

        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(
                SoundSettingsIntent.CustomAudioPicked(
                    prayerType = PrayerType.AKSAM,
                    uriString = customUri,
                    displayName = customName
                )
            )
            testDispatcher.scheduler.advanceUntilIdle()

            val expectedOption = SoundOption.CustomFile(
                id = customUri,
                title = customName,
                uriString = customUri
            )
            coVerify { repository.saveSoundForPrayer(PrayerType.AKSAM, expectedOption) }

            val state = awaitItem()
            assertThat(state.prayerSounds[PrayerType.AKSAM]).isEqualTo(expectedOption)
        }
    }

    @Test
    fun `global volume change updates state and is persisted`() = runTest {
        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.SetGlobalVolume(0.35f))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repository.setGlobalVolume(0.35f) }
            val state = awaitItem()
            assertThat(state.globalVolume).isEqualTo(0.35f)
        }
    }

    @Test
    fun `delay selection is saved only for the selected prayer`() = runTest {
        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.SelectDelay(PrayerType.AKSAM, 5))
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repository.saveDelayMinutesForPrayer(PrayerType.AKSAM, 5) }
            coVerify(exactly = 0) { repository.saveDelayMinutesForPrayer(PrayerType.IMSAK, any()) }
            val state = awaitItem()
            assertThat(state.prayerDelays[PrayerType.AKSAM]).isEqualTo(5)
        }
    }

    @Test
    fun `delay selection without prayer applies to every prayer`() = runTest {
        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.SelectDelay(null, 2))
            testDispatcher.scheduler.advanceUntilIdle()

            PrayerType.entries.forEach { prayer ->
                coVerify { repository.saveDelayMinutesForPrayer(prayer, 2) }
            }
            val state = awaitItem()
            assertThat(state.prayerDelays.values.toSet()).containsExactly(2)
        }
    }

    @Test
    fun `global volume change is applied to the audio player immediately`() = runTest {
        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.sendIntent(SoundSettingsIntent.SetGlobalVolume(0.35f))
        testDispatcher.scheduler.advanceUntilIdle()

        verify { audioPlayerManager.setVolume(0.35f) }
    }

    @Test
    fun `when preview finishes playing, play button state resets`() = runTest {
        val soundOption = SoundOption.BuiltIn(
            id = "crystal_alarm",
            title = "Kristal Vakit Melodisi",
            resName = "tone_crystal_alarm"
        )
        val finished = slot<() -> Unit>()
        every {
            audioPlayerManager.play(any(), any(), capture(finished))
        } returns Unit

        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.PlayPreview(soundOption, "IMSAK"))
            testDispatcher.scheduler.advanceUntilIdle()
            assertThat(awaitItem().currentlyPlayingTargetId).isEqualTo("IMSAK")

            finished.captured.invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertThat(state.currentlyPlayingSoundId).isNull()
            assertThat(state.currentlyPlayingTargetId).isNull()
        }
    }

    @Test
    fun `finished callback of an older preview does not clear a newer one`() = runTest {
        val first = SoundOption.BuiltIn("a", "A", "melody_ney")
        val second = SoundOption.BuiltIn("b", "B", "ezan1")
        val finished = mutableListOf<() -> Unit>()
        every {
            audioPlayerManager.play(any(), any(), capture(finished))
        } returns Unit

        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        viewModel.uiState.test {
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.PlayPreview(first, "IMSAK"))
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.sendIntent(SoundSettingsIntent.PlayPreview(second, "AKSAM"))
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            finished.first().invoke()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.currentlyPlayingTargetId).isEqualTo("AKSAM")
        }
    }

    @Test
    fun `toggling sahur alarm persists preference and reschedules alarms`() = runTest {
        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        // Başlangıç akışları otursun, sonra tercih değiştirilsin.
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(SoundSettingsIntent.ToggleSahurAlarm)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.sahurAlarmEnabled).isTrue()
        coVerify { fastingAlarmPreferences.setSahurAlarmEnabled(true) }
        coVerify { alarmScheduler.rescheduleSaved() }
    }

    @Test
    fun `selecting sahur alarm minutes persists preference`() = runTest {
        val viewModel = SoundSettingsViewModel(
            repository = repository,
            fastingAlarmPreferences = fastingAlarmPreferences,
            alarmScheduler = alarmScheduler,
            audioPlayerManager = audioPlayerManager,
            testNotificationSender = testNotificationSender
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(SoundSettingsIntent.SetSahurAlarmMinutes(45))
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.sahurAlarmMinutesBefore).isEqualTo(45)
        coVerify { fastingAlarmPreferences.setSahurAlarmMinutesBefore(45) }
        coVerify { alarmScheduler.rescheduleSaved() }
    }
}
