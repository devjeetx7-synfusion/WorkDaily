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
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadConfig() }) {
                            Text("Retry")
                        }
                    }
                }
                is ConfigUiState.Success -> {
                    var config by remember(state.config) { mutableStateOf(state.config) }
                    var isSaving by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                    ) {
                        Text(
                            text = "Release Version Gate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = config.minSupportedVersionCode.toString(),
                                onValueChange = {
                                    val code = it.toIntOrNull() ?: config.minSupportedVersionCode
                                    config = config.copy(minSupportedVersionCode = code)
                                },
                                label = { Text("Min Version") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = config.latestVersionCode.toString(),
                                onValueChange = {
                                    val code = it.toIntOrNull() ?: config.latestVersionCode
                                    config = config.copy(latestVersionCode = code)
                                },
                                label = { Text("Latest Version") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Feature Toggles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Force App Update")
                                    Switch(
                                        checked = config.forceUpdate,
                                        onCheckedChange = { config = config.copy(forceUpdate = it) }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Maintenance Mode")
                                    Switch(
                                        checked = config.maintenanceMode,
                                        onCheckedChange = { config = config.copy(maintenanceMode = it) }
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Live Streaming Feature")
                                    Switch(
                                        checked = config.liveEnabled,
                                        onCheckedChange = { config = config.copy(liveEnabled = it) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isSaving = true
                                viewModel.updateConfig(config) { _, _ -> isSaving = false }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Configuration")
                            }
                        }
                    }
                }
            }
        }
    }
}
