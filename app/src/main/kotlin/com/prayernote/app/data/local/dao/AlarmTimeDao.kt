package com.prayernote.app.data.local.dao

import androidx.room.*
import com.prayernote.app.data.local.entity.AlarmTime
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmTimeDao {
    @Query("SELECT * FROM alarm_times ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmTime>>

    @Query("SELECT * FROM alarm_times WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledAlarms(): Flow<List<AlarmTime>>

    @Query("SELECT * FROM alarm_times WHERE id = :alarmId")
    fun getAlarmById(alarmId: Long): Flow<AlarmTime?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarmTime: AlarmTime): Long

    @Update
    suspend fun updateAlarm(alarmTime: AlarmTime)

    @Delete
    suspend fun deleteAlarm(alarmTime: AlarmTime)

    @Query("DELETE FROM alarm_times WHERE id = :alarmId")
    suspend fun deleteAlarmById(alarmId: Long)

    @Query("SELECT COUNT(*) FROM alarm_times")
    suspend fun getAlarmCount(): Int
}
