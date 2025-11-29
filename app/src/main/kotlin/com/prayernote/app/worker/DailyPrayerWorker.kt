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

            if (personsToday.isNotEmpty()) {
                // Get prayer topics for each person (max 3 topics total, prioritized)
                val prayerTopics = mutableListOf<Pair<String, String>>() // Pair<PersonName, TopicTitle>
                
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

                if (prayerTopics.isNotEmpty()) {
                    showNotification(prayerTopics, dayOfWeek)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(if (prayerTopics.size == 1) contentText else "${prayerTopics.size}개의 기도제목")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(contentText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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
}
