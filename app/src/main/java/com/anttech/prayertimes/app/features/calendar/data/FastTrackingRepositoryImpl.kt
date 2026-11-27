package com.anttech.prayertimes.features.calendar.data

import com.anttech.prayertimes.core.common.hijriMonthName as hijriMonthNameFor
import com.anttech.prayertimes.core.database.FastTrackingDao
import com.anttech.prayertimes.core.database.FastTrackingEntity
import com.anttech.prayertimes.features.calendar.domain.FastTrackingDay
import com.anttech.prayertimes.features.calendar.domain.FastTrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastTrackingRepositoryImpl @Inject constructor(
    private val dao: FastTrackingDao
) : FastTrackingRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun toggleFastedDay(
        city: String,
        date: LocalDate,
        hijriDay: Int,
        hijriMonth: Int,
        hijriMonthName: String,
        hijriYear: Int
    ): Boolean {
        val dateStr = date.format(dateFormatter)
        return withContext(Dispatchers.IO) {
            val existing = dao.getTrackingForDate(city, dateStr).first()
            if (existing != null) {
                val newMarked = !existing.isMarked
                dao.insert(existing.copy(isMarked = newMarked))
                newMarked
            } else {
                val entity = FastTrackingEntity(
                    city = city,
                    date = dateStr,
                    hijriMonth = hijriMonth,
                    hijriDay = hijriDay,
                    hijriMonthName = hijriMonthName,
                    hijriYear = hijriYear,
                    isMarked = true,
                    isForbidden = false
                )
                dao.insert(entity)
                true
            }
        }
    }

    override fun getTrackingForDate(city: String, date: LocalDate): Flow<FastTrackingDay?> {
        val dateStr = date.format(dateFormatter)
        return dao.getTrackingForDate(city, dateStr).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getMonthlyTracking(city: String, yearMonth: String): Flow<List<FastTrackingDay>> {
        return dao.getMonthlyTracking(city, yearMonth).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllMarkedDays(city: String): Flow<List<FastTrackingDay>> {
        return dao.getAllMarkedDays(city).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getForbiddenDays(city: String): Flow<List<FastTrackingDay>> {
        return dao.getForbiddenDays(city).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getNonFastedRamadanDays(city: String): Flow<List<FastTrackingDay>> {
        return dao.getNonFastedRamadanDays(city).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFastedNonRamadanDays(city: String): Flow<List<FastTrackingDay>> {
        return dao.getFastedNonRamadanDays(city).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    private fun FastTrackingEntity.toDomainModel(): FastTrackingDay {
        return FastTrackingDay(
            date = LocalDate.parse(date, dateFormatter),
            hijriDay = hijriDay,
            hijriMonth = hijriMonth,
            hijriMonthName = if (hijriMonthName.isNotBlank()) hijriMonthName else hijriMonthNameFor(hijriMonth),
            hijriYear = hijriYear,
            isMarked = isMarked,
            isForbidden = isForbidden,
            city = city
        )
    }

}