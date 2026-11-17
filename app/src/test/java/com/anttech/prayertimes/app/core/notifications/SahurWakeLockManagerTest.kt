package com.anttech.prayertimes.core.notifications

import android.content.Context
import android.os.PowerManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Test

class SahurWakeLockManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)
    private val wakeLock = mockk<PowerManager.WakeLock>(relaxed = true)

    @After
    fun tearDown() {
        SahurWakeLockManager.release()
    }

    private fun stubPowerManager() {
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every {
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "namazvakitleri:SahurAlarmWake")
        } returns wakeLock
    }

    @Test
    fun `acquire uses partial wake lock with app tag and timeout`() {
        stubPowerManager()

        SahurWakeLockManager.acquire(context)

        verify { wakeLock.acquire(10_000L) }
    }

    @Test
    fun `release frees the original acquired lock once`() {
        stubPowerManager()
        SahurWakeLockManager.acquire(context)
        every { wakeLock.isHeld } returns true

        SahurWakeLockManager.release()
        SahurWakeLockManager.release()

        verify(exactly = 1) { wakeLock.release() }
    }
}
