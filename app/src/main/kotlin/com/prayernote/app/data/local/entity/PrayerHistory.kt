package com.prayernote.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "prayer_history",
    foreignKeys = [
        ForeignKey(
            entity = PrayerTopic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId"]),
        Index(value = ["personId"]),
        Index(value = ["prayedAt"]),
        Index(value = ["topicId", "prayedAt"])
    ]
)
data class PrayerHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long,
    val personId: Long,
    val prayedAt: Date = Date()
)
