package com.prayernote.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "prayer_topics",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["personId"]),
        Index(value = ["status"]),
        Index(value = ["personId", "status"])
    ]
)
data class PrayerTopic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val title: String,
    val priority: Int = 0,
    val status: PrayerStatus = PrayerStatus.ACTIVE,
    val createdAt: Date = Date(),
    val answeredAt: Date? = null
)

enum class PrayerStatus {
    ACTIVE,
    ANSWERED
}
