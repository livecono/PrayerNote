package com.prayernote.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val memo: String = "",
    val dayOfWeekAssignment: Set<Int> = emptySet(), // 0-6: 일-토, 7: 매일
    val priority: Int = 0, // For ordering
    val createdAt: Date = Date()
)
