package com.anttech.prayertimes.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_times",
    indices = [Index(value = ["city", "date"], unique = true)]
)
data class PrayerTimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val city: String,
    val date: String, // Format: YYYY-MM-DD
    val imsak: String,
    val gunes: String,
    val ogle: String,
    val ikindi: String,
    val aksam: String,
    val yatsi: String,
    val tomorrowImsak: String? = null,
    // Hijri date fields
    val hijriDay: Int = 1,
    val hijriMonth: Int = 1,
    val hijriMonthName: String = "",
    val hijriYear: Int = 1448
)
