package com.prayernote.app.domain.repository

import androidx.paging.PagingData
import com.prayernote.app.data.local.dao.MonthlyPrayerCount
import com.prayernote.app.data.local.dao.PersonWithDay
import com.prayernote.app.data.local.dao.PrayerStatistics
import com.prayernote.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PrayerRepository {
    // Person operations
    fun getAllPersons(): Flow<List<Person>>
    fun getAllPersonsPaged(): Flow<PagingData<Person>>
    fun getPersonById(personId: Long): Flow<Person?>
    fun searchPersons(query: String): Flow<List<Person>>
    fun getPersonsByDayOfWeek(dayOfWeek: Int): Flow<List<Person>>
    suspend fun insertPerson(person: Person): Long
    suspend fun updatePerson(person: Person)
    suspend fun deletePerson(person: Person)
    suspend fun getPersonCount(): Int

    // Prayer Topic operations
    fun getPrayerTopicsByPersonAndStatus(personId: Long, status: PrayerStatus): Flow<List<PrayerTopic>>
    fun getPrayerTopicsByPerson(personId: Long): Flow<List<PrayerTopic>>
    fun getPrayerTopicById(topicId: Long): Flow<PrayerTopic?>
    fun getAnsweredPrayers(): Flow<List<PrayerTopic>>
    fun getAnsweredPrayersWithPerson(): Flow<List<PrayerTopicWithPerson>>
    fun getAnsweredPrayersPaged(): Flow<PagingData<PrayerTopic>>
    suspend fun insertPrayerTopic(prayerTopic: PrayerTopic): Long
    suspend fun updatePrayerTopic(prayerTopic: PrayerTopic)
    suspend fun updatePrayerTopics(prayerTopics: List<PrayerTopic>)
    suspend fun deletePrayerTopic(prayerTopic: PrayerTopic)
    suspend fun markAsAnswered(topicId: Long)
    suspend fun getPrayerCountByStatus(status: PrayerStatus): Int
    suspend fun getPrayerCountByPersonAndStatus(personId: Long, status: PrayerStatus): Int

    // Day Assignment operations
    fun getAssignmentsByDay(dayOfWeek: Int): Flow<List<DayAssignment>>
    fun getPersonsByDay(dayOfWeek: Int): Flow<List<PersonWithDay>>
    fun getAssignmentsByPerson(personId: Long): Flow<List<DayAssignment>>
    suspend fun insertAssignment(assignment: DayAssignment): Long
    suspend fun deleteAssignment(assignment: DayAssignment)
    suspend fun deleteAssignment(personId: Long, dayOfWeek: Int)
    suspend fun isAssigned(personId: Long, dayOfWeek: Int): Boolean

    // Prayer History operations
    fun getAllHistory(): Flow<List<PrayerHistory>>
    fun getHistoryByTopic(topicId: Long): Flow<List<PrayerHistory>>
    fun getHistoryByPerson(personId: Long): Flow<List<PrayerHistory>>
    fun getHistoryByDateRange(startDate: Date, endDate: Date): Flow<List<PrayerHistory>>
    suspend fun insertHistory(history: PrayerHistory): Long
    suspend fun hasHistoryForDate(topicId: Long, date: Date): Boolean
    suspend fun getTotalPrayerCount(): Int
    suspend fun getPrayerCountByDateRange(startDate: Date, endDate: Date): Int
    suspend fun getPrayerStatisticsByPerson(startDate: Date, endDate: Date): List<PrayerStatistics>
    suspend fun getMonthlyPrayerCount(startDate: Date, endDate: Date): List<MonthlyPrayerCount>

    // Alarm Time operations
    fun getAllAlarms(): Flow<List<AlarmTime>>
    fun getEnabledAlarms(): Flow<List<AlarmTime>>
    fun getAlarmById(alarmId: Long): Flow<AlarmTime?>
    suspend fun insertAlarm(alarmTime: AlarmTime): Long
    suspend fun updateAlarm(alarmTime: AlarmTime)
    suspend fun deleteAlarm(alarmTime: AlarmTime)
    suspend fun getAlarmCount(): Int

    // Statistics operations
    suspend fun getAnswerRate(startDate: Date, endDate: Date): Float
    suspend fun getMonthlyAnswerRate(startDate: Date, endDate: Date): List<Pair<String, Float>>
}
