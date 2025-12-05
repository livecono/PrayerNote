package com.prayernote.app.data.firebase.service

import android.util.Log
import android.provider.Settings
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prayernote.app.data.firebase.model.BackupSession
import com.prayernote.app.data.firebase.model.PersonBackup
import com.prayernote.app.data.firebase.model.PrayerTopicBackup
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.domain.repository.PrayerRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class FirebaseBackupService @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val auth: FirebaseAuth?,
    @ApplicationContext private val context: Context,
    private val prayerRepository: PrayerRepository
) {
    companion object {
        private const val TAG = "FirebaseBackupService"
        private const val BACKUP_COLLECTION = "backups"
        private const val PERSONS_SUBCOLLECTION = "persons"
        private const val TOPICS_SUBCOLLECTION = "topics"
        private const val SESSIONS_SUBCOLLECTION = "sessions"
    }

    /**
     * 선택된 대상자와 기도제목을 Firebase에 백업
     */
    suspend fun backupSelectedData(
        persons: List<Person>,
        topics: List<PrayerTopic>,
        includeAnswered: Boolean = false
    ): Result<String> = try {
        Log.d(TAG, "backupSelectedData called - firestore: $firestore, auth: $auth")
        
        if (firestore == null) {
            Log.e(TAG, "Firestore is null - Firebase not properly initialized")
            throw Exception("Firebase가 초기화되지 않았습니다")
        }

        val userId = getCurrentUserId()
        Log.d(TAG, "Current userId: $userId")
        
        if (userId.isBlank()) {
            throw Exception("사용자가 로그인하지 않았습니다")
        }

        val backupId = UUID.randomUUID().toString()
        val backupSession = BackupSession(
            userId = userId,
            totalPersons = persons.size,
            totalTopics = topics.size
        )

        val personBackupIds = mutableListOf<String>()
        val topicBackupIds = mutableListOf<String>()

        // 대상자 백업
        for (person in persons) {
            val personBackupId = backupPerson(userId, backupId, person)
            personBackupIds.add(personBackupId)
        }

        // 기도제목 백업 (선택된 대상자의 기도제목만)
        val selectedPersonIds = persons.map { it.id }
        val filteredTopics = topics.filter { it.personId in selectedPersonIds }

        for (topic in filteredTopics) {
            val topicBackupId = backupPrayerTopic(userId, backupId, topic)
            topicBackupIds.add(topicBackupId)
        }

        // 백업 세션 정보 저장
        val sessionWithIds = backupSession.copy(
            personBackupIds = personBackupIds,
            topicBackupIds = topicBackupIds
        )
        saveBackupSession(userId, backupId, sessionWithIds)

        Log.d(TAG, "Backup completed: $backupId. Persons: ${personBackupIds.size}, Topics: ${topicBackupIds.size}")
        Result.success(backupId)
    } catch (e: Exception) {
        Log.e(TAG, "Backup failed", e)
        Result.failure(e)
    }

    /**
     * 개별 대상자 백업
     */
    private suspend fun backupPerson(userId: String, backupId: String, person: Person): String {
        val personDocId = UUID.randomUUID().toString()
        val personBackup = PersonBackup(
            id = personDocId,
            originalId = person.id,
            name = person.name,
            memo = person.memo,
            dayOfWeekAssignment = person.dayOfWeekAssignment.toList(),
            createdAt = person.createdAt
        )

        firestore
            ?.collection(BACKUP_COLLECTION)
            ?.document(userId)
            ?.collection(SESSIONS_SUBCOLLECTION)
            ?.document(backupId)
            ?.collection(PERSONS_SUBCOLLECTION)
            ?.document(personDocId)
            ?.set(personBackup)
            ?.await()

        Log.d(TAG, "Person backed up: ${person.name}")
        return personDocId
    }

    /**
     * 개별 기도제목 백업
     */
    private suspend fun backupPrayerTopic(
        userId: String,
        backupId: String,
        topic: PrayerTopic
    ): String {
        val topicDocId = UUID.randomUUID().toString()
        val topicBackup = PrayerTopicBackup(
            id = topicDocId,
            originalId = topic.id,
            personId = topic.personId,
            title = topic.title,
            priority = topic.priority,
            status = topic.status.name,
            createdAt = topic.createdAt,
            answeredAt = topic.answeredAt
        )

        firestore
            ?.collection(BACKUP_COLLECTION)
            ?.document(userId)
            ?.collection(SESSIONS_SUBCOLLECTION)
            ?.document(backupId)
            ?.collection(TOPICS_SUBCOLLECTION)
            ?.document(topicDocId)
            ?.set(topicBackup)
            ?.await()

        Log.d(TAG, "Prayer topic backed up: ${topic.title}")
        return topicDocId
    }

    /**
     * 백업 세션 정보 저장
     */
    private suspend fun saveBackupSession(
        userId: String,
        backupId: String,
        session: BackupSession
    ) {
        firestore
            ?.collection(BACKUP_COLLECTION)
            ?.document(userId)
            ?.collection(SESSIONS_SUBCOLLECTION)
            ?.document(backupId)
            ?.set(session)
            ?.await()
    }

    /**
     * 백업된 데이터 조회
     */
    suspend fun getBackupSessions(): Result<List<BackupSession>> = try {
        if (firestore == null) {
            throw Exception("Firebase가 초기화되지 않았습니다")
        }

        val userId = getCurrentUserId()
        if (userId.isBlank()) {
            throw Exception("사용자가 로그인하지 않았습니다")
        }

        val sessions = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
            .toObjects(BackupSession::class.java)

        Result.success(sessions)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get backup sessions", e)
        Result.failure(e)
    }

    /**
     * 특정 백업 세션의 대상자 데이터 조회
     */
    suspend fun getBackupPersons(backupId: String): Result<List<PersonBackup>> = try {
        if (firestore == null) {
            throw Exception("Firebase가 초기화되지 않았습니다")
        }

        val userId = getCurrentUserId()
        if (userId.isBlank()) {
            throw Exception("사용자가 로그인하지 않았습니다")
        }

        val persons = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .collection(PERSONS_SUBCOLLECTION)
            .get()
            .await()
            .toObjects(PersonBackup::class.java)

        Result.success(persons)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get backup persons", e)
        Result.failure(e)
    }

    /**
     * 특정 백업 세션의 기도제목 데이터 조회
     */
    suspend fun getBackupTopics(backupId: String): Result<List<PrayerTopicBackup>> = try {
        if (firestore == null) {
            throw Exception("Firebase가 초기화되지 않았습니다")
        }

        val userId = getCurrentUserId()
        if (userId.isBlank()) {
            throw Exception("사용자가 로그인하지 않았습니다")
        }

        val topics = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .collection(TOPICS_SUBCOLLECTION)
            .get()
            .await()
            .toObjects(PrayerTopicBackup::class.java)

        Result.success(topics)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get backup topics", e)
        Result.failure(e)
    }

    /**
     * 백업 세션 삭제
     */
    suspend fun deleteBackupSession(backupId: String): Result<Unit> = try {
        if (firestore == null) {
            throw Exception("Firebase가 초기화되지 않았습니다")
        }

        val userId = getCurrentUserId()
        if (userId.isBlank()) {
            throw Exception("사용자가 로그인하지 않았습니다")
        }

        // 대상자 컬렉션의 모든 문서 삭제
        val personDocs = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .collection(PERSONS_SUBCOLLECTION)
            .get()
            .await()

        for (doc in personDocs) {
            doc.reference.delete().await()
        }

        // 기도제목 컬렉션의 모든 문서 삭제
        val topicDocs = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .collection(TOPICS_SUBCOLLECTION)
            .get()
            .await()

        for (doc in topicDocs) {
            doc.reference.delete().await()
        }

        // 세션 문서 삭제
        firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .delete()
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete backup session", e)
        Result.failure(e)
    }

    /**
     * 사용자 ID 반환 (Device ID 또는 Firebase Auth UID)
     */
    private fun getCurrentUserId(): String {
        return try {
            // 먼저 Firebase Auth 사용자 확인
            val authUser = auth?.currentUser?.uid
            if (!authUser.isNullOrBlank()) {
                Log.d(TAG, "Using Firebase Auth UID: $authUser")
                return authUser
            }

            // Firebase Auth가 없으면 Device ID 사용
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            Log.d(TAG, "Using Device ID: $deviceId")
            deviceId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user ID: ${e.message}")
            "unknown"
        }
    }

    /**
     * 백업된 데이터 복원
     */
    suspend fun restoreBackupData(backupId: String): Result<String> = try {
        if (firestore == null) {
            throw Exception("Firebase가 초기화되지 않았습니다")
        }

        val userId = getCurrentUserId()
        if (userId.isBlank()) {
            throw Exception("사용자가 로그인하지 않았습니다")
        }

        Log.d(TAG, "Starting restore for backup: $backupId")

        // 백업된 대상자 데이터 조회
        val backupPersons = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .collection(PERSONS_SUBCOLLECTION)
            .get()
            .await()
            .toObjects(PersonBackup::class.java)

        Log.d(TAG, "Retrieved ${backupPersons.size} persons from backup")

        // 백업된 기도제목 데이터 조회
        val backupTopics = firestore
            .collection(BACKUP_COLLECTION)
            .document(userId)
            .collection(SESSIONS_SUBCOLLECTION)
            .document(backupId)
            .collection(TOPICS_SUBCOLLECTION)
            .get()
            .await()
            .toObjects(PrayerTopicBackup::class.java)

        Log.d(TAG, "Retrieved ${backupTopics.size} topics from backup")

        // 대상자 복원
        var personCount = 0
        val personIdMap = mutableMapOf<Long, Long>()
        for (backupPerson in backupPersons) {
            // 같은 이름의 대상자가 존재하는지 확인
            val existingPersons = prayerRepository.searchPersons(backupPerson.name)
                .first()
            
            val newPersonId = if (existingPersons.isNotEmpty()) {
                // 기존 대상자 업데이트
                val existingPerson = existingPersons.first()
                val updatedPerson = existingPerson.copy(
                    memo = backupPerson.memo,
                    dayOfWeekAssignment = backupPerson.dayOfWeekAssignment.toSet()
                )
                prayerRepository.updatePerson(updatedPerson)
                existingPerson.id
            } else {
                // 새로운 대상자 생성
                val newPerson = Person(
                    name = backupPerson.name,
                    memo = backupPerson.memo,
                    dayOfWeekAssignment = backupPerson.dayOfWeekAssignment.toSet(),
                    createdAt = backupPerson.createdAt ?: Date()
                )
                prayerRepository.insertPerson(newPerson)
            }
            
            personIdMap[backupPerson.originalId] = newPersonId
            personCount++
            Log.d(TAG, "Restored person: ${backupPerson.name}")
        }

        // 기도제목 복원
        var topicCount = 0
        for (backupTopic in backupTopics) {
            val newPersonId = personIdMap[backupTopic.personId] ?: continue
            
            val status = try {
                PrayerStatus.valueOf(backupTopic.status)
            } catch (e: Exception) {
                PrayerStatus.ACTIVE
            }

            // 같은 개인의 같은 제목의 기도제목이 존재하는지 확인
            val existingTopics = prayerRepository.getPrayerTopicsByPerson(newPersonId)
                .first()
                .filter { it.title == backupTopic.title }
            
            if (existingTopics.isNotEmpty()) {
                // 기존 기도제목 업데이트
                val existingTopic = existingTopics.first()
                val updatedTopic = existingTopic.copy(
                    priority = backupTopic.priority,
                    status = status,
                    answeredAt = backupTopic.answeredAt
                )
                prayerRepository.updatePrayerTopic(updatedTopic)
            } else {
                // 새로운 기도제목 생성
                val newTopic = PrayerTopic(
                    personId = newPersonId,
                    title = backupTopic.title,
                    priority = backupTopic.priority,
                    status = status,
                    createdAt = backupTopic.createdAt ?: Date(),
                    answeredAt = backupTopic.answeredAt
                )
                prayerRepository.insertPrayerTopic(newTopic)
            }
            
            topicCount++
            Log.d(TAG, "Restored topic: ${backupTopic.title}")
        }

        val message = "Restore completed: $personCount persons, $topicCount topics"
        Log.d(TAG, message)
        Result.success(message)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to restore backup", e)
        Result.failure(e)
    }
}
