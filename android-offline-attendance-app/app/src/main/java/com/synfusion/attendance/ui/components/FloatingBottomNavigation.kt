package com.synfusion.attendance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class WorkDailyNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("dashboard", "Home", Icons.Default.Home),
    CALENDAR("calendar", "Calendar", Icons.Default.CalendarMonth),
    STATISTICS("stats", "Statistics", Icons.Default.BarChart),
    PASSBOOK("passbook", "Passbook", Icons.Default.Description),
    SETTINGS("settings", "Settings", Icons.Default.Settings);

    companion object {
        fun fromRoute(route: String): WorkDailyNavItem {
            return values().firstOrNull { it.route == route } ?: HOME
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    currentRoute: String,
    leftRoute1: String = "dashboard",
    leftRoute2: String = "passbook",
    rightRoute: String = "settings",
    onNavigate: (String) -> Unit
) {
    val leftItem1 = remember(leftRoute1) { WorkDailyNavItem.fromRoute(leftRoute1) }
    val leftItem2 = remember(leftRoute2) { WorkDailyNavItem.fromRoute(leftRoute2) }
    val rightItem = remember(rightRoute) { WorkDailyNavItem.fromRoute(rightRoute) }

    var showSelector by remember { mutableStateOf(false) }
    var hoveredItem by remember { mutableStateOf<WorkDailyNavItem?>(null) }
    var homeAnchorBounds by remember { mutableStateOf(Rect.Zero) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Selector options to display on Home long press (Home, Calendar, Statistics, Passbook)
    val selectorItems = remember {
        listOf(
            WorkDailyNavItem.HOME,
            WorkDailyNavItem.CALENDAR,
            WorkDailyNavItem.STATISTICS,
            WorkDailyNavItem.PASSBOOK
        )
    }

    val optionBoundsMap = remember { mutableStateMapOf<WorkDailyNavItem, Rect>() }

    val onDragHandler: (Offset) -> Unit = { touchWindowPos ->
        if (optionBoundsMap.isNotEmpty()) {
            val minTop = optionBoundsMap.values.minOf { it.top }
            val maxBottom = optionBoundsMap.values.maxOf { it.bottom }

            // Vertical cancellation bounds check (-120dp above, +100dp below)
            val topThreshold = minTop - with(density) { 120.dp.toPx() }
            val bottomThreshold = maxBottom + with(density) { 100.dp.toPx() }

            if (touchWindowPos.y < topThreshold || touchWindowPos.y > bottomThreshold) {
                if (hoveredItem != null) {
                    hoveredItem = null
                }
            } else {
                var closestOption: WorkDailyNavItem? = null
                var minDistance = Float.MAX_VALUE

                val sortedOptions = selectorItems.mapNotNull { option ->
                    optionBoundsMap[option]?.let { bounds -> option to bounds }
                }.sortedBy { it.second.center.x }

                if (sortedOptions.isNotEmpty()) {
                    val minX = sortedOptions.first().second.center.x
                    val maxX = sortedOptions.last().second.center.x

                    // Continuous horizontal tracking with edge clamping
                    if (touchWindowPos.x <= minX) {
                        closestOption = sortedOptions.first().first
                    } else if (touchWindowPos.x >= maxX) {
                        closestOption = sortedOptions.last().first
                    } else {
                        sortedOptions.forEach { (option, bounds) ->
                            val dist = abs(touchWindowPos.x - bounds.center.x)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestOption = option
                            }
                        }
                    }
                }

                val newHovered = closestOption ?: WorkDailyNavItem.HOME
                if (newHovered != hoveredItem) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    hoveredItem = newHovered
                }
            }
        }
    }

    val onReleaseHandler: () -> Unit = {
        if (showSelector) {
            val target = hoveredItem
            showSelector = false
            hoveredItem = null
            if (target != null) {
                onNavigate(target.route)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Compact Drag-to-Select Overlay for Home Long-Press
        if (showSelector) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 68.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectorItems.forEach { option ->
                        val isHovered = (hoveredItem == option)
                        val scale by animateFloatAsState(
                            targetValue = if (isHovered) 1.25f else 0.85f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "optionScale"
                        )

                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coords ->
                                    optionBoundsMap[option] = coords.boundsInWindow()
                                }
                                .scale(scale)
                                .size(44.dp)
                                .then(
                                    if (isHovered) {
                                        Modifier.background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.title,
                                tint = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Standard Floating Bottom Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT CAPSULE (2 Items)
            Surface(
                modifier = Modifier
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingNavItemButton(
                        item = leftItem1,
                        isSelected = currentRoute == leftItem1.route,
                        onNavigate = onNavigate,
                        onHomeLongPress = { bounds ->
                            homeAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = leftItem1
                            showSelector = true
                        },
                        onDrag = onDragHandler,
                        onRelease = onReleaseHandler
                    )

                    FloatingNavItemButton(
                        item = leftItem2,
                        isSelected = currentRoute == leftItem2.route,
                        onNavigate = onNavigate,
                        onHomeLongPress = { bounds ->
                            homeAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = leftItem2
                            showSelector = true
                        },
                        onDrag = onDragHandler,
                        onRelease = onReleaseHandler
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT CIRCLE (1 Item)
            Surface(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingNavItemButton(
                        item = rightItem,
                        isSelected = currentRoute == rightItem.route,
                        isSingleCircle = true,
                        onNavigate = onNavigate,
                        onHomeLongPress = { bounds ->
                            homeAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = rightItem
                            showSelector = true
                        },
                        onDrag = onDragHandler,
                        onRelease = onReleaseHandler
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItemButton(
    item: WorkDailyNavItem,
    isSelected: Boolean,
    isSingleCircle: Boolean = false,
    onNavigate: (String) -> Unit,
    onHomeLongPress: (Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onRelease: () -> Unit
) {
    var itemBounds by remember { mutableStateOf(Rect.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isHomeItem = (item == WorkDailyNavItem.HOME)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "navBgColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "navContentColor"
    )

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                itemBounds = coords.boundsInWindow()
            }
            .pointerInput(item, isHomeItem) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (isHomeItem) {
                        var isLongPressTriggered = false
                        val longPressJob = coroutineScope.launch {
                            delay(350)
                            isLongPressTriggered = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHomeLongPress(itemBounds)
                        }

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pointerChange = event.changes.firstOrNull() ?: break
                            if (isLongPressTriggered) {
                                pointerChange.consume()
                                val fingerWindowPos = itemBounds.topLeft + pointerChange.position
                                onDrag(fingerWindowPos)
                            }
                        } while (event.changes.any { it.pressed })

                        longPressJob.cancel()
                        if (isLongPressTriggered) {
                            onRelease()
                        } else {
                            onNavigate(item.route)
                        }
                    } else {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            onNavigate(item.route)
                        }
                    }
                }
            }
            .clip(if (isSingleCircle) CircleShape else RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(
                horizontal = if (isSingleCircle) 14.dp else if (isSelected) 18.dp else 14.dp,
                vertical = 10.dp
            ),
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
                modifier = Modifier.size(24.dp)
            )

            if (isSelected && !isSingleCircle) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}
