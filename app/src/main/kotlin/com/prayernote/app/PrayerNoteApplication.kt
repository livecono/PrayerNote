package com.prayernote.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.domain.repository.PrayerRepository
import com.prayernote.app.util.AlarmScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PrayerNoteApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var repository: PrayerRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        initializeApp()
    }

    private fun initializeApp() {
        applicationScope.launch {
            try {
                // Initialize default alarm if no alarms exist
                val alarmCount = repository.getAlarmCount()
                if (alarmCount == 0) {
                    val defaultAlarm = AlarmTime(hour = 7, minute = 0, enabled = true)
                    val alarmId = repository.insertAlarm(defaultAlarm)
                    alarmScheduler.scheduleAlarm(defaultAlarm.copy(id = alarmId))
                } else {
                    // Reschedule all existing enabled alarms
                    val alarms = repository.getAllAlarms().first()
                    alarmScheduler.rescheduleAllAlarms(alarms)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
