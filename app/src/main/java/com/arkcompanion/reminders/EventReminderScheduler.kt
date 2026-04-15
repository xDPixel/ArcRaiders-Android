package com.arkcompanion.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Schedules and cancels event reminder jobs.
 *
 * Parameters: Implementations define their own scheduling backend.
 * Returns: A scheduling contract used by the UI layer.
 */
interface EventReminderScheduler {
    /**
     * Schedules a reminder job for a selected event.
     *
     * Parameters:
     * - reminder: The reminder payload to schedule.
     * - settings: The current notification settings snapshot.
     * Returns: Unit.
     */
    suspend fun schedule(reminder: EventReminder, settings: NotificationSettings)

    /**
     * Cancels a single reminder job.
     *
     * Parameters:
     * - reminderId: The identifier of the reminder to cancel.
     * Returns: Unit.
     */
    suspend fun cancel(reminderId: String)

    /**
     * Reconciles all saved reminders with the latest settings.
     *
     * Parameters:
     * - reminders: The selected reminders that should be considered.
     * - settings: The current notification settings snapshot.
     * Returns: Unit.
     */
    suspend fun sync(reminders: List<EventReminder>, settings: NotificationSettings)
}

/**
 * WorkManager-backed reminder scheduler.
 *
 * Parameters:
 * - context: Application context used to resolve WorkManager.
 * Returns: A persistent scheduler implementation.
 */
class WorkManagerEventReminderScheduler(context: Context) : EventReminderScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    /**
     * Schedules a reminder job for a selected event.
     *
     * Parameters:
     * - reminder: The reminder payload to schedule.
     * - settings: The current notification settings snapshot.
     * Returns: Unit.
     */
    override suspend fun schedule(reminder: EventReminder, settings: NotificationSettings) {
        if (!settings.notificationsEnabled || reminder.endTime <= System.currentTimeMillis()) {
            cancel(reminder.reminderId)
            return
        }

        val triggerAtMillis = reminder.startTime - TimeUnit.MINUTES.toMillis(settings.reminderMinutesBefore.toLong())
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    EventReminderWorker.KEY_REMINDER_ID to reminder.reminderId,
                    EventReminderWorker.KEY_EVENT_NAME to reminder.eventName,
                    EventReminderWorker.KEY_MAP_NAME to reminder.mapName,
                    EventReminderWorker.KEY_ICON_URL to reminder.iconUrl,
                    EventReminderWorker.KEY_START_TIME to reminder.startTime,
                    EventReminderWorker.KEY_REMINDER_MINUTES to settings.reminderMinutesBefore
                )
            )
            .addTag(uniqueWorkName(reminder.reminderId))
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(reminder.reminderId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Cancels a single reminder job.
     *
     * Parameters:
     * - reminderId: The identifier of the reminder to cancel.
     * Returns: Unit.
     */
    override suspend fun cancel(reminderId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(reminderId))
    }

    /**
     * Reconciles all saved reminders with the latest settings.
     *
     * Parameters:
     * - reminders: The selected reminders that should be considered.
     * - settings: The current notification settings snapshot.
     * Returns: Unit.
     */
    override suspend fun sync(reminders: List<EventReminder>, settings: NotificationSettings) {
        reminders.forEach { reminder ->
            if (settings.notificationsEnabled) {
                schedule(reminder, settings)
            } else {
                cancel(reminder.reminderId)
            }
        }
    }

    /**
     * Builds a unique WorkManager work name for one reminder.
     *
     * Parameters:
     * - reminderId: The identifier that should map to a unique job.
     * Returns: The unique work name.
     */
    private fun uniqueWorkName(reminderId: String): String = "event-reminder-$reminderId"
}

/**
 * Test double used by previews and unit tests.
 *
 * Parameters: None.
 * Returns: An in-memory scheduler implementation.
 */
class InMemoryEventReminderScheduler : EventReminderScheduler {
    val scheduledReminders: MutableMap<String, Pair<EventReminder, NotificationSettings>> = linkedMapOf()

    /**
     * Schedules a reminder job for a selected event.
     *
     * Parameters:
     * - reminder: The reminder payload to schedule.
     * - settings: The current notification settings snapshot.
     * Returns: Unit.
     */
    override suspend fun schedule(reminder: EventReminder, settings: NotificationSettings) {
        scheduledReminders[reminder.reminderId] = reminder to settings
    }

    /**
     * Cancels a single reminder job.
     *
     * Parameters:
     * - reminderId: The identifier of the reminder to cancel.
     * Returns: Unit.
     */
    override suspend fun cancel(reminderId: String) {
        scheduledReminders.remove(reminderId)
    }

    /**
     * Reconciles all saved reminders with the latest settings.
     *
     * Parameters:
     * - reminders: The selected reminders that should be considered.
     * - settings: The current notification settings snapshot.
     * Returns: Unit.
     */
    override suspend fun sync(reminders: List<EventReminder>, settings: NotificationSettings) {
        if (!settings.notificationsEnabled) {
            scheduledReminders.clear()
            return
        }

        val validIds = reminders.map { it.reminderId }.toSet()
        scheduledReminders.keys.retainAll(validIds)
        reminders.forEach { schedule(it, settings) }
    }
}
