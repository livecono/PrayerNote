package com.prayernote.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.prayernote.app.domain.repository.PrayerRepository
import com.prayernote.app.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: PrayerRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            // Reschedule all alarms after device boot
            scope.launch {
                try {
                    val alarms = repository.getAllAlarms().first()
                    alarmScheduler.rescheduleAllAlarms(alarms)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
