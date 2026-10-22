package com.synfusion.attendance.notifications.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.notifications.data.NotificationPreferencesRepository
import com.synfusion.attendance.notifications.model.NotificationPreferences
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateStudio: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NotificationPreferencesRepository(context) }
    val prefsState by repository.preferencesFlow.collectAsState(initial = NotificationPreferences())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings Card
            Text("PREFERENCES & QUIET HOURS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggleRow(
                        title = "Notification Sound",
                        subtitle = "Play audio tones for rich alerts",
                        checked = prefsState.soundEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch { repository.updateSoundAndVibration(checked, prefsState.vibrationEnabled) }
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Vibration",
                        subtitle = "Haptic feedback on notification delivery",
                        checked = prefsState.vibrationEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch { repository.updateSoundAndVibration(prefsState.soundEnabled, checked) }
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Quiet Hours (22:00 - 07:00)",
                        subtitle = "Suppress non-essential alerts during sleep",
                        checked = prefsState.quietHoursEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch { repository.updateQuietHours(checked) }
                        }
                    )
                }
            }

            // Category Specific Toggles
            Text("NOTIFICATION CATEGORIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggleRow(
                        title = "Daily Check-in",
                        subtitle = "Daily coin claim reminders",
                        checked = prefsState.checkInEnabled,
                        onCheckedChange = { coroutineScope.launch { repository.updateCategoryEnabled("check_in", it) } }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Builder's Spin",
                        subtitle = "Free wheel spin alerts",
                        checked = prefsState.spinEnabled,
                        onCheckedChange = { coroutineScope.launch { repository.updateCategoryEnabled("spin", it) } }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Streaks & Milestones",
                        subtitle = "Streak risk and achievement badges",
                        checked = prefsState.streakEnabled,
                        onCheckedChange = { coroutineScope.launch { repository.updateCategoryEnabled("streak", it) } }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Attendance & Work",
                        subtitle = "Daily work logging reminders",
                        checked = prefsState.attendanceEnabled,
                        onCheckedChange = { coroutineScope.launch { repository.updateCategoryEnabled("attendance", it) } }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Salary & Wages",
                        subtitle = "Monthly wage and advance updates",
                        checked = prefsState.salaryEnabled,
                        onCheckedChange = { coroutineScope.launch { repository.updateCategoryEnabled("salary", it) } }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingToggleRow(
                        title = "Live Streams & Chat",
                        subtitle = "Live broadcasts and direct messages",
                        checked = prefsState.liveEnabled,
                        onCheckedChange = { coroutineScope.launch { repository.updateCategoryEnabled("live", it) } }
                    )
                }
            }

            // Developer / Admin Section for Notification Studio
            Text("DEVELOPER TOOLS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateStudio() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Preview & test all 57 Tango-inspired notification designs", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(WorkDailyIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
