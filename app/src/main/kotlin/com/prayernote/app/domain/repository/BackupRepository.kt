package com.prayernote.app.domain.repository

import com.prayernote.app.data.firebase.model.BackupSession
import com.prayernote.app.data.firebase.model.PersonBackup
import com.prayernote.app.data.firebase.model.PrayerTopicBackup
import com.prayernote.app.data.local.entity.Person
import com.prayernote.app.data.local.entity.PrayerTopic

interface BackupRepository {
    /**
     * 선택된 대상자와 기도제목을 Firebase에 백업
     */
    suspend fun backupSelectedData(
        persons: List<Person>,
        topics: List<PrayerTopic>,
        includeAnswered: Boolean = false
    ): Result<String>

    /**
     * 백업된 세션 목록 조회
     */
    suspend fun getBackupSessions(): Result<List<BackupSession>>

    /**
     * 특정 백업 세션의 대상자 조회
     */
    suspend fun getBackupPersons(backupId: String): Result<List<PersonBackup>>

    /**
     * 특정 백업 세션의 기도제목 조회
     */
    suspend fun getBackupTopics(backupId: String): Result<List<PrayerTopicBackup>>

    /**
     * 백업 세션 삭제
     */
    suspend fun deleteBackupSession(backupId: String): Result<Unit>

    /**
     * 백업된 데이터 복원
     */
    suspend fun restoreBackupData(backupId: String): Result<String>
}
