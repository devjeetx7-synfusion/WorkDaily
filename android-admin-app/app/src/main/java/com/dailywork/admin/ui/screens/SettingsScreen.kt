package com.dailywork.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dailywork.admin.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToLogs() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("View Admin Logs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Check recent administrative actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            SettingsCategory(title = "General Configuration") {
                ToggleItem(
                    icon = Icons.Default.Language,
                    title = "Language Selection",
                    description = "Allow users to change app language",
                    checked = config.languageEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(languageEnabled = it)) }
                )
                ToggleItem(
                    icon = Icons.Default.Translate,
                    title = "Language Activity Visibility",
                    description = "Show language activity to normal users",
                    checked = config.languageActivityEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(languageActivityEnabled = it)) }
                )
                ToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Push Notifications",
                    description = "Enable system-wide notifications",
                    checked = config.notificationsEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(notificationsEnabled = it)) }
                )
                ToggleItem(
                    icon = Icons.Default.AccountTree,
                    title = "Role UI Visibility",
                    description = "Show role selection in registration",
                    checked = config.roleUiEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(roleUiEnabled = it)) }
                )
            }

            SettingsCategory(title = "System Control") {
                ToggleItem(
                    icon = Icons.Default.Build,
                    title = "Maintenance Mode",
                    description = "Lock app for all users except admins",
                    checked = config.maintenanceMode,
                    onCheckedChange = { viewModel.updateConfig(config.copy(maintenanceMode = it)) }
                )
                ToggleItem(
                    icon = Icons.Default.PersonAdd,
                    title = "User Registration",
                    description = "Allow new users to create accounts",
                    checked = config.registrationEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(registrationEnabled = it)) }
                )
                ToggleItem(
                    icon = Icons.Default.ShoppingCart,
                    title = "Premium Purchases",
                    description = "Allow users to buy premium plans",
                    checked = config.premiumPurchaseEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(premiumPurchaseEnabled = it)) }
                )
            }

            SettingsCategory(title = "Announcements") {
                ToggleItem(
                    icon = Icons.Default.Campaign,
                    title = "Enable Announcement",
                    description = "Show announcement banner to users",
                    checked = config.announcementEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(announcementEnabled = it)) }
                )
                OutlinedTextField(
                    value = config.announcementTitle,
                    onValueChange = { viewModel.updateConfig(config.copy(announcementTitle = it)) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = config.announcementMessage,
                    onValueChange = { viewModel.updateConfig(config.copy(announcementMessage = it)) },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            SettingsCategory(title = "Support & Share Links") {
                OutlinedTextField(
                    value = config.supportWhatsapp,
                    onValueChange = { viewModel.updateConfig(config.copy(supportWhatsapp = it)) },
                    label = { Text("Support WhatsApp Number") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = config.supportEmail,
                    onValueChange = { viewModel.updateConfig(config.copy(supportEmail = it)) },
                    label = { Text("Support Email") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = config.shareTitle,
                    onValueChange = { viewModel.updateConfig(config.copy(shareTitle = it)) },
                    label = { Text("Share Title") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = config.shareText,
                    onValueChange = { viewModel.updateConfig(config.copy(shareText = it)) },
                    label = { Text("Share Text") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = config.shareLink,
                    onValueChange = { viewModel.updateConfig(config.copy(shareLink = it)) },
                    label = { Text("Share Link") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            SettingsCategory(title = "Updates") {
                ToggleItem(
                    icon = Icons.Default.Update,
                    title = "Force Update",
                    description = "Require users to update app",
                    checked = config.forceUpdateEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(forceUpdateEnabled = it)) }
                )
                OutlinedTextField(
                    value = config.minVersionCode.toString(),
                    onValueChange = { viewModel.updateConfig(config.copy(minVersionCode = it.toIntOrNull() ?: 1)) },
                    label = { Text("Minimum Version Code") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout Admin Session")
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun ToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else null
        )
    }
}
