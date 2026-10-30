package com.synfusion.workdailyadmin.ui.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synfusion.workdailyadmin.data.models.AppConfigData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    viewModel: ConfigViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote App Configuration", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadConfig() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is ConfigUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ConfigUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadConfig() }) { Text("Retry") }
                    }
                }
                is ConfigUiState.Success -> {
                    var minVer by remember { mutableStateOf(state.config.minSupportedVersionCode.toString()) }
                    var latestVer by remember { mutableStateOf(state.config.latestVersionCode.toString()) }
                    var maintMode by remember { mutableStateOf(state.config.maintenanceMode) }
                    var liveActive by remember { mutableStateOf(state.config.liveEnabled) }
                    var socialActive by remember { mutableStateOf(state.config.socialEnabled) }
                    var rewardsActive by remember { mutableStateOf(state.config.rewardsEnabled) }
                    var supportEmail by remember { mutableStateOf(state.config.supportEmail) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Version Gating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = minVer,
                                    onValueChange = { minVer = it },
                                    label = { Text("Minimum Supported Version Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = latestVer,
                                    onValueChange = { latestVer = it },
                                    label = { Text("Latest Version Code") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("System Feature Flags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Maintenance Mode")
                                    Switch(checked = maintMode, onCheckedChange = { maintMode = it })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Live Streaming Access")
                                    Switch(checked = liveActive, onCheckedChange = { liveActive = it })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Social Messaging Access")
                                    Switch(checked = socialActive, onCheckedChange = { socialActive = it })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Rewards Hub Access")
                                    Switch(checked = rewardsActive, onCheckedChange = { rewardsActive = it })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val newCfg = AppConfigData(
                                    minSupportedVersionCode = minVer.toIntOrNull() ?: 1,
                                    latestVersionCode = latestVer.toIntOrNull() ?: 1,
                                    maintenanceMode = maintMode,
                                    liveEnabled = liveActive,
                                    socialEnabled = socialActive,
                                    rewardsEnabled = rewardsActive,
                                    supportEmail = supportEmail
                                )
                                viewModel.updateConfig(newCfg) { _, _ -> }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Publish Configuration")
                        }
                    }
                }
            }
        }
    }
}
