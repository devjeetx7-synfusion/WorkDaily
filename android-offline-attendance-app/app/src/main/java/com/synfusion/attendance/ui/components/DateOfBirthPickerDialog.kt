package com.synfusion.attendance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.utils.DateOfBirthUtils
import java.time.LocalDate

/**
 * Custom day / month / year date-of-birth picker.
 *
 * WorkDaily has no existing date-picker component and Compose exposes no date keyboard type, which
 * is why date of birth was previously a free-text "YYYY-MM-DD" field. Typing a raw ISO date is
 * error-prone and cannot prevent invalid calendar dates, so this dialog replaces that entry with
 * three scrollable columns that can only express real dates.
 *
 * Contract:
 *  - [initialDob] is the canonical "yyyy-MM-dd" string, or null/blank when nothing is saved yet.
 *    Reopening the dialog therefore restores the previously saved date rather than resetting it.
 *  - [onConfirm] receives the canonical "yyyy-MM-dd" string -- the same format the rest of the app
 *    already stores, so no migration or format change is needed.
 *  - Only the date is ever emitted. Age is derived on demand by [DateOfBirthUtils.ageLabel] and is
 *    never persisted, so it cannot go stale.
 *
 * Calendar correctness comes from [DateOfBirthUtils.daysInMonth], which delegates to
 * `java.time.YearMonth`, so 29 February is offered in leap years and withheld in common ones
 * rather than being handled by a hand-written month table.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOfBirthPickerDialog(
    initialDob: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Pinned once per dialog instance so the "no future dates" bound cannot drift mid-selection.
    val today = remember { LocalDate.now() }

    val years = remember { DateOfBirthUtils.selectableYears(today) }
    val restored = remember(initialDob) { DateOfBirthUtils.parse(initialDob) }

    var selectedYear by remember { mutableStateOf(restored?.year ?: DEFAULT_YEAR) }
    var selectedMonth by remember { mutableStateOf(restored?.monthValue ?: 1) }
    var selectedDay by remember { mutableStateOf(restored?.dayOfMonth ?: 1) }

    // Future dates are unreachable rather than merely rejected: the offered months and days are
    // bounded by today whenever the selection is in the current year or month.
    val maxMonth = if (selectedYear >= today.year) today.monthValue else 12
    val maxDay = run {
        val calendarMax = DateOfBirthUtils.daysInMonth(selectedYear, selectedMonth)
        if (selectedYear >= today.year && selectedMonth >= today.monthValue) {
            minOf(calendarMax, today.dayOfMonth)
        } else {
            calendarMax
        }
    }

    // Changing month or year can invalidate the current day (31 -> February, or a leap-year
    // rollover). Clamp instead of leaving an impossible date selected.
    LaunchedEffect(selectedYear, selectedMonth) {
        if (selectedMonth > maxMonth) selectedMonth = maxMonth
        if (selectedDay > maxDay) selectedDay = maxDay
    }

    val days = (1..maxDay.coerceAtLeast(1)).toList()
    val months = (1..maxMonth.coerceAtLeast(1)).toList()

    // Re-validated on every change so the Save button reflects the real state of the selection.
    val candidate = "%04d-%02d-%02d".format(selectedYear, selectedMonth, selectedDay)
    val validationError = when {
        DateOfBirthUtils.parse(candidate) == null -> "That date does not exist"
        !DateOfBirthUtils.isValid(candidate) -> "Date of birth cannot be in the future"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Date of Birth",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(216.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerColumn(
                        modifier = Modifier.weight(1f),
                        label = "Day",
                        values = days.map { it.toString() },
                        selected = selectedDay.toString(),
                        onSelect = { selectedDay = it.toIntOrNull() ?: selectedDay }
                    )
                    PickerColumn(
                        modifier = Modifier.weight(1.2f),
                        label = "Month",
                        values = months.map { monthLabel(it) },
                        selected = monthLabel(selectedMonth),
                        onSelect = { picked ->
                            months.firstOrNull { monthLabel(it) == picked }?.let { selectedMonth = it }
                        }
                    )
                    PickerColumn(
                        modifier = Modifier.weight(1.2f),
                        label = "Year",
                        values = years.map { it.toString() },
                        selected = selectedYear.toString(),
                        onSelect = { selectedYear = it.toIntOrNull() ?: selectedYear }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (validationError != null) {
                    Text(
                        text = validationError,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Live feedback that the selection is a real, acceptable date.
                    Text(
                        text = DateOfBirthUtils.ageLabel(candidate)?.let { "Age: $it" } ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(candidate) },
                enabled = validationError == null
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Medium)
            }
        }
    )
}

/**
 * One labelled, scrollable column of the picker.
 *
 * Selection is by tap rather than by scroll position. A scroll-driven wheel has to reconcile
 * fling settling with programmatic scrolling and is easy to leave displaying one value while
 * holding another; tap-to-select keeps the selected value unambiguous and is accessible without
 * extra work. The list still auto-scrolls to the current selection when it opens.
 */
@Composable
private fun PickerColumn(
    modifier: Modifier = Modifier,
    label: String,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedIndex = values.indexOf(selected)

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(values) { value ->
                val isSelected = value == selected
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(value) },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = value,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Short month label for the picker.
 *
 * Uses the platform's own month names so the dialog follows the app language, falling back to the
 * numeric month if a name cannot be resolved.
 */
private fun monthLabel(month: Int): String = try {
    java.time.Month.of(month)
        .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
        .replaceFirstChar { it.uppercase() }
} catch (_: Exception) {
    "%02d".format(month)
}

/**
 * Default year shown when no date of birth has been saved.
 *
 * Deliberately a neutral scroll position, not a stored or implied value: nothing is persisted
 * until the user presses Save, so opening the dialog never assigns a date of birth by itself.
 */
private val DEFAULT_YEAR: Int = LocalDate.now().year - 25
