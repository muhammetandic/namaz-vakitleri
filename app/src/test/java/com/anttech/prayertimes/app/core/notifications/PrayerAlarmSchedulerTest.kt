package com.anttech.prayertimes.core.notifications

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.features.times.domain.TimesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PrayerAlarmSchedulerTest {

    private val today: LocalDate = LocalDate.now()

    private fun prayerTimes(date: LocalDate, city: String = "İstanbul") = PrayerTimes(
        city = city,
        date = date,
        imsak = LocalTime.of(5, 45),
        gunes = LocalTime.of(7, 10),
        ogle = LocalTime.of(12, 50),
        ikindi = LocalTime.of(15, 40),
        aksam = LocalTime.of(18, 5),
        yatsi = LocalTime.of(19, 25)
    )

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

    private class FakeLastCityStore(var city: String? = null) : LastCityStore {
        override suspend fun saveCity(city: String) {
            this.city = city
        }

        override suspend fun getCity(): String? = city
    }

    private class FakeNotificationPreferences(
        var enabled: Boolean = true
    ) : NotificationPreferencesRepository {
        override val notificationsEnabled: Flow<Boolean> = flowOf(enabled)
        override val vibrationEnabled: Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            this.enabled = enabled
        }

        override suspend fun setVibrationEnabled(enabled: Boolean) = Unit
    }

    @Test
    fun `scheduleUpcoming schedules one alarm per prayer for each known day`() = runTest {
        val tomorrow = today.plusDays(1)
        val dayAfter = today.plusDays(2)
        val repository = FakeTimesRepository(
            mapOf(
                tomorrow to prayerTimes(tomorrow),
                dayAfter to prayerTimes(dayAfter)
            )
        )
        val scheduler = RecordingAlarmScheduler()
        val cityStore = FakeLastCityStore()
        val subject = PrayerAlarmScheduler(repository, scheduler, cityStore)

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled).hasSize(12)
        assertThat(scheduler.scheduled.map { it.id }).containsNoDuplicates()
        assertThat(scheduler.scheduled.map { it.prayerType }.toSet())
            .containsExactlyElementsIn(PrayerType.entries)
        assertThat(cityStore.city).isEqualTo("İstanbul")

        val tomorrowOgle = scheduler.scheduled.single {
            it.prayerType == PrayerType.OGLE && it.triggerDateTime.toLocalDate() == tomorrow
        }
        assertThat(tomorrowOgle.triggerDateTime.toLocalTime()).isEqualTo(LocalTime.of(12, 50))
        assertThat(tomorrowOgle.title).contains("Öğle")
    }

    @Test
    fun `scheduleUpcoming skips days without prayer times`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(repository, scheduler, FakeLastCityStore())

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled).hasSize(6)
    }

    @Test
    fun `rescheduleSaved uses last saved city`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(city = "Ankara")
        )

        subject.rescheduleSaved()

        assertThat(scheduler.scheduled).hasSize(6)
    }

    @Test
    fun `rescheduleSaved does nothing when no city saved`() = runTest {
        val repository = FakeTimesRepository(emptyMap())
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(repository, scheduler, FakeLastCityStore(city = null))

        subject.rescheduleSaved()

        assertThat(scheduler.scheduled).isEmpty()
    }

    @Test
    fun `scheduleUpcoming skips alarms when notifications disabled`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val cityStore = FakeLastCityStore()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            cityStore,
            notificationPreferences = FakeNotificationPreferences(enabled = false)
        )

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled).isEmpty()
        assertThat(cityStore.city).isEqualTo("İstanbul")
    }

    @Test
    fun `scheduleUpcoming schedules alarms beyond 30 days so window keeps rolling`() = runTest {
        val farDay = today.plusDays(40)
        val repository = FakeTimesRepository(mapOf(farDay to prayerTimes(farDay)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(repository, scheduler, FakeLastCityStore())

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled).hasSize(6)
    }

    @Test
    fun `notifications disabled but widget on home screen keeps alarms for widget refresh`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(),
            notificationPreferences = FakeNotificationPreferences(enabled = false),
            hasActiveWidgets = { true }
        )

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled).hasSize(6)
    }

    @Test
    fun `notifications disabled and no widget clears pending alarms`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(city = "İstanbul"),
            notificationPreferences = FakeNotificationPreferences(enabled = true)
        )
        subject.scheduleUpcoming("İstanbul")
        assertThat(scheduler.scheduled).hasSize(6)

        val disabled = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(city = "İstanbul"),
            notificationPreferences = FakeNotificationPreferences(enabled = false),
            hasActiveWidgets = { false }
        )
        disabled.rescheduleSaved()

        assertThat(scheduler.scheduled).isEmpty()
    }

    private class FakeFastingAlarmPreferences(
        var enabled: Boolean = false,
        var minutesBefore: Int = 30
    ) : FastingAlarmPreferencesRepository {
        override val sahurAlarmEnabled: Flow<Boolean> = flowOf(enabled)
        override val sahurAlarmMinutesBefore: Flow<Int> = flowOf(minutesBefore)

        override suspend fun setSahurAlarmEnabled(enabled: Boolean) {
            this.enabled = enabled
        }

        override suspend fun setSahurAlarmMinutesBefore(minutes: Int) {
            this.minutesBefore = minutes
        }
    }

    @Test
    fun `sahur alarm is scheduled before imsak when enabled`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(),
            fastingAlarmPreferences = FakeFastingAlarmPreferences(enabled = true, minutesBefore = 30)
        )

        subject.scheduleUpcoming("İstanbul")

        val sahurAlarm = scheduler.scheduled.single { it.isPreAlarm }
        assertThat(sahurAlarm.triggerDateTime.toLocalDate()).isEqualTo(tomorrow)
        assertThat(sahurAlarm.triggerDateTime.toLocalTime()).isEqualTo(LocalTime.of(5, 15))
        assertThat(sahurAlarm.title).contains("Sahur")
        assertThat(sahurAlarm.prayerType).isEqualTo(PrayerType.IMSAK)
    }

    @Test
    fun `sahur alarm uses separate id from imsak alarm`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(),
            fastingAlarmPreferences = FakeFastingAlarmPreferences(enabled = true)
        )

        subject.scheduleUpcoming("İstanbul")

        val ids = scheduler.scheduled.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `sahur alarm is not scheduled when disabled`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(),
            fastingAlarmPreferences = FakeFastingAlarmPreferences(enabled = false)
        )

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled.none { it.isPreAlarm }).isTrue()
    }

    @Test
    fun `sahur alarm is scheduled even when prayer notifications are disabled`() = runTest {
        val tomorrow = today.plusDays(1)
        val repository = FakeTimesRepository(mapOf(tomorrow to prayerTimes(tomorrow)))
        val scheduler = RecordingAlarmScheduler()
        val subject = PrayerAlarmScheduler(
            repository,
            scheduler,
            FakeLastCityStore(),
            notificationPreferences = FakeNotificationPreferences(enabled = false),
            fastingAlarmPreferences = FakeFastingAlarmPreferences(enabled = true)
        )

        subject.scheduleUpcoming("İstanbul")

        assertThat(scheduler.scheduled.count { it.isPreAlarm }).isEqualTo(1)
    }
}
