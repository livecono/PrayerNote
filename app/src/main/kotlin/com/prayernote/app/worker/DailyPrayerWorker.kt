package com.prayernote.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayernote.app.MainActivity
import com.prayernote.app.R
import com.prayernote.app.data.local.entity.PrayerHistory
import com.prayernote.app.data.local.entity.PrayerStatus
import com.prayernote.app.domain.repository.PrayerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

@HiltWorker
class DailyPrayerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PrayerRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "daily_prayer_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            // Get current day of week (0 = Sunday, 6 = Saturday)
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

            // Get persons assigned to today
            val personsToday = repository.getPersonsByDay(dayOfWeek).first()

            val prayerTopics = mutableListOf<Pair<String, String>>()

            if (personsToday.isNotEmpty()) {
                // Get prayer topics for each person (max 3 topics total, prioritized)
                
                for (personWithDay in personsToday) {
                    val topics = repository.getPrayerTopicsByPersonAndStatus(
                        personWithDay.person.id,
                        PrayerStatus.ACTIVE
                    ).first()

                    topics.take(1).forEach { topic ->
                        prayerTopics.add(Pair(personWithDay.person.name, topic.title))
                        
                        // Save prayer history
                        val today = Date()
                        val hasHistory = repository.hasHistoryForDate(topic.id, today)
                        if (!hasHistory) {
                            repository.insertHistory(
                                PrayerHistory(
                                    topicId = topic.id,
                                    personId = personWithDay.person.id,
                                    prayedAt = today
                                )
                            )
                        }
                    }

                    if (prayerTopics.size >= 3) break
                }
            }

            // Show notification even if no data (for testing)
            if (prayerTopics.isNotEmpty()) {
                showNotification(prayerTopics, dayOfWeek)
            } else {
                // Show test notification when no data
                showTestNotification(dayOfWeek)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error notification for debugging
            showErrorNotification(e.message ?: "Unknown error")
            Result.failure()
        }
    }

    private fun showNotification(prayerTopics: List<Pair<String, String>>, dayOfWeek: Int) {
        createNotificationChannel()

        // Build notification content
        val contentText = buildString {
            prayerTopics.forEachIndexed { index, (personName, topicTitle) ->
                append("• $personName: $topicTitle")
                if (index < prayerTopics.size - 1) append("\n")
            }
        }

        // Create intent to open app with deep link
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data = android.net.Uri.parse("prayernote://home?dayOfWeek=$dayOfWeek")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(if (prayerTopics.size == 1) contentText else "${prayerTopics.size}개의 기도제목")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showTestNotification(dayOfWeek: Int) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("기도 알림 테스트")
            .setContentText("오늘은 ${getDayName(dayOfWeek)}입니다. 할당된 기도 대상자가 없습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(error: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("알림 오류")
            .setContentText("알림 처리 중 오류: $error")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            0 -> "일요일"
            1 -> "월요일"
            2 -> "화요일"
            3 -> "수요일"
            4 -> "목요일"
            5 -> "금요일"
            6 -> "토요일"
            else -> "알 수 없음"
        }
    }
}

