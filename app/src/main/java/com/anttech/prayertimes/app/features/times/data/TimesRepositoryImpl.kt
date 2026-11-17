package com.anttech.prayertimes.features.times.data

import com.anttech.prayertimes.core.database.PrayerTimeDao
import com.anttech.prayertimes.core.database.PrayerTimeEntity
import com.anttech.prayertimes.core.network.AladhanApiService
import com.anttech.prayertimes.core.network.DayCalendarDto
import com.anttech.prayertimes.core.network.HijriDateDto
import com.anttech.prayertimes.core.network.HijriMonthDto
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.TimesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimesRepositoryImpl(
    private val apiService: AladhanApiService,
    private val dao: PrayerTimeDao
) : TimesRepository {

    override fun getPrayerTimesForDate(city: String, date: LocalDate): Flow<PrayerTimes?> {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE) // YYYY-MM-DD
        return dao.getTimesForDate(city, dateString).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getMonthlyPrayerTimes(city: String, yearMonth: String): Flow<List<PrayerTimes>> {
        return dao.getMonthlyTimes(city, yearMonth).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun syncMonthlyPrayerTimes(city: String): Result<Unit> {
        val today = LocalDate.now()
        val yearMonth = "%04d-%02d".format(today.year, today.monthValue)
        return syncMonthlyPrayerTimesFor(city, yearMonth)
    }

    override suspend fun syncMonthlyPrayerTimesFor(city: String, yearMonth: String): Result<Unit> {
        return runCatching {
            // yearMonth biçimi: "YYYY-MM"
            val parts = yearMonth.split("-")
            require(parts.size == 2) { "Geçersiz ay biçimi: $yearMonth" }
            val year = parts[0].toInt()
            val month = parts[1].toInt()

            val response = apiService.getMonthlyCalendarByCity(
                year = year,
                month = month,
                city = city,
                country = "Turkey",
                method = 13 // Diyanet İşleri Başkanlığı yöntemi
            )

            if (response.code != 200) {
                error("API hatası: ${response.status}")
            }

            val entities = response.data.mapIndexed { index, dayDto ->
                val nextDayImsak = if (index + 1 < response.data.size) {
                    cleanTimeString(response.data[index + 1].timings.imsak)
                } else null

                dayDto.toEntity(city = city, tomorrowImsak = nextDayImsak)
            }

            dao.insertAll(entities)
        }
    }

    override suspend fun hasMonthlyPrayerTimes(city: String, yearMonth: String): Boolean {
        return dao.countMonthlyTimes(city, yearMonth) > 0
    }

    override suspend fun getCachedCity(): String? {
        return dao.getLastUsedCity()
    }

    private fun DayCalendarDto.toEntity(city: String, tomorrowImsak: String?): PrayerTimeEntity {
        // Aladhan DD-MM-YYYY formatını YYYY-MM-DD standardına çevir
        val parts = date.gregorian.date.split("-")
        val isoDate = if (parts.size == 3) {
            "${parts[2]}-${parts[1]}-${parts[0]}"
        } else {
            date.gregorian.date
        }

        // Extract Hijri date - use Turkish month names
        val hijri = date.hijri
        val hijriDay = hijri?.day?.toInt() ?: 1
        val hijriMonth = hijri?.month?.number ?: 1
        val hijriMonthName = hijriMonthTurkish(hijriMonth)
        val hijriYear = hijri?.year?.toInt() ?: 1448

        return PrayerTimeEntity(
            city = city,
            date = isoDate,
            imsak = cleanTimeString(timings.imsak),
            gunes = cleanTimeString(timings.sunrise),
            ogle = cleanTimeString(timings.dhuhr),
            ikindi = cleanTimeString(timings.asr),
            aksam = cleanTimeString(timings.maghrib),
            yatsi = cleanTimeString(timings.isha),
            tomorrowImsak = tomorrowImsak,
            hijriDay = hijriDay,
            hijriMonth = hijriMonth,
            hijriMonthName = hijriMonthName,
            hijriYear = hijriYear
        )
    }

    private fun PrayerTimeEntity.toDomainModel(): PrayerTimes {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return PrayerTimes(
            city = city,
            date = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE),
            imsak = LocalTime.parse(imsak, timeFormatter),
            gunes = LocalTime.parse(gunes, timeFormatter),
            ogle = LocalTime.parse(ogle, timeFormatter),
            ikindi = LocalTime.parse(ikindi, timeFormatter),
            aksam = LocalTime.parse(aksam, timeFormatter),
            yatsi = LocalTime.parse(yatsi, timeFormatter),
            tomorrowImsak = tomorrowImsak?.let { LocalTime.parse(it, timeFormatter) },
            hijriDay = hijriDay,
            hijriMonth = hijriMonth,
            hijriMonthName = hijriMonthName,
            hijriYear = hijriYear
        )
    }

    private fun hijriMonthTurkish(month: Int): String = when (month) {
        1 -> "Muharrem"
        2 -> "Safer"
        3 -> "Rebiülevvel"
        4 -> "Rebiülahir"
        5 -> "Cemaziyelevvel"
        6 -> "Cemaziyelahir"
        7 -> "Recep"
        8 -> "Şaban"
        9 -> "Ramazan"
        10 -> "Şevval"
        11 -> "Zilkade"
        12 -> "Zilhicce"
        else -> ""
    }

    private fun cleanTimeString(raw: String): String {
        // "05:45 (EET)" -> "05:45"
        return raw.trim().split(" ")[0]
    }
}
