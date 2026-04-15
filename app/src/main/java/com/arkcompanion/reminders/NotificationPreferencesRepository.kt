package com.arkcompanion.reminders

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Stores app-level notification preferences and selected event reminders.
 *
 * Parameters: Implementations define their own persistence strategy.
 * Returns: State flows and suspend operations for settings management.
 */
interface NotificationPreferencesRepository {
    val settingsFlow: StateFlow<NotificationSettings>
    val remindersFlow: StateFlow<List<EventReminder>>

    /**
     * Enables or disables notifications globally.
     *
     * Parameters:
     * - enabled: The new notifications state.
     * Returns: Unit.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /**
     * Updates how many minutes before an event the reminder should fire.
     *
     * Parameters:
     * - minutes: The lead time in minutes.
     * Returns: Unit.
     */
    suspend fun setReminderMinutesBefore(minutes: Int)

    /**
     * Persists a reminder selection for a specific event slot.
     *
     * Parameters:
     * - reminder: The reminder to save.
     * Returns: Unit.
     */
    suspend fun addOrReplaceReminder(reminder: EventReminder)

    /**
     * Removes a previously saved reminder.
     *
     * Parameters:
     * - reminderId: The identifier of the reminder to remove.
     * Returns: Unit.
     */
    suspend fun removeReminder(reminderId: String)

    /**
     * Deletes reminders for events that already ended.
     *
     * Parameters:
     * - nowMillis: The current device time in milliseconds.
     * Returns: Unit.
     */
    suspend fun pruneExpiredReminders(nowMillis: Long)
}

/**
 * Represents the persisted notification settings snapshot.
 *
 * Parameters: Property values are supplied through the primary constructor.
 * Returns: A simple immutable settings model.
 */
data class NotificationSettings(
    val notificationsEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 30
)

/**
 * Represents one scheduled reminder selection.
 *
 * Parameters: Property values are supplied through the primary constructor.
 * Returns: A serializable reminder payload.
 */
@Serializable
data class EventReminder(
    val reminderId: String,
    val eventName: String,
    val mapName: String,
    val iconUrl: String,
    val startTime: Long,
    val endTime: Long
)

/**
 * SharedPreferences-backed repository for notification preferences.
 *
 * Parameters:
 * - context: Application context used to access preferences.
 * Returns: A concrete persistence implementation.
 */
class SharedPreferencesNotificationPreferencesRepository(context: Context) : NotificationPreferencesRepository {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _settingsFlow = MutableStateFlow(readSettings())
    override val settingsFlow: StateFlow<NotificationSettings> = _settingsFlow.asStateFlow()

    private val _remindersFlow = MutableStateFlow(readReminders())
    override val remindersFlow: StateFlow<List<EventReminder>> = _remindersFlow.asStateFlow()

    /**
     * Enables or disables notifications globally.
     *
     * Parameters:
     * - enabled: The new notifications state.
     * Returns: Unit.
     */
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        mutex.withLock {
            preferences.edit()
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
                .apply()
            _settingsFlow.value = readSettings()
        }
    }

    /**
     * Updates how many minutes before an event the reminder should fire.
     *
     * Parameters:
     * - minutes: The lead time in minutes.
     * Returns: Unit.
     */
    override suspend fun setReminderMinutesBefore(minutes: Int) {
        mutex.withLock {
            preferences.edit()
                .putInt(KEY_REMINDER_MINUTES_BEFORE, minutes)
                .apply()
            _settingsFlow.value = readSettings()
        }
    }

    /**
     * Persists a reminder selection for a specific event slot.
     *
     * Parameters:
     * - reminder: The reminder to save.
     * Returns: Unit.
     */
    override suspend fun addOrReplaceReminder(reminder: EventReminder) {
        mutex.withLock {
            val updatedReminders = readReminders()
                .filterNot { it.reminderId == reminder.reminderId }
                .plus(reminder)
                .sortedBy { it.startTime }
            writeReminders(updatedReminders)
        }
    }

    /**
     * Removes a previously saved reminder.
     *
     * Parameters:
     * - reminderId: The identifier of the reminder to remove.
     * Returns: Unit.
     */
    override suspend fun removeReminder(reminderId: String) {
        mutex.withLock {
            writeReminders(readReminders().filterNot { it.reminderId == reminderId })
        }
    }

    /**
     * Deletes reminders for events that already ended.
     *
     * Parameters:
     * - nowMillis: The current device time in milliseconds.
     * Returns: Unit.
     */
    override suspend fun pruneExpiredReminders(nowMillis: Long) {
        mutex.withLock {
            writeReminders(readReminders().filter { it.endTime > nowMillis })
        }
    }

    /**
     * Reads the settings snapshot from SharedPreferences.
     *
     * Parameters: None.
     * Returns: The current settings snapshot.
     */
    private fun readSettings(): NotificationSettings {
        return NotificationSettings(
            notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false),
            reminderMinutesBefore = preferences.getInt(KEY_REMINDER_MINUTES_BEFORE, 30)
        )
    }

    /**
     * Reads the saved reminders list from SharedPreferences.
     *
     * Parameters: None.
     * Returns: The decoded reminders list.
     */
    private fun readReminders(): List<EventReminder> {
        val payload = preferences.getString(KEY_REMINDERS_JSON, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<EventReminder>>(payload)
        }.getOrDefault(emptyList())
    }

    /**
     * Writes the reminders list to SharedPreferences and updates the observable state.
     *
     * Parameters:
     * - reminders: The reminders list to persist.
     * Returns: Unit.
     */
    private fun writeReminders(reminders: List<EventReminder>) {
        preferences.edit()
            .putString(KEY_REMINDERS_JSON, json.encodeToString(reminders))
            .apply()
        _remindersFlow.value = reminders
    }

    private companion object {
        private const val PREFERENCES_NAME = "event_reminders"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_REMINDER_MINUTES_BEFORE = "reminder_minutes_before"
        private const val KEY_REMINDERS_JSON = "reminders_json"
    }
}

