package com.arkcompanion.events

import com.arkcompanion.network.EventScheduleDto
import com.arkcompanion.reminders.NotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventScheduleModelsTest {

    @Test
    fun `toUiSections groups events by start slot and sorts entries by map`() {
        val nowMillis = 1_000L
        val settings = NotificationSettings(
            notificationsEnabled = true,
            reminderMinutesBefore = 30
        )
        val events = listOf(
            EventScheduleDto(
                name = "Night Raid",
                map = "Spaceport",
                icon = "night.webp",
                startTime = 10_000L,
                endTime = 20_000L
            ),
            EventScheduleDto(
                name = "Close Scrutiny",
                map = "Blue Gate",
                icon = "close.webp",
                startTime = 10_000L,
                endTime = 20_000L
            ),
            EventScheduleDto(
                name = "Harvester",
                map = "Dam",
                icon = "harvester.webp",
                startTime = 30_000L,
                endTime = 40_000L
            )
        )

        val sections = events.toUiSections(
            nowMillis = nowMillis,
            settings = settings,
            activeReminderIds = setOf(events.first().reminderId())
        )

        assertEquals(2, sections.size)
        assertEquals(2, sections.first().entries.size)
        assertEquals("Blue Gate", sections.first().entries.first().map)
        assertTrue(sections.first().entries.any { it.reminderEnabled })
    }

    @Test
    fun `toUiSections disables reminders for started events`() {
        val settings = NotificationSettings(
            notificationsEnabled = true,
            reminderMinutesBefore = 15
        )
        val event = EventScheduleDto(
            name = "Lush Blooms",
            map = "Buried City",
            icon = "lush.webp",
            startTime = 5_000L,
            endTime = 15_000L
        )

        val section = listOf(event).toUiSections(
            nowMillis = 6_000L,
            settings = settings,
            activeReminderIds = setOf(event.reminderId())
        ).single()

        val entry = section.entries.single()
        assertFalse(entry.reminderAvailable)
        assertTrue(entry.isOngoing)
    }

    @Test
    fun `reminderId is stable across invocations`() {
        val event = EventScheduleDto(
            name = "Electromagnetic Storm",
            map = "Blue Gate",
            icon = "electrical.webp",
            startTime = 123_456L,
            endTime = 456_789L
        )

        assertEquals(
            "Electromagnetic Storm|Blue Gate|123456",
            event.reminderId()
        )
        assertEquals(event.reminderId(), event.reminderId())
    }
}
