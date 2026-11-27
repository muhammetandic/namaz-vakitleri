package com.anttech.prayertimes.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(times: List<PrayerTimeEntity>)

    @Query("SELECT * FROM prayer_times WHERE city = :city AND date = :date LIMIT 1")
    fun getTimesForDate(city: String, date: String): Flow<PrayerTimeEntity?>

    @Query("SELECT * FROM prayer_times WHERE city = :city AND date LIKE :yearMonth || '%' ORDER BY date ASC")
    fun getMonthlyTimes(city: String, yearMonth: String): Flow<List<PrayerTimeEntity>>

    @Query("SELECT * FROM prayer_times WHERE city = :city AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getTimesBetween(city: String, startDate: String, endDate: String): List<PrayerTimeEntity>

    @Query("SELECT COUNT(*) FROM prayer_times WHERE city = :city AND date LIKE :yearMonth || '%'")
    suspend fun countMonthlyTimes(city: String, yearMonth: String): Int

    @Query("SELECT city FROM prayer_times ORDER BY date DESC LIMIT 1")
    suspend fun getLastUsedCity(): String?
}
