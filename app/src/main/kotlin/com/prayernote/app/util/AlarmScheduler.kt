package com.prayernote.app.util

import android.content.Context
import androidx.work.*
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.worker.DailyPrayerWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleAlarm(alarm: AlarmTime) {
        if (!alarm.enabled) {
            cancelAlarm(alarm)
            return
        }

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If target time is in the past, schedule for next day
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DailyPrayerWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(getWorkTag(alarm))
            .build()

        workManager.enqueueUniquePeriodicWork(
            getWorkName(alarm),
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelAlarm(alarm: AlarmTime) {
        workManager.cancelUniqueWork(getWorkName(alarm))
    }

    fun cancelAllAlarms() {
        workManager.cancelAllWorkByTag("daily_prayer")
    }

    fun rescheduleAllAlarms(alarms: List<AlarmTime>) {
        cancelAllAlarms()
        alarms.filter { it.enabled }.forEach { alarm ->
            scheduleAlarm(alarm)
        }
    }

    private fun getWorkName(alarm: AlarmTime): String {
        return "daily_prayer_${alarm.id}_${alarm.hour}_${alarm.minute}"
    }

    private fun getWorkTag(alarm: AlarmTime): String {
        return "daily_prayer"
    }
}
