package com.prayernote.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prayernote.app.data.local.dao.*
import com.prayernote.app.data.local.entity.*

@Database(
    entities = [
        Person::class,
        PrayerTopic::class,
        DayAssignment::class,
        PrayerHistory::class,
        AlarmTime::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun prayerTopicDao(): PrayerTopicDao
    abstract fun dayAssignmentDao(): DayAssignmentDao
    abstract fun prayerHistoryDao(): PrayerHistoryDao
    abstract fun alarmTimeDao(): AlarmTimeDao

    companion object {
        const val DATABASE_NAME = "prayer_note_database"
    }
}
