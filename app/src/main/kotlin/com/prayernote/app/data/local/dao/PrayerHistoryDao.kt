package com.prayernote.app.data.local.dao

import androidx.room.*
import com.prayernote.app.data.local.entity.PrayerHistory
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class PrayerStatistics(
    val personId: Long,
    val personName: String,
    val prayerCount: Int
)

data class MonthlyPrayerCount(
    val month: String,
    val count: Int
)

@Dao
interface PrayerHistoryDao {
    @Query("SELECT * FROM prayer_history ORDER BY prayedAt DESC")
    fun getAllHistory(): Flow<List<PrayerHistory>>

    @Query("SELECT * FROM prayer_history WHERE topicId = :topicId ORDER BY prayedAt DESC")
    fun getHistoryByTopic(topicId: Long): Flow<List<PrayerHistory>>

    @Query("SELECT * FROM prayer_history WHERE personId = :personId ORDER BY prayedAt DESC")
    fun getHistoryByPerson(personId: Long): Flow<List<PrayerHistory>>

    @Query("SELECT * FROM prayer_history WHERE prayedAt BETWEEN :startDate AND :endDate ORDER BY prayedAt DESC")
    fun getHistoryByDateRange(startDate: Date, endDate: Date): Flow<List<PrayerHistory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(history: PrayerHistory): Long

    @Query("SELECT EXISTS(SELECT 1 FROM prayer_history WHERE topicId = :topicId AND DATE(prayedAt / 1000, 'unixepoch') = DATE(:date / 1000, 'unixepoch'))")
    suspend fun hasHistoryForDate(topicId: Long, date: Date): Boolean

    @Query("SELECT COUNT(*) FROM prayer_history")
    suspend fun getTotalPrayerCount(): Int

    @Query("SELECT COUNT(*) FROM prayer_history WHERE prayedAt BETWEEN :startDate AND :endDate")
    suspend fun getPrayerCountByDateRange(startDate: Date, endDate: Date): Int

    @Query("""
        SELECT persons.id as personId, persons.name as personName, COUNT(*) as prayerCount
        FROM prayer_history
        INNER JOIN persons ON prayer_history.personId = persons.id
        WHERE prayedAt BETWEEN :startDate AND :endDate
        GROUP BY persons.id, persons.name
        ORDER BY prayerCount DESC
    """)
    suspend fun getPrayerStatisticsByPerson(startDate: Date, endDate: Date): List<PrayerStatistics>

    @Query("""
        SELECT strftime('%Y-%m', prayedAt / 1000, 'unixepoch') as month, COUNT(*) as count
        FROM prayer_history
        WHERE prayedAt BETWEEN :startDate AND :endDate
        GROUP BY month
        ORDER BY month ASC
    """)
    suspend fun getMonthlyPrayerCount(startDate: Date, endDate: Date): List<MonthlyPrayerCount>
}
