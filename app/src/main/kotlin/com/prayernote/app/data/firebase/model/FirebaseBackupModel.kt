package com.prayernote.app.data.firebase.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firebase Firestore 백업 모델들
 */

// 대상자 백업 데이터 모델
data class PersonBackup(
    @DocumentId
    val id: String = "",
    val originalId: Long = 0,
    val name: String = "",
    val memo: String = "",
    val dayOfWeekAssignment: List<Int> = emptyList(),
    val createdAt: Date? = null,
    @ServerTimestamp
    val backupTimestamp: Date? = null
)

// 기도제목 백업 데이터 모델
data class PrayerTopicBackup(
    @DocumentId
    val id: String = "",
    val originalId: Long = 0,
    val personId: Long = 0,
    val personBackupId: String = "",
    val title: String = "",
    val priority: Int = 0,
    val status: String = "ACTIVE",
    val createdAt: Date? = null,
    val answeredAt: Date? = null,
    @ServerTimestamp
    val backupTimestamp: Date? = null
)

// 백업 세션 정보
data class BackupSession(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val personBackupIds: List<String> = emptyList(),
    val topicBackupIds: List<String> = emptyList(),
    val totalPersons: Int = 0,
    val totalTopics: Int = 0,
    @ServerTimestamp
    val timestamp: Date? = null,
    val status: String = "COMPLETED" // PENDING, COMPLETED, FAILED
)

// 백업 통계
data class BackupStatistics(
    val totalPersonsBackedUp: Int = 0,
    val totalTopicsBackedUp: Int = 0,
    val lastBackupTime: Date? = null,
    val selectedPersonIds: List<Long> = emptyList(),
    val selectedTopicIds: List<Long> = emptyList()
)
