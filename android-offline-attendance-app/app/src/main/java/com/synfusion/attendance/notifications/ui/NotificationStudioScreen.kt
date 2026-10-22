package com.synfusion.attendance.notifications.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationPayload
import com.synfusion.attendance.notifications.model.NotificationVisualStyle
import com.synfusion.attendance.notifications.registry.NotificationTemplateRegistry
import com.synfusion.attendance.notifications.studio.NotificationStudioUiState
import com.synfusion.attendance.notifications.studio.NotificationStudioViewModel
import com.synfusion.attendance.notifications.studio.StudioPreviewMode
import com.synfusion.attendance.ui.components.icons.WorkDailyIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationStudioScreen(
    viewModel: NotificationStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredList = viewModel.filteredTemplates
    val currentTemplate = viewModel.currentTemplate

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notification Studio", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Catalog Test Mode (${if (filteredList.isNotEmpty()) uiState.currentIndex + 1 else 0}/${filteredList.size})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(WorkDailyIcons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkModePreview() }) {
                        Icon(
                            imageVector = if (uiState.isDarkModePreview) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme Preview"
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All (57)") }
                    )
                }
                items(NotificationCategory.values()) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.displayName) }
                    )
                }
            }

            // Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StudioPreviewMode.values().forEach { mode ->
                    val selected = uiState.previewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { viewModel.setPreviewMode(mode) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                StudioPreviewMode.RICH_IN_APP -> "In-App Poster"
                                StudioPreviewMode.COLLAPSED_SYSTEM -> "Compact System"
                                StudioPreviewMode.EXPANDED_SYSTEM -> "Expanded System"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Main Interactive Visual Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (currentTemplate != null) {
                    val previewTheme = if (uiState.isDarkModePreview) darkColorScheme() else lightColorScheme()
                    MaterialTheme(colorScheme = previewTheme) {
                        when (uiState.previewMode) {
                            StudioPreviewMode.RICH_IN_APP -> TangoRichNotificationCard(payload = currentTemplate)
                            StudioPreviewMode.COLLAPSED_SYSTEM -> SystemCompactNotificationPreview(payload = currentTemplate)
                            StudioPreviewMode.EXPANDED_SYSTEM -> SystemExpandedNotificationPreview(payload = currentTemplate)
                        }
                    }
                } else {
                    Text("No notification templates available for this category", color = Color.Gray)
                }
            }

            // Bottom Navigation & Test Action Toolbar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousTemplate() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(WorkDailyIcons.ChevronLeft, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }

                        IconButton(onClick = { viewModel.toggleAutoCarousel() }) {
                            Icon(
                                imageVector = if (uiState.isAutoCarouselRunning) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Auto Carousel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.nextTemplate() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(WorkDailyIcons.ChevronRight, contentDescription = null)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.playTestSound() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Sound", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.triggerLocalTestNotification() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Push", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Tango-inspired Split-Art Rich Poster Notification Card (60% Info, 40% Artwork)
@Composable
fun TangoRichNotificationCard(
    payload: NotificationPayload,
    modifier: Modifier = Modifier
) {
    val style = payload.style
    val event = payload.event

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(style.gradientColors))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF12131C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Info Area (60%)
                    Column(
                        modifier = Modifier
                            .weight(0.65f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Label Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = style.primaryColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, style.primaryColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = style.categoryLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = style.primaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Headline Title
                        Text(
                            text = event.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Body Description
                        Text(
                            text = event.body,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action CTA Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = style.primaryColor,
                            modifier = Modifier.clickable { }
                        ) {
                            Text(
                                text = style.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Right Split-Art Visual Area (40%)
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(style.primaryColor.copy(alpha = 0.4f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (style.illustrationResId != null) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = style.illustrationResId),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = style.primaryColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemCompactNotificationPreview(payload: NotificationPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2B36))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(payload.style.primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("WorkDaily • ${payload.style.categoryLabel}", fontSize = 10.sp, color = Color.Gray)
                    Text("Now", fontSize = 10.sp, color = Color.Gray)
                }
                Text(payload.event.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(payload.event.body, fontSize = 11.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SystemExpandedNotificationPreview(payload: NotificationPayload) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2B36))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemCompactNotificationPreview(payload = payload)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(payload.style.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${payload.style.categoryLabel}\n${payload.event.title}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
