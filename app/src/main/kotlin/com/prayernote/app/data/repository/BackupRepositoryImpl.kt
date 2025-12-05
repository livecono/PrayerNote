package com.prayernote.app.data.repository

import com.prayernote.app.data.firebase.model.BackupSession
import com.prayernote.app.data.firebase.model.PersonBackup
import com.prayernote.app.data.firebase.model.PrayerTopicBackup
import com.prayernote.app.data.firebase.service.FirebaseBackupService
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val firebaseBackupService: FirebaseBackupService?
) : BackupRepository {

    override suspend fun backupSelectedData(
        persons: List<Person>,
        topics: List<PrayerTopic>,
        includeAnswered: Boolean
    ): Result<String> {
        return if (firebaseBackupService != null) {
            firebaseBackupService.backupSelectedData(persons, topics, includeAnswered)
        } else {
            Result.failure(Exception("Firebase가 초기화되지 않았습니다. Firebase Console에서 프로젝트를 설정하고 google-services.json을 추가해주세요."))
        }
    }

    override suspend fun getBackupSessions(): Result<List<BackupSession>> {
        return if (firebaseBackupService != null) {
            firebaseBackupService.getBackupSessions()
        } else {
            Result.failure(Exception("Firebase가 초기화되지 않았습니다"))
        }
    }

    override suspend fun getBackupPersons(backupId: String): Result<List<PersonBackup>> {
        return if (firebaseBackupService != null) {
            firebaseBackupService.getBackupPersons(backupId)
        } else {
            Result.failure(Exception("Firebase가 초기화되지 않았습니다"))
        }
    }

    override suspend fun getBackupTopics(backupId: String): Result<List<PrayerTopicBackup>> {
        return if (firebaseBackupService != null) {
            firebaseBackupService.getBackupTopics(backupId)
        } else {
            Result.failure(Exception("Firebase가 초기화되지 않았습니다"))
        }
    }

    override suspend fun deleteBackupSession(backupId: String): Result<Unit> {
        return if (firebaseBackupService != null) {
            firebaseBackupService.deleteBackupSession(backupId)
        } else {
            Result.failure(Exception("Firebase가 초기화되지 않았습니다"))
        }
    }

    override suspend fun restoreBackupData(backupId: String): Result<String> {
        return if (firebaseBackupService != null) {
            firebaseBackupService.restoreBackupData(backupId)
        } else {
            Result.failure(Exception("Firebase가 초기화되지 않았습니다"))
        }
    }
}
