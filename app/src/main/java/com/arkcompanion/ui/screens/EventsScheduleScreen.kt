package com.arkcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.arkcompanion.events.EventScheduleEntryUiModel
import com.arkcompanion.events.EventScheduleSectionUiModel
import com.arkcompanion.network.EventScheduleDto
import com.arkcompanion.reminders.NotificationSettings
import com.arkcompanion.ui.theme.ArkCompanionTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Renders the live events schedule backed by the schedule view model.
 *
 * Parameters:
 * - onBackClick: Invoked when the back button is tapped.
 * - onSettingsClick: Invoked when the settings button is tapped.
 * - viewModel: The events schedule view model used by the screen.
 * Returns: A Compose screen.
 */
@Composable
fun EventsScheduleScreen(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: EventsScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    EventsScheduleContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onSettingsClick = onSettingsClick,
        onRefreshClick = { viewModel.refresh(forceRefresh = true) },
        onToggleReminder = { event -> viewModel.toggleReminder(event) }
    )
}

/**
 * Stateless schedule content used by previews and the live screen.
 *
 * Parameters:
 * - uiState: The immutable state snapshot to render.
 * - onBackClick: Invoked when the back button is tapped.
 * - onSettingsClick: Invoked when the settings button is tapped.
 * - onRefreshClick: Invoked when refresh is requested.
 * - onToggleReminder: Invoked when a reminder button is tapped.
 * Returns: A Compose UI tree.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EventsScheduleContent(
    uiState: EventsScheduleUiState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleReminder: (EventScheduleDto) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events Schedule") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSettingsClick) {
                        Text("Settings")
                    }
                    TextButton(onClick = onRefreshClick) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.sections.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMessage != null && uiState.sections.isEmpty() -> {
                    EmptyScheduleState(
                        title = "Unable to load schedule",
                        message = uiState.errorMessage,
                        onRefreshClick = onRefreshClick,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            EventsScheduleSummaryCard(uiState = uiState)
                        }
                        items(uiState.sections, key = { it.slotStartTime }) { section ->
                            EventScheduleSectionCard(
                                section = section,
                                settings = uiState.settings,
                                sourceEvents = uiState.rawEvents,
                                onToggleReminder = onToggleReminder
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays the high-level schedule summary.
 *
 * Parameters:
 * - uiState: The schedule state snapshot.
 * Returns: A summary card composable.
 */
@Composable
fun EventsScheduleSummaryCard(uiState: EventsScheduleUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Live rotation", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${uiState.rawEvents.size} events across ${uiState.sections.size} upcoming time slots.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (uiState.settings.notificationsEnabled) {
                    "${uiState.scheduledRemindersCount} reminders scheduled ${uiState.settings.reminderMinutesBefore} minutes before start."
                } else {
                    "Notifications are off. Open Settings to enable event reminders."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Displays a grouped schedule section for one shared start time.
 *
 * Parameters:
 * - section: The grouped section to display.
 * - settings: The current notification settings.
 * - sourceEvents: The raw events used to resolve reminder actions.
 * - onToggleReminder: Invoked when a reminder button is tapped.
 * Returns: A section card composable.
 */
@Composable
fun EventScheduleSectionCard(
    section: EventScheduleSectionUiModel,
    settings: NotificationSettings,
    sourceEvents: List<EventScheduleDto>,
    onToggleReminder: (EventScheduleDto) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = EventTimeFormatter.formatSectionHeader(section.slotStartTime),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = EventTimeFormatter.formatSlot(section.slotStartTime, section.slotEndTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            section.entries.forEach { entry ->
                val matchingEvent = sourceEvents.firstOrNull {
                    it.name == entry.name && it.map == entry.map && it.startTime == entry.startTime
                }
                if (matchingEvent != null) {
                    EventScheduleRow(
                        entry = entry,
                        settings = settings,
                        onToggleReminder = { onToggleReminder(matchingEvent) }
                    )
                }
            }
        }
    }
}

/**
 * Displays a single schedule row.
 *
 * Parameters:
 * - entry: The row UI model.
 * - settings: The current notification settings.
 * - onToggleReminder: Invoked when the reminder button is tapped.
 * Returns: A row composable.
 */
