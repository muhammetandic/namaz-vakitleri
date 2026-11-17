package com.anttech.prayertimes.core.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AladhanApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: AladhanApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AladhanApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getMonthlyCalendarByCity sends request with method 13 for Diyanet and parses timings correctly`() = runTest {
        val mockJsonResponse = """
        {
          "code": 200,
          "status": "OK",
          "data": [
            {
              "timings": {
                "Fajr": "05:45 (EET)",
                "Sunrise": "07:10 (EET)",
                "Dhuhr": "12:50 (EET)",
                "Asr": "15:40 (EET)",
                "Sunset": "18:05 (EET)",
                "Maghrib": "18:05 (EET)",
                "Isha": "19:25 (EET)",
                "Imsak": "05:45 (EET)"
              },
              "date": {
                "readable": "05 Nov 2026",
                "timestamp": "1793912400",
                "gregorian": {
                  "date": "05-11-2026",
                  "day": "05",
                  "year": "2026"
                }
              },
              "meta": {
                "method": {
                  "id": 13,
                  "name": "Diyanet İşleri Başkanlığı, Turkey"
                }
              }
            }
          ]
        }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockJsonResponse)
        )

        val response = apiService.getMonthlyCalendarByCity(
            city = "Istanbul",
            country = "Turkey",
            year = 2026,
            month = 11,
            method = 13 // Diyanet İşleri Başkanlığı method ID
        )

        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.path).contains("method=13")
        assertThat(recordedRequest.path).contains("city=Istanbul")
        assertThat(recordedRequest.path).contains("country=Turkey")

        assertThat(response.code).isEqualTo(200)
        assertThat(response.data).hasSize(1)

        val dayData = response.data[0]
        assertThat(dayData.timings.fajr).contains("05:45")
        assertThat(dayData.timings.sunrise).contains("07:10")
        assertThat(dayData.timings.dhuhr).contains("12:50")
        assertThat(dayData.timings.asr).contains("15:40")
        assertThat(dayData.timings.maghrib).contains("18:05")
        assertThat(dayData.timings.isha).contains("19:25")
        assertThat(dayData.date.gregorian.date).isEqualTo("05-11-2026")
    }
}
