package com.anttech.prayertimes.features.times.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.anttech.prayertimes.core.database.PrayerTimeDao
import com.anttech.prayertimes.core.database.PrayerTimeEntity
import com.anttech.prayertimes.core.network.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimesRepositoryImplTest {

    private val apiService: AladhanApiService = mockk()
    private val dao: PrayerTimeDao = mockk(relaxed = true)
    private lateinit var repository: TimesRepositoryImpl

    @Before
    fun setUp() {
        repository = TimesRepositoryImpl(apiService, dao)
    }

    @Test
    fun `syncMonthlyPrayerTimes fetches from API, cleans time strings, and inserts into Room DAO`() = runTest {
        val mockApiResponse = AladhanResponse(
            code = 200,
            status = "OK",
            data = listOf(
                DayCalendarDto(
                    timings = TimingsDto(
                        fajr = "05:45 (EET)",
                        sunrise = "07:10 (EET)",
                        dhuhr = "12:50 (EET)",
                        asr = "15:40 (EET)",
                        sunset = "18:05 (EET)",
                        maghrib = "18:05 (EET)",
                        isha = "19:25 (EET)",
                        imsak = "05:45 (EET)"
                    ),
                    date = DateDto(
                        readable = "05 Nov 2026",
                        timestamp = "1793912400",
                        gregorian = GregorianDateDto(
                            date = "05-11-2026",
                            day = "05",
                            year = "2026"
                        )
                    ),
                    meta = MetaDto(method = MethodDto(id = 13, name = "Diyanet"))
                )
            )
        )

        coEvery {
            apiService.getMonthlyCalendarByCity(
                year = any(),
                month = any(),
                city = "İstanbul",
                country = "Turkey",
                method = 13
            )
        } returns mockApiResponse

        // When
        val result = repository.syncMonthlyPrayerTimes("İstanbul")

        // Then
        assertThat(result.isSuccess).isTrue()

        val capturedEntities = slot<List<PrayerTimeEntity>>()
        coVerify { dao.insertAll(capture(capturedEntities)) }

        val entity = capturedEntities.captured.first()
        assertThat(entity.city).isEqualTo("İstanbul")
        assertThat(entity.date).isEqualTo("2026-11-05")
        // Check that "(EET)" suffix was cleaned
        assertThat(entity.imsak).isEqualTo("05:45")
        assertThat(entity.gunes).isEqualTo("07:10")
        assertThat(entity.ogle).isEqualTo("12:50")
        assertThat(entity.ikindi).isEqualTo("15:40")
        assertThat(entity.aksam).isEqualTo("18:05")
        assertThat(entity.yatsi).isEqualTo("19:25")
    }

    @Test
    fun `getPrayerTimesForDate emits mapped domain PrayerTimes from DAO cache`() = runTest {
        val entity = PrayerTimeEntity(
            id = 1,
            city = "İstanbul",
            date = "2026-11-05",
            imsak = "05:45",
            gunes = "07:10",
            ogle = "12:50",
            ikindi = "15:40",
            aksam = "18:05",
            yatsi = "19:25",
            tomorrowImsak = "05:46"
        )

        every { dao.getTimesForDate("İstanbul", "2026-11-05") } returns flowOf(entity)

        repository.getPrayerTimesForDate("İstanbul", LocalDate.of(2026, 11, 5)).test {
            val domainModel = awaitItem()
            assertThat(domainModel).isNotNull()
            assertThat(domainModel?.city).isEqualTo("İstanbul")
            assertThat(domainModel?.imsak).isEqualTo(LocalTime.of(5, 45))
            assertThat(domainModel?.aksam).isEqualTo(LocalTime.of(18, 5))
            assertThat(domainModel?.tomorrowImsak).isEqualTo(LocalTime.of(5, 46))
            awaitComplete()
        }
    }

    @Test
    fun `hasMonthlyPrayerTimes reflects DAO row count`() = runTest {
        coEvery { dao.countMonthlyTimes("İstanbul", "2026-12") } returns 31
        assertThat(repository.hasMonthlyPrayerTimes("İstanbul", "2026-12")).isTrue()

        coEvery { dao.countMonthlyTimes("İstanbul", "2027-01") } returns 0
        assertThat(repository.hasMonthlyPrayerTimes("İstanbul", "2027-01")).isFalse()
    }

    @Test
    fun `syncMonthlyPrayerTimesFor requests the given month from the API`() = runTest {
        val mockApiResponse = AladhanResponse(
            code = 200,
            status = "OK",
            data = emptyList()
        )

        coEvery {
            apiService.getMonthlyCalendarByCity(
                year = 2026,
                month = 12,
                city = "İstanbul",
                country = "Turkey",
                method = 13
            )
        } returns mockApiResponse

        val result = repository.syncMonthlyPrayerTimesFor("İstanbul", "2026-12")

        assertThat(result.isSuccess).isTrue()
        coVerify {
            apiService.getMonthlyCalendarByCity(
                year = 2026,
                month = 12,
                city = "İstanbul",
                country = "Turkey",
                method = 13
            )
        }
    }

    @Test
    fun `getCachedCity returns last used city from DAO`() = runTest {
        coEvery { dao.getLastUsedCity() } returns "Artvin, Hopa"
        assertThat(repository.getCachedCity()).isEqualTo("Artvin, Hopa")

        coEvery { dao.getLastUsedCity() } returns null
        assertThat(repository.getCachedCity()).isNull()
    }
}
