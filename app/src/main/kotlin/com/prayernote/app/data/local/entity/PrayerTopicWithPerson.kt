package com.prayernote.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PrayerTopicWithPerson(
    @Embedded val prayerTopic: PrayerTopic,
    @Relation(
        parentColumn = "personId",
        entityColumn = "id"
    )
    val person: Person
)
