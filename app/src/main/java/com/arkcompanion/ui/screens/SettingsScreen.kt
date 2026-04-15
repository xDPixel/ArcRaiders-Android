package com.arkcompanion.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arkcompanion.reminders.EventReminder
import com.arkcompanion.reminders.NotificationSettings
import com.arkcompanion.reminders.ReminderModule
import com.arkcompanion.ui.theme.ArkCompanionTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Renders the notifications settings screen backed by the shared settings view model.
 *
 * Parameters:
 * - onBackClick: Invoked when the back button is tapped.
 * - viewModel: The settings view model used by the screen.
 * Returns: A Compose screen.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setNotificationsEnabled(granted)
    }

    SettingsContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNotificationsToggle = { enabled ->
            if (!enabled) {
                viewModel.setNotificationsEnabled(false)
                return@SettingsContent
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                viewModel.setNotificationsEnabled(true)
                return@SettingsContent
            }

            val permissionState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState == PackageManager.PERMISSION_GRANTED) {
                viewModel.setNotificationsEnabled(true)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onReminderLeadTimeSelected = viewModel::setReminderMinutesBefore
    )
}

/**
 * Stateless notifications settings content used by previews and the live screen.
 *
 * Parameters:
 * - uiState: The immutable state snapshot to render.
 * - onBackClick: Invoked when the back button is tapped.
 * - onNotificationsToggle: Invoked when the notifications switch changes.
 * - onReminderLeadTimeSelected: Invoked when the reminder lead time changes.
 * Returns: A Compose UI tree.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsContent(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onReminderLeadTimeSelected: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSummaryCard(uiState = uiState)
            }
            item {
                SettingsToggleCard(
                    title = "Event notifications",
                    description = "Enable local reminders for tracked live event slots.",
                    checked = uiState.settings.notificationsEnabled,
                    onCheckedChange = onNotificationsToggle
                )
            }
            item {
                ReminderLeadTimeCard(
                    selectedMinutes = uiState.settings.reminderMinutesBefore,
                    enabled = uiState.settings.notificationsEnabled,
                    onReminderLeadTimeSelected = onReminderLeadTimeSelected
                )
            }
            item {
                Text(
                    text = "Tracked reminders",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (uiState.reminders.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "No reminders selected yet. Open Events Schedule and enable reminders for any upcoming slot.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(uiState.reminders, key = { it.reminderId }) { reminder ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = reminder.eventName, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reminder.mapName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = EventTimeFormatter.formatSlot(reminder.startTime, reminder.endTime),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Displays a short summary of the current reminder configuration.
 *
 * Parameters:
 * - uiState: The settings state snapshot.
 * Returns: A summary card composable.
 */
@Composable
fun SettingsSummaryCard(uiState: SettingsUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Live event reminders", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (uiState.settings.notificationsEnabled) {
                    "${uiState.reminders.size} reminder(s) scheduled ${uiState.settings.reminderMinutesBefore} minutes before each event."
                } else {
                    "Notifications are off. Turn them on to receive reminder alerts."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Displays a single toggle row card.
 *
 * Parameters:
 * - title: Primary row title.
 * - description: Secondary helper text.
 * - checked: The current toggle state.
 * - onCheckedChange: Called when the switch changes.
 * Returns: A toggle card composable.
 */
@Composable
fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * Displays the available reminder lead-time chips.
 *
 * Parameters:
 * - selectedMinutes: The currently selected reminder lead time.
 * - enabled: Whether the chip group should accept input.
 * - onReminderLeadTimeSelected: Called when a chip is selected.
 * Returns: A card composable.
 */
@Composable
fun ReminderLeadTimeCard(
    selectedMinutes: Int,
    enabled: Boolean,
    onReminderLeadTimeSelected: (Int) -> Unit
) {
    val options = listOf(15, 30, 60)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Reminder lead time", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Choose how early the app should notify you before a tracked event starts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { minutes ->
                    FilterChip(
                        selected = selectedMinutes == minutes,
                        onClick = { onReminderLeadTimeSelected(minutes) },
                        label = { Text("$minutes min") },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

/**
 * Holds state for the notifications settings screen.
 *
 * Parameters:
 * - application: The application instance supplied by the default ViewModel factory.
 * Returns: A lifecycle-aware settings view model.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = ReminderModule.preferencesRepository
    private val reminderScheduler = ReminderModule.reminderScheduler

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
    }

    /**
     * Updates the notifications enabled flag and reschedules reminder work.
     *
     * Parameters:
     * - enabled: The desired notifications state.
     * Returns: Unit.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            reminderScheduler.sync(
                reminders = preferencesRepository.remindersFlow.value,
                settings = preferencesRepository.settingsFlow.value
            )
        }
    }

    /**
     * Updates the reminder lead time and reschedules reminder work.
     *
     * Parameters:
     * - minutes: The desired lead time in minutes.
     * Returns: Unit.
     */
    fun setReminderMinutesBefore(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setReminderMinutesBefore(minutes)
            reminderScheduler.sync(
                reminders = preferencesRepository.remindersFlow.value,
                settings = preferencesRepository.settingsFlow.value
            )
        }
    }

    /**
     * Observes stored settings and reminders for UI updates.
     *
     * Parameters: None.
     * Returns: Unit.
     */
    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferencesRepository.settingsFlow,
                preferencesRepository.remindersFlow
            ) { settings, reminders ->
                SettingsUiState(settings = settings, reminders = reminders)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}

/**
 * Represents the full UI state for the settings screen.
 *
 * Parameters: Property values are supplied through the primary constructor.
 * Returns: An immutable state snapshot for Compose rendering.
 */
data class SettingsUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val reminders: List<EventReminder> = emptyList()
)

/**
 * Preview for the settings content.
 *
 * Parameters: None.
 * Returns: A Compose preview.
 */
@Preview(showBackground = true)
@Composable
fun SettingsContentPreview() {
    ArkCompanionTheme {
        SettingsContent(
            uiState = SettingsUiState(
                settings = NotificationSettings(
                    notificationsEnabled = true,
                    reminderMinutesBefore = 30
                ),
                reminders = listOf(
                    EventReminder(
                        reminderId = "Night Raid|Dam|1",
                        eventName = "Night Raid",
                        mapName = "Dam",
                        iconUrl = "",
                        startTime = 1_776_254_400_000,
                        endTime = 1_776_258_000_000
                    )
                )
            ),
            onBackClick = {},
            onNotificationsToggle = {},
            onReminderLeadTimeSelected = {}
        )
    }
}