/**
 * In-memory repository used for previews and tests.
 *
 * Parameters:
 * - initialSettings: Initial notification settings.
 * - initialReminders: Initial reminders list.
 * Returns: A lightweight non-persistent implementation.
 */
class InMemoryNotificationPreferencesRepository(
    initialSettings: NotificationSettings = NotificationSettings(),
    initialReminders: List<EventReminder> = emptyList()
) : NotificationPreferencesRepository {
    private val _settingsFlow = MutableStateFlow(initialSettings)
    override val settingsFlow: StateFlow<NotificationSettings> = _settingsFlow.asStateFlow()

    private val _remindersFlow = MutableStateFlow(initialReminders)
    override val remindersFlow: StateFlow<List<EventReminder>> = _remindersFlow.asStateFlow()

    /**
     * Enables or disables notifications globally.
     *
     * Parameters:
     * - enabled: The new notifications state.
     * Returns: Unit.
     */
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(notificationsEnabled = enabled)
    }

    /**
     * Updates how many minutes before an event the reminder should fire.
     *
     * Parameters:
     * - minutes: The lead time in minutes.
     * Returns: Unit.
     */
    override suspend fun setReminderMinutesBefore(minutes: Int) {
        _settingsFlow.value = _settingsFlow.value.copy(reminderMinutesBefore = minutes)
    }

    /**
     * Persists a reminder selection for a specific event slot.
     *
     * Parameters:
     * - reminder: The reminder to save.
     * Returns: Unit.
     */
    override suspend fun addOrReplaceReminder(reminder: EventReminder) {
        _remindersFlow.value = _remindersFlow.value
            .filterNot { it.reminderId == reminder.reminderId }
            .plus(reminder)
            .sortedBy { it.startTime }
    }

    /**
     * Removes a previously saved reminder.
     *
     * Parameters:
     * - reminderId: The identifier of the reminder to remove.
     * Returns: Unit.
     */
    override suspend fun removeReminder(reminderId: String) {
        _remindersFlow.value = _remindersFlow.value.filterNot { it.reminderId == reminderId }
    }

    /**
     * Deletes reminders for events that already ended.
     *
     * Parameters:
     * - nowMillis: The current device time in milliseconds.
     * Returns: Unit.
     */
    override suspend fun pruneExpiredReminders(nowMillis: Long) {
        _remindersFlow.value = _remindersFlow.value.filter { it.endTime > nowMillis }
    }
}

/**
 * Provides app-wide access to reminder collaborators.
 *
 * Parameters: None.
 * Returns: Lazily initialized dependencies.
 */
object ReminderModule {
    lateinit var preferencesRepository: NotificationPreferencesRepository
        private set

    lateinit var reminderScheduler: EventReminderScheduler
        private set

    /**
     * Initializes reminder dependencies once during app startup.
     *
     * Parameters:
     * - context: Application context used for concrete services.
     * Returns: Unit.
     */
    fun initialize(context: Context) {
        if (!::preferencesRepository.isInitialized) {
            preferencesRepository = SharedPreferencesNotificationPreferencesRepository(context.applicationContext)
        }
        if (!::reminderScheduler.isInitialized) {
            reminderScheduler = WorkManagerEventReminderScheduler(context.applicationContext)
        }
    }
}
