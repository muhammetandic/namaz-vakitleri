package com.anttech.prayertimes.features.qibla.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class CalculateQiblaAngleUseCaseTest {

    private lateinit var useCase: CalculateQiblaAngleUseCase

    @Before
    fun setUp() {
        useCase = CalculateQiblaAngleUseCase()
    }

    @Test
    fun `calculateQiblaBearing for Istanbul returns approximately 151 degrees`() {
        // Istanbul coordinates: 41.0082° N, 28.9784° E
        val istanbulLat = 41.0082
        val istanbulLng = 28.9784

        val bearing = useCase.calculateQiblaBearing(istanbulLat, istanbulLng)

        // Expected qibla angle from Istanbul is ~151.7 degrees
        assertThat(bearing).isWithin(1.5f).of(151.7f)
    }

    @Test
    fun `calculateQiblaBearing for Ankara returns approximately 161 degrees`() {
        // Ankara coordinates: 39.9334° N, 32.8597° E
        val ankaraLat = 39.9334
        val ankaraLng = 32.8597

        val bearing = useCase.calculateQiblaBearing(ankaraLat, ankaraLng)

        // Expected qibla angle from Ankara is ~161.4 degrees
        assertThat(bearing).isWithin(1.5f).of(161.4f)
    }

    @Test
    fun `calculateNeedleAngle computes correct offset from device azimuth`() {
        val qiblaBearing = 150f

        // When device is pointing North (azimuth = 0)
        val needleAtNorth = useCase.calculateNeedleAngle(qiblaBearing, currentAzimuth = 0f)
        assertThat(needleAtNorth).isEqualTo(150f)

        // When device is pointing East (azimuth = 90)
        val needleAtEast = useCase.calculateNeedleAngle(qiblaBearing, currentAzimuth = 90f)
        assertThat(needleAtEast).isEqualTo(60f)

        // When device is pointing directly to Qibla (azimuth = 150)
        val needleAtQibla = useCase.calculateNeedleAngle(qiblaBearing, currentAzimuth = 150f)
        assertThat(needleAtQibla).isEqualTo(0f)
    }

    @Test
    fun `isAlignedWithQibla returns true within 3 degrees tolerance`() {
        val qiblaBearing = 150f

        // Exactly aligned
        assertThat(useCase.isAlignedWithQibla(qiblaBearing, currentAzimuth = 150f)).isTrue()

        // Within tolerance (152 degrees)
        assertThat(useCase.isAlignedWithQibla(qiblaBearing, currentAzimuth = 152f)).isTrue()

        // Within tolerance (148 degrees)
        assertThat(useCase.isAlignedWithQibla(qiblaBearing, currentAzimuth = 148f)).isTrue()

        // Far off
        assertThat(useCase.isAlignedWithQibla(qiblaBearing, currentAzimuth = 90f)).isFalse()
    }
}
