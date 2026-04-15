package com.arkcompanion.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arkcompanion.MainActivity
import com.arkcompanion.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Displays a local notification when an event reminder becomes due.
 *
 * Parameters:
 * - appContext: Application context provided by WorkManager.
 * - workerParams: The input parameters for the scheduled work.
 * Returns: A worker capable of posting reminder notifications.
 */
class EventReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Executes the reminder notification work.
     *
     * Parameters: None.
     * Returns: A WorkManager result indicating completion.
     */
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val eventName = inputData.getString(KEY_EVENT_NAME) ?: return Result.failure()
        val mapName = inputData.getString(KEY_MAP_NAME) ?: return Result.failure()
        val startTime = inputData.getLong(KEY_START_TIME, 0L)
        val reminderMinutes = inputData.getInt(KEY_REMINDER_MINUTES, 30)

        val launchIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$eventName starts soon")
            .setContentText("$mapName • Starts in $reminderMinutes min at ${formatStartTime(startTime)}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$eventName on $mapName starts in $reminderMinutes minutes at ${formatStartTime(startTime)}."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(reminderId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "event-reminders"
        const val KEY_REMINDER_ID = "key_reminder_id"
        const val KEY_EVENT_NAME = "key_event_name"
        const val KEY_MAP_NAME = "key_map_name"
        const val KEY_ICON_URL = "key_icon_url"
        const val KEY_START_TIME = "key_start_time"
        const val KEY_REMINDER_MINUTES = "key_reminder_minutes"

        /**
         * Creates the reminder notification channel on Android O and above.
         *
         * Parameters:
         * - context: Application context used to access the notification manager.
         * Returns: Unit.
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled reminders for upcoming ARC Raiders live events."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        /**
         * Formats an epoch timestamp into a short local time string.
         *
         * Parameters:
         * - startTimeMillis: The event start time in epoch milliseconds.
         * Returns: A localised short time string.
         */
        fun formatStartTime(startTimeMillis: Long): String {
            return DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(startTimeMillis))
        }
    }
}
