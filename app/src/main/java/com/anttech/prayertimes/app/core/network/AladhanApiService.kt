package com.anttech.prayertimes.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class AladhanResponse(
    val code: Int,
    val status: String,
    val data: List<DayCalendarDto>
)

data class DayCalendarDto(
    val timings: TimingsDto,
    val date: DateDto,
    val meta: MetaDto
)

data class TimingsDto(
    @SerializedName("Fajr") val fajr: String,
    @SerializedName("Sunrise") val sunrise: String,
    @SerializedName("Dhuhr") val dhuhr: String,
    @SerializedName("Asr") val asr: String,
    @SerializedName("Sunset") val sunset: String,
    @SerializedName("Maghrib") val maghrib: String,
    @SerializedName("Isha") val isha: String,
    @SerializedName("Imsak") val imsak: String
)

data class DateDto(
    val readable: String,
    val timestamp: String,
    val gregorian: GregorianDateDto,
    val hijri: HijriDateDto? = null
)

data class GregorianDateDto(
    val date: String, // DD-MM-YYYY
    val day: String,
    val year: String
)

data class HijriDateDto(
    val date: String,
    val day: String,
    val year: String,
    val month: HijriMonthDto? = null
)

data class HijriMonthDto(
    val number: Int,
    val en: String,
    val ar: String
)

data class MetaDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val method: MethodDto? = null
)

data class MethodDto(
    val id: Int,
    val name: String
)

interface AladhanApiService {

    /**
     * Aladhan API Diyanet İşleri Başkanlığı yöntemi ile aylık namaz takvimini çeker.
     * @param method 13: Diyanet İşleri Başkanlığı
     */
    @GET("v1/calendarByCity/{year}/{month}")
    suspend fun getMonthlyCalendarByCity(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("city") city: String,
        @Query("country") country: String = "Turkey",
        @Query("method") method: Int = 13
    ): AladhanResponse

    /**
     * GPS koordinatları ile aylık namaz takvimini çeker.
     */
    @GET("v1/calendar/{year}/{month}")
    suspend fun getMonthlyCalendarByCoordinates(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 13
    ): AladhanResponse
}
