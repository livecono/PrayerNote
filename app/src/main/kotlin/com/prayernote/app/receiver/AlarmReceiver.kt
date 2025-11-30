package com.prayernote.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.prayernote.app.data.local.entity.AlarmTime
import com.prayernote.app.util.AlarmScheduler
import com.prayernote.app.worker.DailyPrayerWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "onReceive called: action=${intent.action}")
        
        if (intent.action == "com.prayernote.app.DAILY_ALARM") {
            val alarmId = intent.getLongExtra("alarm_id", -1)
            val hour = intent.getIntExtra("alarm_hour", 0)
            val minute = intent.getIntExtra("alarm_minute", 0)

            Log.d("AlarmReceiver", "Alarm triggered: ID=$alarmId, Time=$hour:$minute")

            // Trigger the DailyPrayerWorker immediately
            val workRequest = OneTimeWorkRequestBuilder<DailyPrayerWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d("AlarmReceiver", "DailyPrayerWorker enqueued")

            // Reschedule the alarm for next day
            if (alarmId != -1L) {
                val alarm = AlarmTime(
                    id = alarmId,
                    hour = hour,
                    minute = minute,
                    enabled = true
                )
                alarmScheduler.scheduleAlarm(alarm)
                Log.d("AlarmReceiver", "Alarm rescheduled for next day")
            }
        }
    }
}

