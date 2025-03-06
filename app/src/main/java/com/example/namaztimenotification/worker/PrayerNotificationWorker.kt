package com.example.namaztimenotification.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.namaztimenotification.R
import com.example.namaztimenotification.data.model.PrayerTime
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import java.util.concurrent.TimeUnit

class PrayerNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "prayer_notifications"
        private const val CHANNEL_NAME = "Prayer Time Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for prayer times"
        private const val NOTIFICATION_ID = 1

        fun scheduleNotifications(
            context: Context,
            prayerTime: PrayerTime,
            timeZone: ZoneId
        ) {
            val workManager = WorkManager.getInstance(context)
            
            // Schedule start notification
            val startWorkRequest = createWorkRequest(
                prayerTime,
                NotificationType.START,
                timeZone
            )
            workManager.enqueueUniqueWork(
                "prayer_start_${prayerTime.date}_${prayerTime.prayerName}",
                ExistingWorkPolicy.REPLACE,
                startWorkRequest
            )

            // Schedule 15 minutes before end notification
            val beforeEndWorkRequest = createWorkRequest(
                prayerTime,
                NotificationType.BEFORE_END,
                timeZone
            )
            workManager.enqueueUniqueWork(
                "prayer_before_end_${prayerTime.date}_${prayerTime.prayerName}",
                ExistingWorkPolicy.REPLACE,
                beforeEndWorkRequest
            )

            // Schedule end notification
            val endWorkRequest = createWorkRequest(
                prayerTime,
                NotificationType.END,
                timeZone
            )
            workManager.enqueueUniqueWork(
                "prayer_end_${prayerTime.date}_${prayerTime.prayerName}",
                ExistingWorkPolicy.REPLACE,
                endWorkRequest
            )
        }

        private fun createWorkRequest(
            prayerTime: PrayerTime,
            type: NotificationType,
            timeZone: ZoneId
        ): OneTimeWorkRequest {
            val now = LocalDateTime.now(timeZone)
            val targetTime = when (type) {
                NotificationType.START -> LocalDateTime.of(prayerTime.date, prayerTime.startTime)
                NotificationType.BEFORE_END -> LocalDateTime.of(prayerTime.date, prayerTime.endTime)
                    .minusMinutes(15)
                NotificationType.END -> LocalDateTime.of(prayerTime.date, prayerTime.endTime)
            }

            val delay = Duration.between(now, targetTime)
            if (delay.isNegative) {
                return OneTimeWorkRequestBuilder<PrayerNotificationWorker>()
                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                    .build()
            }

            return OneTimeWorkRequestBuilder<PrayerNotificationWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        "prayer_name" to prayerTime.prayerName,
                        "notification_type" to type.name
                    )
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()
        
        val prayerName = inputData.getString("prayer_name") ?: return Result.failure()
        val notificationType = NotificationType.valueOf(
            inputData.getString("notification_type") ?: return Result.failure()
        )

        val notification = createNotification(prayerName, notificationType)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(prayerName: String, type: NotificationType): android.app.Notification {
        val title = when (type) {
            NotificationType.START -> "$prayerName Prayer Time Started"
            NotificationType.BEFORE_END -> "$prayerName Prayer Time Ending Soon"
            NotificationType.END -> "$prayerName Prayer Time Ended"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    enum class NotificationType {
        START, BEFORE_END, END
    }
} 