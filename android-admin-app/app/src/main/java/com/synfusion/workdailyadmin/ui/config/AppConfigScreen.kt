package com.synfusion.workdailyadmin.ui.config

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.models.AppConfigData
import com.synfusion.workdailyadmin.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    viewModel: ConfigViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Remote App Configuration", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Live runtime variables, flags & client version enforcement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadConfig() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Configuration")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ConfigUiState.Loading -> {
                    AdminLoadingState(message = "Retrieving remote configuration settings...")
                }
                is ConfigUiState.Error -> {
                    AdminErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadConfig() }
                    )
                }
                is ConfigUiState.Success -> {
                    ConfigForm(
                        initialConfig = state.config,
                        onSave = { updated ->
                            viewModel.updateConfig(updated) { _, _ -> }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigForm(
    initialConfig: AppConfigData,
    onSave: (AppConfigData) -> Unit
) {
    var minVersion by remember(initialConfig) { mutableStateOf(initialConfig.minSupportedVersionCode.toString()) }
    var latestVersion by remember(initialConfig) { mutableStateOf(initialConfig.latestVersionCode.toString()) }
    var announcementTitle by remember(initialConfig) { mutableStateOf(initialConfig.updateAnnouncementTitle) }
    var announcementMessage by remember(initialConfig) { mutableStateOf(initialConfig.updateAnnouncementMessage) }
    var forceUpdate by remember(initialConfig) { mutableStateOf(initialConfig.forceUpdate) }
    var maintenanceMode by remember(initialConfig) { mutableStateOf(initialConfig.maintenanceMode) }
    var liveEnabled by remember(initialConfig) { mutableStateOf(initialConfig.liveEnabled) }
    var socialEnabled by remember(initialConfig) { mutableStateOf(initialConfig.socialEnabled) }
    var rewardsEnabled by remember(initialConfig) { mutableStateOf(initialConfig.rewardsEnabled) }
    var supportEmail by remember(initialConfig) { mutableStateOf(initialConfig.supportEmail) }
    var supportWhatsappUrl by remember(initialConfig) { mutableStateOf(initialConfig.supportWhatsappUrl) }

    var showSaveConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Section: System Availability & Flags
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Operational Flags & Switches",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                ConfigToggleRow(
                    label = "Maintenance Mode",
                    description = "When active, client apps show maintenance wall and block writes.",
                    checked = maintenanceMode,
                    checkedColor = Color(0xFFEF4444),
                    onCheckedChange = { maintenanceMode = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                ConfigToggleRow(
                    label = "Live Broadcasting Engine",
                    description = "Permit creators to start LiveKit video/audio rooms.",
                    checked = liveEnabled,
                    onCheckedChange = { liveEnabled = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                ConfigToggleRow(
                    label = "Social & Creator Profiles",
                    description = "Enable public creator profiles and member discovery.",
                    checked = socialEnabled,
                    onCheckedChange = { socialEnabled = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                ConfigToggleRow(
                    label = "Rewards & Economy System",
                    description = "Allow coins check-in, builder spin wheel, and redemptions.",
                    checked = rewardsEnabled,
                    onCheckedChange = { rewardsEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Version Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Release & Version Governance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = minVersion,
                        onValueChange = { minVersion = it },
                        label = { Text("Min Version Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = latestVersion,
                        onValueChange = { latestVersion = it },
                        label = { Text("Latest Version Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ConfigToggleRow(
                    label = "Force Upgrade Gate",
                    description = "Require clients below min version code to update before app launch.",
                    checked = forceUpdate,
                    checkedColor = Color(0xFFF59E0B),
                    onCheckedChange = { forceUpdate = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = announcementTitle,
                    onValueChange = { announcementTitle = it },
                    label = { Text("Update Announcement Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = announcementMessage,
                    onValueChange = { announcementMessage = it },
                    label = { Text("Update Announcement Message") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Support Channels
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Support & Contact Endpoints",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = supportEmail,
                    onValueChange = { supportEmail = it },
                    label = { Text("Official Support Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = supportWhatsappUrl,
                    onValueChange = { supportWhatsappUrl = it },
                    label = { Text("WhatsApp Support Link") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Button(
            onClick = { showSaveConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish Remote Configuration", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSaveConfirm) {
        ConfirmationDialog(
            title = "Confirm Remote Config Update",
            message = "These changes will immediately take effect across all client installations and sessions worldwide. Do you wish to proceed?",
            confirmText = "Publish Now",
            dismissText = "Cancel",
            isDestructive = false,
            onConfirm = {
                showSaveConfirm = false
                val minV = minVersion.toIntOrNull() ?: initialConfig.minSupportedVersionCode
                val latV = latestVersion.toIntOrNull() ?: initialConfig.latestVersionCode
                onSave(
                    initialConfig.copy(
                        minSupportedVersionCode = minV,
                        latestVersionCode = latV,
                        updateAnnouncementTitle = announcementTitle,
                        updateAnnouncementMessage = announcementMessage,
                        forceUpdate = forceUpdate,
                        maintenanceMode = maintenanceMode,
                        liveEnabled = liveEnabled,
                        socialEnabled = socialEnabled,
                        rewardsEnabled = rewardsEnabled,
                        supportEmail = supportEmail,
                        supportWhatsappUrl = supportWhatsappUrl
                    )
                )
            },
            onDismiss = { showSaveConfirm = false }
        )
    }
}

@Composable
private fun ConfigToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = checkedColor
            )
        )
    }
}
