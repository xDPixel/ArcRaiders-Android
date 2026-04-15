package com.arkcompanion.events

import com.arkcompanion.network.EventScheduleDto
import com.arkcompanion.reminders.NotificationSettings

/**
 * Builds a stable reminder identifier for a scheduled event instance.
 *
 * Parameters: None.
 * Returns: A deterministic identifier that is safe to persist locally.
 */
fun EventScheduleDto.reminderId(): String = "$name|$map|$startTime"

/**
 * Converts the raw API payload into time-based UI sections.
 *
 * Parameters:
 * - nowMillis: The current device time in milliseconds.
 * - settings: The current notification settings snapshot.
 * - activeReminderIds: Reminder identifiers that are currently enabled.
 * Returns: Sorted UI sections ready for rendering.
 */
fun List<EventScheduleDto>.toUiSections(
    nowMillis: Long,
    settings: NotificationSettings,
    activeReminderIds: Set<String>
): List<EventScheduleSectionUiModel> {
    return groupBy { it.startTime }
        .toSortedMap()
        .map { (slotStartTime, slotEvents) ->
            val sortedEntries = slotEvents
                .sortedWith(compareBy<EventScheduleDto>({ it.map }, { it.name }))
                .map { event ->
                    val reminderKey = event.reminderId()
                    EventScheduleEntryUiModel(
                        reminderId = reminderKey,
                        name = event.name,
                        map = event.map,
                        icon = event.icon,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        isOngoing = nowMillis in event.startTime until event.endTime,
                        reminderAvailable = event.startTime > nowMillis,
                        reminderEnabled = reminderKey in activeReminderIds
                    )
                }
            EventScheduleSectionUiModel(
                slotStartTime = slotStartTime,
                slotEndTime = sortedEntries.maxOfOrNull { it.endTime } ?: slotStartTime,
                entries = sortedEntries
            )
        }
}

/**
 * Represents one visible event row in the schedule screen.
 *
 * Parameters: Property values are supplied through the primary constructor.
 * Returns: A fully described event row model.
 */
data class EventScheduleEntryUiModel(
    val reminderId: String,
    val name: String,
    val map: String,
    val icon: String,
    val startTime: Long,
    val endTime: Long,
    val isOngoing: Boolean,
    val reminderAvailable: Boolean,
    val reminderEnabled: Boolean
)

/**
 * Represents a time slot section in the schedule screen.
 *
 * Parameters: Property values are supplied through the primary constructor.
 * Returns: A grouped section of event rows.
 */
data class EventScheduleSectionUiModel(
    val slotStartTime: Long,
    val slotEndTime: Long,
    val entries: List<EventScheduleEntryUiModel>
)
