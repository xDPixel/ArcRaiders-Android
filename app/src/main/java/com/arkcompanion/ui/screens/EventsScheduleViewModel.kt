package com.arkcompanion.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arkcompanion.events.EventScheduleSectionUiModel
import com.arkcompanion.events.reminderId
import com.arkcompanion.events.toUiSections
import com.arkcompanion.network.EventScheduleDto
import com.arkcompanion.reminders.EventReminder
import com.arkcompanion.reminders.NotificationSettings
import com.arkcompanion.reminders.ReminderModule
import com.arkcompanion.repository.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Holds state for the live events schedule screen.
 *
 * Parameters:
 * - application: The application instance supplied by the default ViewModel factory.
 * Returns: A lifecycle-aware schedule view model.
 */
class EventsScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = ReminderModule.preferencesRepository
    private val reminderScheduler = ReminderModule.reminderScheduler
    private val events = MutableStateFlow<List<EventScheduleDto>>(emptyList())

    private val _uiState = MutableStateFlow(EventsScheduleUiState())
    val uiState: StateFlow<EventsScheduleUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        refresh(forceRefresh = false)
    }

    /**
     * Refreshes the events schedule from the live API.
     *
     * Parameters:
     * - forceRefresh: Whether the repository should bypass its in-memory cache.
     * Returns: Unit.
     */
    fun refresh(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = DataRepository.getEventsSchedule(forceRefresh = forceRefresh)
            result.onSuccess { schedule ->
                events.value = schedule
                preferencesRepository.pruneExpiredReminders(System.currentTimeMillis())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    rawEvents = schedule
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Failed to fetch live events schedule", throwable)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unable to load the events schedule."
                )
            }
        }
    }

    /**
     * Enables or disables a reminder for the selected event instance.
     *
     * Parameters:
     * - event: The event instance that should be toggled.
     * Returns: Unit.
     */
    fun toggleReminder(event: EventScheduleDto) {
        viewModelScope.launch {
            val currentSettings = preferencesRepository.settingsFlow.value
            if (!currentSettings.notificationsEnabled || event.endTime <= System.currentTimeMillis()) {
                return@launch
            }

            val reminder = EventReminder(
                reminderId = event.reminderId(),
                eventName = event.name,
                mapName = event.map,
                iconUrl = event.icon,
                startTime = event.startTime,
                endTime = event.endTime
            )
            val activeReminderIds = preferencesRepository.remindersFlow.value.map { it.reminderId }.toSet()
            if (reminder.reminderId in activeReminderIds) {
                preferencesRepository.removeReminder(reminder.reminderId)
                reminderScheduler.cancel(reminder.reminderId)
            } else {
                preferencesRepository.addOrReplaceReminder(reminder)
                reminderScheduler.schedule(reminder, currentSettings)
            }
        }
    }

    /**
     * Observes settings and reminder changes to keep the UI in sync.
     *
     * Parameters: None.
     * Returns: Unit.
     */
    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferencesRepository.settingsFlow,
                preferencesRepository.remindersFlow,
                events
            ) { settings, reminders, schedule ->
                Triple(settings, reminders, schedule)
            }.collect { (settings, reminders, schedule) ->
                val nowMillis = System.currentTimeMillis()
                val activeReminderIds = reminders.map { it.reminderId }.toSet()
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    activeReminderIds = activeReminderIds,
                    rawEvents = schedule,
                    sections = schedule.toUiSections(
                        nowMillis = nowMillis,
                        settings = settings,
                        activeReminderIds = activeReminderIds
                    ),
                    scheduledRemindersCount = reminders.size
                )
            }
        }
    }

    private companion object {
        private const val TAG = "EventsScheduleViewModel"
    }
}

/**
 * Represents the full UI state for the events schedule screen.
 *
 * Parameters: Property values are supplied through the primary constructor.
 * Returns: An immutable state snapshot for Compose rendering.
 */
data class EventsScheduleUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val rawEvents: List<EventScheduleDto> = emptyList(),
    val sections: List<EventScheduleSectionUiModel> = emptyList(),
    val settings: NotificationSettings = NotificationSettings(),
    val activeReminderIds: Set<String> = emptySet(),
    val scheduledRemindersCount: Int = 0
)
