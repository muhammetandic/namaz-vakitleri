package com.anttech.prayertimes.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anttech.prayertimes.core.network.AladhanApiService
import com.anttech.prayertimes.features.calendar.data.FastTrackingRepositoryImpl
import com.anttech.prayertimes.features.calendar.domain.FastTrackingRepository
import com.anttech.prayertimes.features.times.data.TimesRepositoryImpl
import com.anttech.prayertimes.features.times.domain.TimesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * v2 dropped the retired kaza-counts table. Existing installs keep their prayer times.
 * v3 adds fast_tracking table for oruç takibi.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS kaza_counts")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "namaz_vakitleri_db"
        )
            .addMigrations(MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun providePrayerTimeDao(database: AppDatabase): PrayerTimeDao {
        return database.prayerTimeDao()
    }

    @Provides
    fun provideFastTrackingDao(database: AppDatabase): FastTrackingDao {
        return database.fastTrackingDao()
    }

    @Provides
    @Singleton
    fun provideTimesRepository(
        apiService: AladhanApiService,
        dao: PrayerTimeDao
    ): TimesRepository {
        return TimesRepositoryImpl(apiService, dao)
    }

    @Provides
    @Singleton
    fun provideFastTrackingRepository(impl: FastTrackingRepositoryImpl): FastTrackingRepository {
        return impl
    }
}
