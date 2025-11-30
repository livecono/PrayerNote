package com.prayernote.app.data.local

import androidx.room.TypeConverter
import com.prayernote.app.data.local.entity.PrayerStatus
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromPrayerStatus(status: PrayerStatus): String {
        return status.name
    }

    @TypeConverter
    fun toPrayerStatus(status: String): PrayerStatus {
        return PrayerStatus.valueOf(status)
    }

    @TypeConverter
    fun fromIntSet(value: Set<Int>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toIntSet(value: String): Set<Int> {
        return if (value.isEmpty()) emptySet()
        else value.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
}
