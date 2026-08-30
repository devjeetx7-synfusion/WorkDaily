package com.synfusion.attendance.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var selectorAnchorBounds by remember { mutableStateOf(Rect.Zero) }

    // Selector options to display on long press (Home, Calendar, Statistics, Passbook)
    val selectorItems = remember {
        listOf(
            WorkDailyNavItem.HOME,
            WorkDailyNavItem.CALENDAR,
            WorkDailyNavItem.STATISTICS,
            WorkDailyNavItem.PASSBOOK
        )
    }

    val optionBoundsMap = remember { mutableStateMapOf<WorkDailyNavItem, Rect>() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
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
                        onLongPress = { bounds ->
                            selectorAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = leftItem1
                            showSelector = true
                        },
                        onDrag = { touchWindowPos ->
                            var found: WorkDailyNavItem? = null
                            optionBoundsMap.forEach { (navItem, rect) ->
                                if (rect.contains(touchWindowPos)) {
                                    found = navItem
                                }
                            }
                            hoveredItem = found
                        },
                        onRelease = {
                            if (showSelector) {
                                hoveredItem?.let { target ->
                                    onNavigate(target.route)
                                }
                                showSelector = false
                                hoveredItem = null
                            }
                        }
                    )

                    FloatingNavItemButton(
                        item = leftItem2,
                        isSelected = currentRoute == leftItem2.route,
                        onNavigate = onNavigate,
                        onLongPress = { bounds ->
                            selectorAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = leftItem2
                            showSelector = true
                        },
                        onDrag = { touchWindowPos ->
                            var found: WorkDailyNavItem? = null
                            optionBoundsMap.forEach { (navItem, rect) ->
                                if (rect.contains(touchWindowPos)) {
                                    found = navItem
                                }
                            }
                            hoveredItem = found
                        },
                        onRelease = {
                            if (showSelector) {
                                hoveredItem?.let { target ->
                                    onNavigate(target.route)
                                }
                                showSelector = false
                                hoveredItem = null
                            }
                        }
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
                        onLongPress = { bounds ->
                            selectorAnchorBounds = bounds
                            optionBoundsMap.clear()
                            hoveredItem = rightItem
                            showSelector = true
                        },
                        onDrag = { touchWindowPos ->
                            var found: WorkDailyNavItem? = null
                            optionBoundsMap.forEach { (navItem, rect) ->
                                if (rect.contains(touchWindowPos)) {
                                    found = navItem
                                }
                            }
                            hoveredItem = found
                        },
                        onRelease = {
                            if (showSelector) {
                                hoveredItem?.let { target ->
                                    onNavigate(target.route)
                                }
                                showSelector = false
                                hoveredItem = null
                            }
                        }
                    )
                }
            }
        }

        // Long-Press Floating Activity Selector Overlay Card
        if (showSelector) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -180),
                onDismissRequest = {
                    showSelector = false
                    hoveredItem = null
                },
                properties = PopupProperties(focusable = false)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .shadow(20.dp, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Quick Jump",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectorItems.forEach { option ->
                                val isHovered = hoveredItem == option
                                val scale by animateFloatAsState(
                                    targetValue = if (isHovered) 1.25f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "optionScale"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            optionBoundsMap[option] = coords.boundsInWindow()
                                        }
                                        .scale(scale)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = option.title,
                                        tint = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isHovered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
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
    onLongPress: (Rect) -> Unit,
    onDrag: (Offset) -> Unit,
    onRelease: () -> Unit
) {
    var itemBounds by remember { mutableStateOf(Rect.Zero) }
    val coroutineScope = rememberCoroutineScope()

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
            .pointerInput(item) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent(PointerEventPass.Main)
                        if (down.type == PointerEventType.Press) {
                            var isLongPressTriggered = false
                            val longPressJob = coroutineScope.launch {
                                delay(350)
                                isLongPressTriggered = true
                                onLongPress(itemBounds)
                            }

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val currentPointer = event.changes.firstOrNull() ?: break

                                if (isLongPressTriggered) {
                                    onDrag(currentPointer.position + itemBounds.topLeft)
                                }

                                if (event.type == PointerEventType.Release) {
                                    longPressJob.cancel()
                                    if (isLongPressTriggered) {
                                        onRelease()
                                    } else {
                                        // Short tap
                                        onNavigate(item.route)
                                    }
                                    break
                                }
                            }
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
