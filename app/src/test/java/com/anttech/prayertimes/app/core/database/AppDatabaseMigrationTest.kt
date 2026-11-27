package com.anttech.prayertimes.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.util.TableInfo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private fun openInMemoryDatabase(version: Int): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }

    @Test
    fun `migration 2 to 3 preserves rows and adds hijri defaults`() {
        val db = openInMemoryDatabase(version = 2)
        db.execSQL(
            """
            CREATE TABLE prayer_times (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                city TEXT NOT NULL,
                date TEXT NOT NULL,
                imsak TEXT NOT NULL,
                gunes TEXT NOT NULL,
                ogle TEXT NOT NULL,
                ikindi TEXT NOT NULL,
                aksam TEXT NOT NULL,
                yatsi TEXT NOT NULL,
                tomorrowImsak TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_prayer_times_city_date ON prayer_times (city, date)")
        db.execSQL(
            """
            INSERT INTO prayer_times (city, date, imsak, gunes, ogle, ikindi, aksam, yatsi)
            VALUES ('İstanbul', '2026-01-01', '05:45', '07:10', '12:50', '15:40', '18:05', '19:25')
            """.trimIndent()
        )

        AppDatabase.MIGRATION_2_3.migrate(db)

        db.query(
            "SELECT city, imsak, hijriDay, hijriMonth, hijriYear FROM prayer_times"
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("İstanbul")
            assertThat(cursor.getString(1)).isEqualTo("05:45")
            assertThat(cursor.getInt(2)).isEqualTo(1)
            assertThat(cursor.getInt(3)).isEqualTo(1)
            assertThat(cursor.getInt(4)).isEqualTo(1448)
        }

        // Yeni tablo oluşturuldu ve kullanılabilir.
        db.execSQL(
            """
            INSERT INTO fast_tracking
                (city, date, hijriMonth, hijriDay, hijriMonthName, hijriYear, isMarked, isForbidden)
            VALUES ('İstanbul', '2026-01-01', 7, 12, 'Recep', 1448, 1, 0)
            """.trimIndent()
        )
        db.query("SELECT COUNT(*) FROM fast_tracking").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun `migrated tables match the room generated schema`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val expectedPrayer = TableInfo.read(expectedDb.openHelper.writableDatabase, "prayer_times")
        val expectedTracking = TableInfo.read(expectedDb.openHelper.writableDatabase, "fast_tracking")

        val migratedDb = openInMemoryDatabase(version = 2)
        migratedDb.execSQL(
            """
            CREATE TABLE prayer_times (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                city TEXT NOT NULL,
                date TEXT NOT NULL,
                imsak TEXT NOT NULL,
                gunes TEXT NOT NULL,
                ogle TEXT NOT NULL,
                ikindi TEXT NOT NULL,
                aksam TEXT NOT NULL,
                yatsi TEXT NOT NULL,
                tomorrowImsak TEXT
            )
            """.trimIndent()
        )
        migratedDb.execSQL("CREATE UNIQUE INDEX index_prayer_times_city_date ON prayer_times (city, date)")
        AppDatabase.MIGRATION_2_3.migrate(migratedDb)

        assertThat(TableInfo.read(migratedDb, "prayer_times")).isEqualTo(expectedPrayer)
        assertThat(TableInfo.read(migratedDb, "fast_tracking")).isEqualTo(expectedTracking)

        expectedDb.close()
        migratedDb.close()
    }

    @Test
    fun `migration 1 to 2 drops legacy kaza table`() {
        val db = openInMemoryDatabase(version = 1)
        db.execSQL(
            """
            CREATE TABLE prayer_times (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                city TEXT NOT NULL,
                date TEXT NOT NULL,
                imsak TEXT NOT NULL,
                gunes TEXT NOT NULL,
                ogle TEXT NOT NULL,
                ikindi TEXT NOT NULL,
                aksam TEXT NOT NULL,
                yatsi TEXT NOT NULL,
                tomorrowImsak TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE kaza_counts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                prayerType TEXT NOT NULL,
                count INTEGER NOT NULL
            )
            """.trimIndent()
        )

        AppDatabase.MIGRATION_1_2.migrate(db)

        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'kaza_counts'"
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(0)
        }
        db.query("SELECT COUNT(*) FROM prayer_times").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(0)
        }
    }
}
