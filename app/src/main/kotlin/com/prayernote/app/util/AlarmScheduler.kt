package com.prayernote.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: AlarmTime) {
        if (!alarm.enabled) {
            cancelAlarm(alarm)
            return
        }

        // Check if we can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Cannot schedule exact alarms, need to request permission
                return
            }
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

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.prayernote.app.DAILY_ALARM"
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_hour", alarm.hour)
            putExtra("alarm_minute", alarm.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("AlarmScheduler", "Scheduling alarm: ID=${alarm.id}, Time=${alarm.hour}:${alarm.minute}, Target=${targetTime.time}")

        // Use setExactAndAllowWhileIdle for precise alarm even in Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTime.timeInMillis,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Used setExactAndAllowWhileIdle")
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                targetTime.timeInMillis,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Used setExact")
        }
    }

    fun cancelAlarm(alarm: AlarmTime) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.prayernote.app.DAILY_ALARM"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun cancelAllAlarms() {
        // This will be called with actual alarm list from repository
    }

    fun rescheduleAllAlarms(alarms: List<AlarmTime>) {
        alarms.forEach { alarm ->
            if (alarm.enabled) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
