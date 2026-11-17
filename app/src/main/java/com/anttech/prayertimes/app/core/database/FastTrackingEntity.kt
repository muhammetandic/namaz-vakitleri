package com.anttech.prayertimes.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks fasting status for a given day.
 * Semantics depend on month:
 * - Ramadan (month 9 in Hijri): true = did NOT fast (makeup needed)
 * - Other months: true = DID fast (nafl/sunnah fasting)
 * - Forbidden days: Eid al-Fitr, Eid al-Adha, Days of Tashriq (11-13 Dhul Hijjah)
 */
@Entity(
    tableName = "fast_tracking",
    indices = [Index(value = ["city", "date"], unique = true)]
)
data class FastTrackingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val city: String,
    val date: String, // Format: YYYY-MM-DD
    val hijriMonth: Int, // 1-12 (Hijri month number)
    val hijriDay: Int, // 1-30 (Hijri day of month)
    val hijriMonthName: String = "", // Arabic/Turkish name
    val hijriYear: Int = 1448,
    val isMarked: Boolean, // true = not fasted (Ramadan) OR fasted (other months)
    val isForbidden: Boolean = false // Eid days, Tashriq days
)