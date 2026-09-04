package com.synfusion.attendance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.ui.components.WorkDailyNavItem
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeNavigationScreen(
    viewModel: OfflineSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            WorkDailyAppBar(
                title = "Customize Navigation",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NavigationCustomizationContent(
                    state = state,
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationCustomizationContent(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel
) {
    val capsuleOptions = remember {
        listOf(
            "dashboard" to "Home",
            "calendar" to "Calendar",
            "stats" to "Statistics"
        )
    }

    val startupOptions = remember {
        listOf(
            "dashboard" to "Home",
            "calendar" to "Calendar",
            "stats" to "Statistics",
            "settings" to "Settings"
        )
    }

    val selectedItem = remember(state.navLeft1) {
        WorkDailyNavItem.fromRoute(state.navLeft1)
    }

    val secondaryItem = remember(state.navLeft2) {
        WorkDailyNavItem.fromRoute(state.navLeft2)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Customize Navigation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Left Capsule (2 Items)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                // Premium Floating Bottom Navigation Bar Live Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(vertical = 20.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "LIVE NAVIGATION PREVIEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Capsule (2 Items)
                            Surface(
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Item 1 (Active Highlight)
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = selectedItem.icon,
                                                contentDescription = selectedItem.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = selectedItem.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Item 2 (Inactive Subdued)
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = secondaryItem.icon,
                                                contentDescription = secondaryItem.title,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right Circle (Fixed Settings)
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                shadowElevation = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = WorkDailyNavItem.SETTINGS.icon,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Left Item 1 Selector
                NavPositionDropdown(
                    label = "Left Item 1",
                    selectedRoute = state.navLeft1,
                    options = capsuleOptions,
                    onSelect = { newRoute ->
                        if (newRoute != state.navLeft2) {
                            viewModel.updateNavigationConfig(newRoute, state.navLeft2, "settings")
                        } else {
                            val oldRoute = state.navLeft1
                            viewModel.updateNavigationConfig(newRoute, oldRoute, "settings")
                        }
                    }
                )

                // Left Item 2 Selector
                NavPositionDropdown(
                    label = "Left Item 2",
                    selectedRoute = state.navLeft2,
                    options = capsuleOptions,
                    onSelect = { newRoute ->
                        if (newRoute != state.navLeft1) {
                            viewModel.updateNavigationConfig(state.navLeft1, newRoute, "settings")
                        } else {
                            val oldRoute = state.navLeft2
                            viewModel.updateNavigationConfig(oldRoute, newRoute, "settings")
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Right Button (Fixed)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Settings is permanently assigned to the right circular button", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("Startup & Persistence", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                NavPositionDropdown(
                    label = "Startup Screen",
                    selectedRoute = state.startupScreen,
                    options = startupOptions,
                    onSelect = { newRoute ->
                        viewModel.updateStartupScreen(newRoute)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remember Last Screen", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Open the last viewed screen on launch", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.rememberLastScreen,
                        onCheckedChange = { viewModel.updateRememberLastScreen(it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavPositionDropdown(
    label: String,
    selectedRoute: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentTitle = options.firstOrNull { it.first == selectedRoute }?.second ?: "Home"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (route, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelect(route)
                        expanded = false
                    }
                )
            }
        }
    }
}
