package com.anttech.prayertimes.core.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackerTest {

    private val context: Context = mockk(relaxed = true)
    private val fusedLocationClient: FusedLocationProviderClient = mockk(relaxed = true)
    private val geocoderHelper: GeocoderHelper = mockk()
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    private lateinit var locationTracker: DefaultLocationTracker

    @Before
    fun setUp() {
        every { context.checkSelfPermission(any()) } returns PackageManager.PERMISSION_GRANTED
        locationTracker = DefaultLocationTracker(
            context = context,
            fusedLocationProviderClient = fusedLocationClient,
            geocoderHelper = geocoderHelper,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `getCurrentLocation returns UserLocation with city name when fused location succeeds`() = runTest(testScheduler) {
        val mockLocation = mockk<Location> {
            every { latitude } returns 41.0082
            every { longitude } returns 28.9784
            every { time } returns System.currentTimeMillis()
        }

        val task = mockk<Task<Location>>()
        val successSlot = slot<OnSuccessListener<Location>>()

        every { fusedLocationClient.lastLocation } returns task
        every { task.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockLocation)
            task
        }
        every { task.addOnFailureListener(any()) } returns task

        every { geocoderHelper.getCityAndDistrict(41.0082, 28.9784) } returns ("İstanbul" to "Fatih")

        // When
        val result = locationTracker.getCurrentLocation()

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.latitude).isEqualTo(41.0082)
        assertThat(result?.longitude).isEqualTo(28.9784)
        assertThat(result?.cityName).isEqualTo("İstanbul")
        assertThat(result?.districtName).isEqualTo("Fatih")
    }

    @Test
    fun `getCurrentLocation ignores stale cached location and uses fresh fix`() = runTest(testScheduler) {
        val staleLocation = mockk<Location> {
            every { latitude } returns 41.1
            every { longitude } returns 29.1
            every { time } returns System.currentTimeMillis() - 60 * 60 * 1000
        }
        val freshLocation = mockk<Location> {
            every { latitude } returns 41.0082
            every { longitude } returns 28.9784
            every { time } returns System.currentTimeMillis()
        }

        val cachedTask = mockk<Task<Location>>()
        val cachedSuccessSlot = slot<OnSuccessListener<Location>>()
        val freshTask = mockk<Task<Location>>()
        val freshSuccessSlot = slot<OnSuccessListener<Location>>()

        every { fusedLocationClient.lastLocation } returns cachedTask
        every { cachedTask.addOnSuccessListener(capture(cachedSuccessSlot)) } answers {
            cachedSuccessSlot.captured.onSuccess(staleLocation)
            cachedTask
        }
        every { cachedTask.addOnFailureListener(any()) } returns cachedTask

        every {
            fusedLocationClient.getCurrentLocation(any<Int>(), any<CancellationToken>())
        } returns freshTask
        every { freshTask.addOnSuccessListener(capture(freshSuccessSlot)) } answers {
            freshSuccessSlot.captured.onSuccess(freshLocation)
            freshTask
        }
        every { freshTask.addOnFailureListener(any()) } returns freshTask

        every { geocoderHelper.getCityAndDistrict(41.0082, 28.9784) } returns ("İstanbul" to "Fatih")

        // When
        val result = locationTracker.getCurrentLocation()

        // Then
        assertThat(result?.cityName).isEqualTo("İstanbul")
        assertThat(result?.districtName).isEqualTo("Fatih")
        verify(exactly = 0) { geocoderHelper.getCityAndDistrict(41.1, 29.1) }
    }

    @Test
    fun `getCurrentLocation returns null when fused location task fails`() = runTest(testScheduler) {
        val task = mockk<Task<Location>>()
        val failureSlot = slot<OnFailureListener>()

        every { fusedLocationClient.lastLocation } returns task
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(capture(failureSlot)) } answers {
            failureSlot.captured.onFailure(Exception("GPS kapalı"))
            task
        }

        // When
        val result = locationTracker.getCurrentLocation()

        // Then
        assertThat(result).isNull()
    }
}
