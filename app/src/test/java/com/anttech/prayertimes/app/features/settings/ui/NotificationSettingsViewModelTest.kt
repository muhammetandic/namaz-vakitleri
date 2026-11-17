package com.anttech.prayertimes.features.settings.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.core.notifications.AlarmItem
import com.anttech.prayertimes.core.notifications.AlarmScheduler
import com.anttech.prayertimes.core.notifications.LastCityStore
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.core.notifications.TestNotificationSender
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.TimesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class NotificationSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeNotificationPreferences(
        enabled: Boolean = true,
        vibration: Boolean = true
    ) : NotificationPreferencesRepository {
        val enabledState = MutableStateFlow(enabled)
        val vibrationState = MutableStateFlow(vibration)

        override val notificationsEnabled: Flow<Boolean> = enabledState
        override val vibrationEnabled: Flow<Boolean> = vibrationState

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            enabledState.value = enabled
        }

        override suspend fun setVibrationEnabled(enabled: Boolean) {
            vibrationState.value = enabled
        }
    }

    private class RecordingAlarmScheduler : AlarmScheduler {
        val scheduled = mutableListOf<AlarmItem>()
        override fun schedule(item: AlarmItem) {
            scheduled += item
        }

        override fun cancel(item: AlarmItem) {
            scheduled.removeAll { it.id == item.id }
        }
    }

    private class FakeTimesRepository(
        private val timesByDate: Map<LocalDate, PrayerTimes>
    ) : TimesRepository {
        override fun getPrayerTimesForDate(city: String, date: LocalDate): Flow<PrayerTimes?> =
            flowOf(timesByDate[date])

        override fun getMonthlyPrayerTimes(city: String, yearMonth: String): Flow<List<PrayerTimes>> =
            flowOf(emptyList())

        override suspend fun syncMonthlyPrayerTimes(city: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun syncMonthlyPrayerTimesFor(city: String, yearMonth: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun hasMonthlyPrayerTimes(city: String, yearMonth: String): Boolean = false

        override suspend fun getCachedCity(): String? = null
    }

    private class FakeLastCityStore(var city: String? = "İstanbul") : LastCityStore {
        override suspend fun saveCity(city: String) {
            this.city = city
        }

        override suspend fun getCity(): String? = city
    }

    private class FakeTestNotificationSender : TestNotificationSender {
        var sent = 0
        var sahurSent = 0
        override fun send() {
            sent++
        }
        override fun sendSahurTest() {
            sahurSent++
        }
    }

    private val tomorrow: LocalDate = LocalDate.now().plusDays(1)

    private fun prayerTimes(date: LocalDate) = PrayerTimes(
        city = "İstanbul",
        date = date,
        imsak = LocalTime.of(5, 45),
        gunes = LocalTime.of(7, 10),
        ogle = LocalTime.of(12, 50),
        ikindi = LocalTime.of(15, 40),
        aksam = LocalTime.of(18, 5),
        yatsi = LocalTime.of(19, 25)
    )

    private val timesRepository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
    private val alarmScheduler = RecordingAlarmScheduler()
    private val cityStore = FakeLastCityStore()
    private val testSender = FakeTestNotificationSender()

    private fun createScheduler(preferences: NotificationPreferencesRepository) = PrayerAlarmScheduler(
        repository = timesRepository,
        alarmScheduler = alarmScheduler,
        lastCityStore = cityStore,
        notificationPreferences = preferences
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects persisted preferences`() = runTest {
        val preferences = FakeNotificationPreferences(enabled = false, vibration = false)
        val viewModel = NotificationSettingsViewModel(
            preferences = preferences,
            prayerAlarmScheduler = createScheduler(preferences),
            testNotificationSender = testSender
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.notificationsEnabled).isFalse()
        assertThat(viewModel.uiState.value.vibrationEnabled).isFalse()
    }

    @Test
    fun `turning notifications off persists choice and cancels pending alarms`() = runTest {
        val preferences = FakeNotificationPreferences(enabled = true, vibration = true)
        val scheduler = createScheduler(preferences)
        scheduler.scheduleUpcoming("İstanbul")
        assertThat(alarmScheduler.scheduled).isNotEmpty()

        val viewModel = NotificationSettingsViewModel(
            preferences = preferences,
            prayerAlarmScheduler = scheduler,
            testNotificationSender = testSender
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(NotificationSettingsIntent.ToggleNotifications)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(preferences.enabledState.value).isFalse()
        assertThat(viewModel.uiState.value.notificationsEnabled).isFalse()
        assertThat(alarmScheduler.scheduled).isEmpty()
    }

    @Test
    fun `turning notifications on rebuilds alarms`() = runTest {
        val preferences = FakeNotificationPreferences(enabled = false, vibration = true)
        val viewModel = NotificationSettingsViewModel(
            preferences = preferences,
            prayerAlarmScheduler = createScheduler(preferences),
            testNotificationSender = testSender
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(NotificationSettingsIntent.ToggleNotifications)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(preferences.enabledState.value).isTrue()
        assertThat(alarmScheduler.scheduled).hasSize(6)
    }

    @Test
    fun `toggling vibration persists the choice`() = runTest {
        val preferences = FakeNotificationPreferences(enabled = true, vibration = true)
        val viewModel = NotificationSettingsViewModel(
            preferences = preferences,
            prayerAlarmScheduler = createScheduler(preferences),
            testNotificationSender = testSender
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(NotificationSettingsIntent.ToggleVibration)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(preferences.vibrationState.value).isFalse()
        assertThat(viewModel.uiState.value.vibrationEnabled).isFalse()
    }

    @Test
    fun `sending test notification delegates and shows toast`() = runTest {
        val preferences = FakeNotificationPreferences()
        val viewModel = NotificationSettingsViewModel(
            preferences = preferences,
            prayerAlarmScheduler = createScheduler(preferences),
            testNotificationSender = testSender
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.sendIntent(NotificationSettingsIntent.SendTestNotification)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(testSender.sent).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(
                NotificationSettingsUiEffect.ShowToast("Test bildirimi gönderildi")
            )
        }
    }
}
