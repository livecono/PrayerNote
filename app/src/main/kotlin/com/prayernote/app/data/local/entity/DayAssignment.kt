package com.prayernote.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "day_assignments",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dayOfWeek"]),
        Index(value = ["personId"]),
        Index(value = ["dayOfWeek", "personId"], unique = true)
    ]
)
data class DayAssignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val dayOfWeek: Int // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
)
