package com.prayernote.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prayernote.app.data.local.PrayerDatabase
import com.prayernote.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Person 테이블에 dayOfWeekAssignment 컬럼 추가
            db.execSQL("ALTER TABLE persons ADD COLUMN dayOfWeekAssignment TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Person 테이블에 priority 컬럼 추가
            db.execSQL("ALTER TABLE persons ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun providePrayerDatabase(
        @ApplicationContext context: Context
    ): PrayerDatabase {
        return Room.databaseBuilder(
            context,
            PrayerDatabase::class.java,
            PrayerDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePersonDao(database: PrayerDatabase): PersonDao {
        return database.personDao()
    }

    @Provides
    @Singleton
    fun providePrayerTopicDao(database: PrayerDatabase): PrayerTopicDao {
        return database.prayerTopicDao()
    }

    @Provides
    @Singleton
    fun provideDayAssignmentDao(database: PrayerDatabase): DayAssignmentDao {
        return database.dayAssignmentDao()
    }

    @Provides
    @Singleton
    fun providePrayerHistoryDao(database: PrayerDatabase): PrayerHistoryDao {
        return database.prayerHistoryDao()
    }

    @Provides
    @Singleton
    fun provideAlarmTimeDao(database: PrayerDatabase): AlarmTimeDao {
        return database.alarmTimeDao()
    }
}
