package com.synfusion.attendance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.viewmodel.OfflinePassbookLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrillDownBottomSheet(
    title: String,
    logs: List<OfflinePassbookLog>,
    onDismiss: () -> Unit,
    totalRecordsText: String,
    totalAmountText: String? = null,
    currencyCode: String = "INR"
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(false) }

    val filteredLogs = remember(searchQuery, sortAscending, logs) {
        val filtered = if (searchQuery.isBlank()) {
            logs
        } else {
            logs.filter {
                it.date.contains(searchQuery, ignoreCase = true) ||
                (it.note?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        if (sortAscending) {
            filtered.sortedBy { it.date }
        } else {
            filtered.sortedByDescending { it.date }
        }
    }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    // Wrap content when empty, but fill 90% when records exist
    val sheetModifier = if (logs.isEmpty()) {
        Modifier.wrapContentHeight()
    } else {
        Modifier.fillMaxHeight(0.9f)
    }

    val columnModifier = if (logs.isEmpty()) {
        Modifier.fillMaxWidth().wrapContentHeight().clip(sheetShape)
    } else {
        Modifier.fillMaxSize().clip(sheetShape)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets(0.dp),
        shape = sheetShape,
        modifier = sheetModifier
    ) {
        Column(
            modifier = columnModifier
        ) {
            // Header Row (Always shown)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            val isLandscape = com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo.current.isLandscape
            if (logs.isEmpty()) {
                // Phase 2 Contextual Empty State with smooth staggered animations
                ContextualEmptyState(title = title)
                Spacer(modifier = Modifier.height(24.dp))
            } else if (isLandscape) {
                // Two-Column Responsive Landscape Composition
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // LEFT COLUMN: Search, Sorting, and Sticky Summary
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.search_records)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = CircleShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(onClick = { sortAscending = !sortAscending }) {
                                Text(if (sortAscending) stringResource(R.string.sort_oldest_first) else stringResource(R.string.sort_newest_first))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_records_drilldown), fontWeight = FontWeight.Bold)
                                    Text(totalRecordsText, fontWeight = FontWeight.Bold)
                                }
                                if (totalAmountText != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_amount_drilldown), fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                        Text(totalAmountText, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Scrollable Records List
                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        if (filteredLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_records_found_dots), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredLogs, key = { it.date }) { log ->
                                    DrillDownRecordItem(log, currencyCode)
                                }
                            }
                        }
                    }
                }
            } else {
                // Populate/Populated State Controls (Portrait Layout)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.search_records)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = CircleShape,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { sortAscending = !sortAscending }) {
                        Text(if (sortAscending) stringResource(R.string.sort_oldest_first) else stringResource(R.string.sort_newest_first))
                    }
                }

                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.no_records_found_dots), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredLogs, key = { it.date }) { log ->
                            DrillDownRecordItem(log, currencyCode)
                        }
                    }
                }

                // Sticky Summary Footer (Hides entirely when list is empty)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_records_drilldown), fontWeight = FontWeight.Bold)
                            Text(totalRecordsText, fontWeight = FontWeight.Bold)
                        }
                        if (totalAmountText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_amount_drilldown), fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                                Text(totalAmountText, fontWeight = FontWeight.Bold, color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContextualEmptyState(title: String) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 100),
        label = "fade"
    )
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.85f,
        animationSpec = tween(durationMillis = 400, delayMillis = 100),
        label = "scale"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 250),
        label = "title_fade"
    )
    val descAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 400),
        label = "desc_fade"
    )

    val lowerTitle = title.lowercase()
    val (icon, tint, displayTitle, displayDesc) = when {
        lowerTitle.contains("advance") -> {
            EmptyStateDetails(
                Icons.Default.AccountBalanceWallet,
                MaterialTheme.colorScheme.primary,
                stringResource(R.string.no_advance_records_title),
                stringResource(R.string.no_advance_records_desc)
            )
        }
        lowerTitle.contains("overtime") -> {
            EmptyStateDetails(
                Icons.Default.Schedule,
                MaterialTheme.colorScheme.secondary,
                stringResource(R.string.no_overtime_records_title),
                stringResource(R.string.no_overtime_records_desc)
            )
        }
        lowerTitle.contains("present") || lowerTitle.contains("absent") || lowerTitle.contains("half") || lowerTitle.contains("attendance") -> {
            EmptyStateDetails(
                Icons.Default.DateRange,
                MaterialTheme.colorScheme.tertiary,
                stringResource(R.string.no_attendance_records_title),
                stringResource(R.string.no_attendance_records_desc)
            )
        }
        lowerTitle.contains("date") -> {
            EmptyStateDetails(
                Icons.Default.Today,
                MaterialTheme.colorScheme.primary,
                stringResource(R.string.no_attendance_for_date_title),
                stringResource(R.string.no_attendance_for_date_desc)
            )
        }
        else -> { // earnings / default
            EmptyStateDetails(
                Icons.Default.History,
                MaterialTheme.colorScheme.primary,
                stringResource(R.string.no_records_yet_title),
                stringResource(R.string.no_records_yet_desc)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Illustration circular container
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title
        Text(
            text = displayTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer { this.alpha = titleAlpha }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = displayDesc,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .graphicsLayer { this.alpha = descAlpha }
        )
    }
}

private data class EmptyStateDetails(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val title: String,
    val description: String
)

@Composable
fun DrillDownRecordItem(log: OfflinePassbookLog, currencyCode: String = "INR") {
    val dateParts = log.date.split("-")
    val formattedDate = if (dateParts.size == 3) "${dateParts[2]} ${getMonthName(dateParts[1].toInt())} ${dateParts[0]}" else log.date

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {}
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formattedDate, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (!log.note.isNullOrBlank()) {
                ExpandableNoteText(
                    text = log.note,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (log.overtimeHours > 0) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_ot_drilldown, log.overtimeHours.toString(), AmountFormatter.formatWithSymbol(log.overtimeAmount, currencyCode)), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val amount = if (log.status == "advance" && log.advanceAmount != null) {
                "-${AmountFormatter.formatWithSymbol(log.advanceAmount, currencyCode)}"
            } else if (log.baseEarnings > 0 || log.overtimeAmount > 0) {
                "+${AmountFormatter.formatWithSymbol(log.baseEarnings + log.overtimeAmount, currencyCode)}"
            } else ""

            val color = if (log.status == "advance") com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent() else com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()

            Text(
                text = log.status.replaceFirstChar { it.uppercase() } + (if (log.type == "half") " (Half)" else ""),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            if (amount.isNotEmpty()) {
                Text(amount, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

private fun getMonthName(month: Int): String {
    return when(month) {
        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
        7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
        else -> ""
    }
}
