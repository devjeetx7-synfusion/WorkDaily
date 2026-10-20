package com.synfusion.attendance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WorkDailyNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("dashboard", "Home", Icons.Default.Home),
    CALENDAR("calendar", "Calendar", Icons.Default.CalendarMonth),
    STATISTICS("stats", "Stats", Icons.Default.BarChart),
    LIVE("live_discover", "Live", Icons.Default.LiveTv);

    companion object {
        fun fromRoute(route: String): WorkDailyNavItem {
            return values().firstOrNull { it.route == route } ?: HOME
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val mainTabs = remember {
        listOf(
            WorkDailyNavItem.HOME,
            WorkDailyNavItem.CALENDAR,
            WorkDailyNavItem.STATISTICS
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        val capsuleBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First Capsule: Main Tabs (Home, Calendar, Stats)
            Surface(
                modifier = Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, capsuleBorderColor),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mainTabs.forEach { item ->
                        val isSelected = currentRoute == item.route
                        DynamicNavItemButton(
                            item = item,
                            isSelected = isSelected,
                            onNavigate = onNavigate
                        )
                    }
                }
            }

            // Second Capsule: Live Tab
            val isLiveSelected = currentRoute == WorkDailyNavItem.LIVE.route
            val liveCapsuleBgColor by animateColorAsState(
                targetValue = if (isLiveSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                animationSpec = tween(durationMillis = 200),
                label = "liveCapsuleBgColor"
            )

            Surface(
                modifier = Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    ),
                shape = CircleShape,
                color = liveCapsuleBgColor,
                border = BorderStroke(1.dp, capsuleBorderColor),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsNavItemButton(
                        item = WorkDailyNavItem.LIVE,
                        isSelected = isLiveSelected,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicNavItemButton(
    item: WorkDailyNavItem,
    isSelected: Boolean,
    onNavigate: (String) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "dynamicNavBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "dynamicNavContentColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onNavigate(item.route)
            }
            .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkHorizontally(animationSpec = tween(200))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsNavItemButton(
    item: WorkDailyNavItem,
    isSelected: Boolean,
    onNavigate: (String) -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "settingsNavContentColor"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onNavigate(item.route)
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
