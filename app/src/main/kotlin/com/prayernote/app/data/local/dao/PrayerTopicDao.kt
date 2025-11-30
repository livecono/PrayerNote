package com.prayernote.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.data.local.entity.PrayerTopic
import com.prayernote.app.data.local.entity.PrayerTopicWithPerson
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTopicDao {
    @Query("SELECT * FROM prayer_topics WHERE personId = :personId AND status = :status ORDER BY priority DESC, createdAt ASC")
    fun getPrayerTopicsByPersonAndStatus(personId: Long, status: PrayerStatus): Flow<List<PrayerTopic>>

    @Query("SELECT * FROM prayer_topics WHERE personId = :personId ORDER BY priority DESC, createdAt ASC")
    fun getPrayerTopicsByPerson(personId: Long): Flow<List<PrayerTopic>>

    @Query("SELECT * FROM prayer_topics WHERE id = :topicId")
    fun getPrayerTopicById(topicId: Long): Flow<PrayerTopic?>

    @Query("SELECT * FROM prayer_topics WHERE status = :status ORDER BY answeredAt DESC")
    fun getAnsweredPrayers(status: PrayerStatus = PrayerStatus.ANSWERED): Flow<List<PrayerTopic>>

    @Transaction
    @Query("SELECT * FROM prayer_topics WHERE status = :status ORDER BY answeredAt DESC")
    fun getAnsweredPrayersWithPerson(status: PrayerStatus = PrayerStatus.ANSWERED): Flow<List<PrayerTopicWithPerson>>

    @Query("SELECT * FROM prayer_topics WHERE status = :status ORDER BY answeredAt DESC")
    fun getAnsweredPrayersPaged(status: PrayerStatus = PrayerStatus.ANSWERED): PagingSource<Int, PrayerTopic>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTopic(prayerTopic: PrayerTopic): Long

    @Update
    suspend fun updatePrayerTopic(prayerTopic: PrayerTopic)

    @Update
    suspend fun updatePrayerTopics(prayerTopics: List<PrayerTopic>)

    @Delete
    suspend fun deletePrayerTopic(prayerTopic: PrayerTopic)

    @Query("DELETE FROM prayer_topics WHERE id = :topicId")
    suspend fun deletePrayerTopicById(topicId: Long)

    @Query("SELECT COUNT(*) FROM prayer_topics WHERE status = :status")
    suspend fun getPrayerCountByStatus(status: PrayerStatus): Int

    @Query("SELECT COUNT(*) FROM prayer_topics WHERE personId = :personId AND status = :status")
    suspend fun getPrayerCountByPersonAndStatus(personId: Long, status: PrayerStatus): Int
}
