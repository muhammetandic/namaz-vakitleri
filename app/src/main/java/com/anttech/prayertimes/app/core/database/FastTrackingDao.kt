package com.anttech.prayertimes.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastTrackingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracking: List<FastTrackingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tracking: FastTrackingEntity)

    @Query("SELECT * FROM fast_tracking WHERE city = :city AND date = :date LIMIT 1")
    fun getTrackingForDate(city: String, date: String): Flow<FastTrackingEntity?>

    @Query("SELECT * FROM fast_tracking WHERE city = :city AND date LIKE :yearMonth || '%' ORDER BY date ASC")
    fun getMonthlyTracking(city: String, yearMonth: String): Flow<List<FastTrackingEntity>>

    @Query("SELECT * FROM fast_tracking WHERE city = :city AND isMarked = 1 ORDER BY date ASC")
    fun getAllMarkedDays(city: String): Flow<List<FastTrackingEntity>>

    @Query("SELECT * FROM fast_tracking WHERE city = :city AND isForbidden = 1 ORDER BY date ASC")
    fun getForbiddenDays(city: String): Flow<List<FastTrackingEntity>>

    @Query("SELECT * FROM fast_tracking WHERE city = :city AND hijriMonth = 9 AND isMarked = 1 ORDER BY date ASC")
    fun getNonFastedRamadanDays(city: String): Flow<List<FastTrackingEntity>>

    @Query("SELECT * FROM fast_tracking WHERE city = :city AND hijriMonth != 9 AND isMarked = 1 ORDER BY date ASC")
    fun getFastedNonRamadanDays(city: String): Flow<List<FastTrackingEntity>>

    @Query("DELETE FROM fast_tracking WHERE city = :city AND date = :date")
    suspend fun deleteTracking(city: String, date: String)
}