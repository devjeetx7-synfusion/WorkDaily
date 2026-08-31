package com.synfusion.attendance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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

    // Requirement 4 & 11: Settings is ALWAYS fixed as the right circular button.
    val rightItem = remember { WorkDailyNavItem.SETTINGS }

    // Dynamic second slot in left capsule to display the active screen if it's not leftItem1 and not Settings
    val leftItem2 = remember(leftRoute2, currentRoute, leftItem1.route) {
        if (currentRoute != leftItem1.route && currentRoute != rightItem.route) {
            WorkDailyNavItem.fromRoute(currentRoute)
        } else {
            WorkDailyNavItem.fromRoute(leftRoute2)
        }
    }

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

            val topThreshold = minTop - with(density) { 150.dp.toPx() }
            val bottomThreshold = maxBottom + with(density) { 120.dp.toPx() }

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
        // Subdued background scrim when long-press arc selector is expanded
        AnimatedVisibility(
            visible = showSelector,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
            )
        }

        // Floating Curved Arc Drag-to-Select Overlay for Long-Press
        if (showSelector) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 76.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectorItems.forEachIndexed { index, option ->
                        val isHovered = (hoveredItem == option)
                        val scale by animateFloatAsState(
                            targetValue = if (isHovered) 1.35f else 0.88f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "optionScale"
                        )

                        val elevation by animateDpAsState(
                            targetValue = if (isHovered) 8.dp else 4.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "optionElevation"
                        )

                        // Shallow arc curve Y offset (outer items slightly higher, inner items slightly lower)
                        val arcYOffset = when (index) {
                            0 -> (-6).dp
                            1 -> 2.dp
                            2 -> 2.dp
                            3 -> (-6).dp
                            else -> 0.dp
                        }

                        Surface(
                            modifier = Modifier
                                .offset(y = arcYOffset)
                                .onGloballyPositioned { coords ->
                                    optionBoundsMap[option] = coords.boundsInWindow()
                                }
                                .scale(scale)
                                .size(48.dp)
                                .shadow(
                                    elevation = elevation,
                                    shape = CircleShape,
                                    ambientColor = Color.Black.copy(alpha = 0.06f),
                                    spotColor = Color.Black.copy(alpha = 0.10f)
                                ),
                            shape = CircleShape,
                            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (!isHovered) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)) else null
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.title,
                                    tint = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
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
                        onItemLongPress = { item, bounds ->
                            homeAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = item
                            showSelector = true
                        },
                        onDrag = onDragHandler,
                        onRelease = onReleaseHandler
                    )

                    FloatingNavItemButton(
                        item = leftItem2,
                        isSelected = currentRoute == leftItem2.route,
                        onNavigate = onNavigate,
                        onItemLongPress = { item, bounds ->
                            homeAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = item
                            showSelector = true
                        },
                        onDrag = onDragHandler,
                        onRelease = onReleaseHandler
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT CIRCLE (1 Item - Permanently Settings)
            Surface(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
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
                        onItemLongPress = { item, bounds ->
                            homeAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = item
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
    onItemLongPress: (WorkDailyNavItem, Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onRelease: () -> Unit
) {
    var itemBounds by remember { mutableStateOf(Rect.Zero) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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

    val modifier = Modifier
        .onGloballyPositioned { coords ->
            itemBounds = coords.boundsInWindow()
        }
        .pointerInput(item.route) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var isLongPressTriggered = false
                val longPressJob = coroutineScope.launch {
                    delay(280)
                    isLongPressTriggered = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onItemLongPress(item, itemBounds)
                }

                var pointerUp = false
                do {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                    if (change != null) {
                        if (isLongPressTriggered) {
                            change.consume()
                            val fingerWindowPos = itemBounds.topLeft + change.position
                            onDrag(fingerWindowPos)
                        }
                        if (!change.pressed) {
                            pointerUp = true
                        }
                    } else {
                        pointerUp = true
                    }
                } while (!pointerUp)

                longPressJob.cancel()
                if (isLongPressTriggered) {
                    onRelease()
                } else {
                    onNavigate(item.route)
                }
            }
        }
        .clip(if (isSingleCircle) CircleShape else RoundedCornerShape(24.dp))
        .background(backgroundColor)
        .padding(
            horizontal = if (isSingleCircle) 14.dp else if (isSelected) 18.dp else 14.dp,
            vertical = 10.dp
        )

    Box(
        modifier = modifier,
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
