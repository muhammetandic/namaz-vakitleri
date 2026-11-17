package com.anttech.prayertimes.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class PrayerTimeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: PrayerTimeDao

    @Before
    fun initDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.prayerTimeDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrievePrayerTimesByCityAndDate() = runTest {
        val entity = PrayerTimeEntity(
            city = "İstanbul",
            date = "2026-11-05",
            imsak = "05:45",
            gunes = "07:10",
            ogle = "12:50",
            ikindi = "15:40",
            aksam = "18:05",
            yatsi = "19:25",
            tomorrowImsak = "05:46"
        )

        dao.insertAll(listOf(entity))

        dao.getTimesForDate("İstanbul", "2026-11-05").test {
            val retrieved = awaitItem()
            assertThat(retrieved).isNotNull()
            assertThat(retrieved?.city).isEqualTo("İstanbul")
            assertThat(retrieved?.imsak).isEqualTo("05:45")
            assertThat(retrieved?.aksam).isEqualTo("18:05")
        }
    }

    @Test
    fun retrieveNullWhenDateDoesNotExist() = runTest {
        dao.getTimesForDate("Ankara", "2026-11-05").test {
            val result = awaitItem()
            assertThat(result).isNull()
        }
    }
}
