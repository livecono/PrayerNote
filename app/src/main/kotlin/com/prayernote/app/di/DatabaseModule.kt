package com.prayernote.app.di

import android.content.Context
import androidx.room.Room
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
