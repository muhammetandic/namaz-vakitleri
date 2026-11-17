package com.anttech.prayertimes.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local storage. Only prayer times are persisted; the dhikr counter is session-only
 * and the app has no kaza (missed-prayer) tracking.
 */
@Database(
    entities = [PrayerTimeEntity::class, FastTrackingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
    abstract fun fastTrackingDao(): FastTrackingDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migrate existing prayer_times: preserve data, add Hijri columns
                // Since Room doesn't always handle ALTER TABLE well, we recreate the table
                database.execSQL("""
                    CREATE TABLE prayer_times_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        city TEXT NOT NULL,
                        date TEXT NOT NULL,
                        imsak TEXT NOT NULL,
                        gunes TEXT NOT NULL,
                        ogle TEXT NOT NULL,
                        ikindi TEXT NOT NULL,
                        aksam TEXT NOT NULL,
                        yatsi TEXT NOT NULL,
                        tomorrowImsak TEXT,
                        hijriDay INTEGER NOT NULL DEFAULT 1,
                        hijriMonth INTEGER NOT NULL DEFAULT 1,
                        hijriMonthName TEXT NOT NULL DEFAULT '',
                        hijriYear INTEGER NOT NULL DEFAULT 1448
                    )
                """.trimIndent())

                // Copy existing data to new table
                database.execSQL("""
                    INSERT INTO prayer_times_new (id, city, date, imsak, gunes, ogle, ikindi, aksam, yatsi, tomorrowImsak, hijriDay, hijriMonth, hijriMonthName, hijriYear)
                    SELECT id, city, date, imsak, gunes, ogle, ikindi, aksam, yatsi, tomorrowImsak, 1, 1, '', 1448
                    FROM prayer_times
                """.trimIndent())

                // Drop old table and rename
                database.execSQL("DROP TABLE prayer_times")
                database.execSQL("ALTER TABLE prayer_times_new RENAME TO prayer_times")

                // Create index on city+date (matching entity @Index)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_prayer_times_city_date ON prayer_times (city, date)")

                // Create fast_tracking table (matching FastTrackingEntity exactly)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS fast_tracking (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        city TEXT NOT NULL,
                        date TEXT NOT NULL,
                        hijriMonth INTEGER NOT NULL,
                        hijriDay INTEGER NOT NULL,
                        hijriMonthName TEXT NOT NULL DEFAULT '',
                        hijriYear INTEGER NOT NULL DEFAULT 1448,
                        isMarked INTEGER NOT NULL DEFAULT 0,
                        isForbidden INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_fast_tracking_city_date ON fast_tracking (city, date)")
            }
        }
    }
}
