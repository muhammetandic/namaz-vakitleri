package com.anttech.prayertimes.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.features.times.domain.PrayerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AlarmSchedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val alarmManager: AlarmManager = mockk(relaxed = true)
    private lateinit var scheduler: AndroidAlarmScheduler

    @Before
    fun setUp() {
        io.mockk.mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk(relaxed = true)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        scheduler = AndroidAlarmScheduler(context, alarmManager)
    }

    @org.junit.After
    fun tearDown() {
        io.mockk.unmockkStatic(PendingIntent::class)
    }

    @Test
    fun `scheduleAlarm sets exact and allow while idle on AlarmManager`() {
        val triggerTime = LocalDateTime.of(
            LocalDate.of(2026, 11, 6),
            LocalTime.of(5, 45)
        )
        val alarmItem = AlarmItem(
            id = 101,
            triggerDateTime = triggerTime,
            prayerType = PrayerType.IMSAK,
            title = "İmsak Vakti",
            message = "Sabah namazı vakti girdi.",
            soundUri = null,
            isPreAlarm = false
        )

        val triggerEpochSlot = slot<Long>()
        val pendingIntentSlot = slot<PendingIntent>()

        every {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                capture(triggerEpochSlot),
                capture(pendingIntentSlot)
            )
        } returns Unit

        every {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                capture(triggerEpochSlot),
                capture(pendingIntentSlot)
            )
        } returns Unit

        // When
        scheduler.schedule(alarmItem)

        // Then
        val expectedEpoch = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertThat(triggerEpochSlot.captured).isEqualTo(expectedEpoch)
    }

    @Test
    fun `cancelAlarm cancels pending intent on AlarmManager`() {
        val alarmItem = AlarmItem(
            id = 101,
            triggerDateTime = LocalDateTime.now().plusHours(1),
            prayerType = PrayerType.OGLE,
            title = "Öğle Vakti",
            message = "Öğle ezanı okundu.",
            soundUri = null,
            isPreAlarm = false
        )

        // When
        scheduler.cancel(alarmItem)

        // Then
        verify { alarmManager.cancel(any<PendingIntent>()) }
    }
}