@Composable
fun EventScheduleRow(
    entry: EventScheduleEntryUiModel,
    settings: NotificationSettings,
    onToggleReminder: () -> Unit
) {
    val reminderButtonEnabled = settings.notificationsEnabled && entry.reminderAvailable
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = System.currentTimeMillis()
        }
    }

    val totalDuration = entry.endTime - entry.startTime
    val isStarted = currentTime >= entry.startTime && currentTime < entry.endTime
    val isEnded = currentTime >= entry.endTime

    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = entry.icon,
                    contentDescription = entry.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(4.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = entry.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        text = entry.map,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                IconButton(onClick = onToggleReminder, enabled = reminderButtonEnabled) {
                    if (entry.reminderEnabled && settings.notificationsEnabled) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminder On",
                            tint = Color(0xFFFFD700)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Reminder Off",
                            tint = Color.Gray
                        )
                    }
                }
            }

            if (!isEnded) {
                if (isStarted) {
                    val remaining = entry.endTime - currentTime
                    val progress = (remaining.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                    val barColor = when {
                        progress > 0.5f -> Color(0xFF4CAF50) // Green
                        progress > 0.15f -> Color(0xFFFFEB3B) // Yellow
                        else -> Color(0xFFF44336) // Red
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(barColor), 
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ends in: ${formatTime(remaining)}", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.Black, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val untilStart = entry.startTime - currentTime
                    val twoHoursMillis = 2 * 60 * 60 * 1000L
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Color(0xFF2196F3)), 
                        contentAlignment = Alignment.Center
                    ) {
                        if (untilStart <= twoHoursMillis) {
                            Text(
                                text = "Starts in: ${formatTime(untilStart)}", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            val timeStr = EventTimeFormatter.formatSectionHeader(entry.startTime) + " " + EventTimeFormatter.formatTimeOnly(entry.startTime)
                            Text(
                                text = "Starts at: $timeStr", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays the empty or error state for the schedule screen.
 *
 * Parameters:
 * - title: Primary title text.
 * - message: Secondary helper text.
 * - onRefreshClick: Invoked when the user requests another refresh.
 * - modifier: Modifier applied to the root container.
 * Returns: An empty-state composable.
 */
@Composable
fun EmptyScheduleState(
    title: String,
    message: String,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRefreshClick) {
            Text("Try Again")
        }
    }
}

/**
 * Formats event times and relative labels for the schedule UI.
 *
 * Parameters: None.
 * Returns: Time formatting helpers used by Compose.
 */
object EventTimeFormatter {
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    /**
     * Formats a section header for a start timestamp.
     *
     * Parameters:
     * - timestampMillis: The start time to format.
     * Returns: A date label for the section header.
     */
    fun formatSectionHeader(timestampMillis: Long): String {
        return dayFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestampMillis))
    }

    fun formatTimeOnly(timestampMillis: Long): String {
        return timeFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestampMillis))
    }

    /**
     * Formats a start/end slot into a compact range.
     *
     * Parameters:
     * - startMillis: Slot start time.
     * - endMillis: Slot end time.
     * Returns: A short local time range.
     */
    fun formatSlot(startMillis: Long, endMillis: Long): String {
        val zoneId = ZoneId.systemDefault()
        val startLabel = timeFormatter.withZone(zoneId).format(Instant.ofEpochMilli(startMillis))
        val endLabel = timeFormatter.withZone(zoneId).format(Instant.ofEpochMilli(endMillis))
        return "$startLabel – $endLabel"
    }

    /**
     * Builds a relative label describing the event timing state.
     *
     * Parameters:
     * - startMillis: Event start time.
     * - endMillis: Event end time.
     * Returns: A short relative timing label.
     */
    fun relativeLabel(startMillis: Long, endMillis: Long): String {
        val nowMillis = System.currentTimeMillis()
        return when {
            nowMillis < startMillis -> {
                val minutes = Duration.ofMillis(startMillis - nowMillis).toMinutes().coerceAtLeast(1)
                "Starts in ${minutes}m"
            }

            nowMillis < endMillis -> "Live Now"
            else -> "Ended"
        }
    }

    /**
     * Builds a short duration label for the event slot.
     *
     * Parameters:
     * - startMillis: Event start time.
     * - endMillis: Event end time.
     * Returns: A duration label in minutes.
     */
    fun durationLabel(startMillis: Long, endMillis: Long): String {
        val durationMinutes = Duration.ofMillis(endMillis - startMillis).toMinutes().coerceAtLeast(1)
        return "${durationMinutes}m slot"
    }
}

/**
 * Preview for the schedule content.
 *
 * Parameters: None.
 * Returns: A Compose preview.
 */
@Preview(showBackground = true, backgroundColor = 0xFF0F0F0F)
@Composable
fun EventsScheduleContentPreview() {
    ArkCompanionTheme {
        EventsScheduleContent(
            uiState = EventsScheduleUiState(
                isLoading = false,
                rawEvents = listOf(
                    EventScheduleDto(
                        name = "Night Raid",
                        map = "Dam",
                        icon = "https://cdn.metaforge.app/arc-raiders/custom/night.webp",
                        startTime = 1_776_254_400_000,
                        endTime = 1_776_258_000_000
                    ),
                    EventScheduleDto(
                        name = "Lush Blooms",
                        map = "Buried City",
                        icon = "https://cdn.metaforge.app/arc-raiders/custom/lush.webp",
                        startTime = 1_776_254_400_000,
                        endTime = 1_776_258_000_000
                    )
                ),
                sections = listOf(
                    EventScheduleSectionUiModel(
                        slotStartTime = 1_776_254_400_000,
                        slotEndTime = 1_776_258_000_000,
                        entries = listOf(
                            EventScheduleEntryUiModel(
                                reminderId = "Night Raid|Dam|1",
                                name = "Night Raid",
                                map = "Dam",
                                icon = "https://cdn.metaforge.app/arc-raiders/custom/night.webp",
                                startTime = 1_776_254_400_000,
                                endTime = 1_776_258_000_000,
                                isOngoing = false,
                                reminderAvailable = true,
                                reminderEnabled = true
                            ),
                            EventScheduleEntryUiModel(
                                reminderId = "Lush Blooms|Buried City|1",
                                name = "Lush Blooms",
                                map = "Buried City",
                                icon = "https://cdn.metaforge.app/arc-raiders/custom/lush.webp",
                                startTime = 1_776_254_400_000,
                                endTime = 1_776_258_000_000,
                                isOngoing = false,
                                reminderAvailable = true,
                                reminderEnabled = false
                            )
                        )
                    )
                ),
                settings = NotificationSettings(
                    notificationsEnabled = true,
                    reminderMinutesBefore = 30
                ),
                scheduledRemindersCount = 1
            ),
            onBackClick = {},
            onSettingsClick = {},
            onRefreshClick = {},
            onToggleReminder = {}
        )
    }
}
