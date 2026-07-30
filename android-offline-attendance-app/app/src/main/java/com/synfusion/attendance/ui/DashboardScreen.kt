package com.synfusion.attendance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.synfusion.attendance.utils.OvertimeCalculator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.viewmodel.compose.viewModel

import com.synfusion.attendance.viewmodel.OfflineCalendarViewModel
import com.synfusion.attendance.viewmodel.OfflineStatsViewModel
import com.synfusion.attendance.viewmodel.OfflinePassbookViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import com.synfusion.attendance.viewmodel.OfflineDashboardViewModel
import com.synfusion.attendance.viewmodel.OfflineDashboardState
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.IndianCurrencyVisualTransformation
import com.synfusion.attendance.viewmodel.OfflineViewModelFactory
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    factory: OfflineViewModelFactory,
    settingsViewModel: OfflineSettingsViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Lazy instantiate the ViewModels needed for DashboardScreen tabs
    val dashboardViewModel: OfflineDashboardViewModel = viewModel(factory = factory)
    val calendarViewModel: OfflineCalendarViewModel = viewModel(factory = factory)
    val statsViewModel: OfflineStatsViewModel = viewModel(factory = factory)

    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle(initialValue = OfflineDashboardState(isLoading = true))
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main_pager"
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var advanceAmount by remember { mutableStateOf("") }
    var showPassbookBottomSheet by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val dashboardScrollState = rememberLazyListState()
    val calendarScrollState = rememberLazyListState()
    val statsScrollState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()

    // Lazy tab-refresh based on active page selection
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> dashboardViewModel.refresh()
            1 -> calendarViewModel.refresh()
            2 -> statsViewModel.refresh()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (currentRoute == "main_pager") {
                BottomNavigationBar(
                    currentRoute = when (selectedTab) {
                        0 -> "dashboard"
                        1 -> "calendar"
                        2 -> "stats"
                        3 -> "settings"
                        else -> "dashboard"
                    },
                    onNavigate = { route ->
                        selectedTab = when (route) {
                            "dashboard" -> 0
                            "calendar" -> 1
                            "stats" -> 2
                            "settings" -> 3
                            else -> 0
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "main_pager",
            modifier = Modifier.fillMaxSize().padding(padding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable("main_pager") {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it / 10 }) + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { -it / 10 }) + fadeOut(animationSpec = tween(220)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(220), initialOffsetX = { -it / 10 }) + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 10 }) + fadeOut(animationSpec = tween(220)))
                        }
                    },
                    label = "tabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            if (dashboardState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (dashboardState.errorMessage != null) {
                                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                                        Text(dashboardState.errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                        Button(onClick = { dashboardViewModel.refresh() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            } else {
                                val pullRefreshState = rememberPullToRefreshState()
                                LaunchedEffect(pullRefreshState.isRefreshing) {
                                    if (pullRefreshState.isRefreshing) {
                                        dashboardViewModel.refresh()
                                    }
                                }
                                LaunchedEffect(dashboardState.isRefreshing) {
                                    if (dashboardState.isRefreshing) {
                                        pullRefreshState.startRefresh()
                                    } else {
                                        pullRefreshState.endRefresh()
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(pullRefreshState.nestedScrollConnection)
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    LazyColumn(
                                        state = dashboardScrollState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item {
                                            HeaderSection(
                                                state = dashboardState
                                            )
                                        }

                                        item { PersonalStatsGrid(dashboardState, onNavigatePassbook = { showPassbookBottomSheet = true }) }
                                        item {
                                            PersonalQuickActions(
                                                onAddAdvance = { showAdvanceDialog = true }
                                            )
                                        }
                                        item { PersonalDailyLog(dashboardState, dashboardViewModel) }

                                        item { Spacer(modifier = Modifier.height(24.dp)) }
                                    }

                                    PullToRefreshContainer(
                                        state = pullRefreshState,
                                        modifier = Modifier.align(Alignment.TopCenter),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        1 -> {
                            CalendarScreen(viewModel = calendarViewModel, scrollState = calendarScrollState)
                        }
                        2 -> {
                            StatsScreen(
                                viewModel = statsViewModel,
                                onNavigatePassbook = {
                                    showPassbookBottomSheet = true
                                },
                                scrollState = statsScrollState
                            )
                        }
                        3 -> {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onLogout = onLogout,
                                scrollState = settingsScrollState
                            )
                        }
                    }
                }
            }
            composable("passbook") {
                val passbookViewModel: OfflinePassbookViewModel = viewModel(factory = factory)
                PassbookScreen(
                    viewModel = passbookViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateToCalendar = {
                        bottomNavController.navigateUp()
                        selectedTab = 1
                    }
                )
            }
        }

        if (showAdvanceDialog) {
            AlertDialog(
                onDismissRequest = { showAdvanceDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.add_advance_payment), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = advanceAmount,
                            onValueChange = { advanceAmount = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.amount)) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("₹", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = IndianCurrencyVisualTransformation()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = advanceAmount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                dashboardViewModel.addAdvance(amount)
                            }
                            showAdvanceDialog = false
                            advanceAmount = ""
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdvanceDialog = false }) { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel)) }
                }
            )
        }

        if (showPassbookBottomSheet) {
            val passbookViewModel: OfflinePassbookViewModel = viewModel(factory = factory)
            ModalBottomSheet(
                onDismissRequest = { showPassbookBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.background,
                dragHandle = null
            ) {
                PassbookScreen(
                    viewModel = passbookViewModel,
                    onNavigateBack = { showPassbookBottomSheet = false },
                    onNavigateToCalendar = {
                        showPassbookBottomSheet = false
                        selectedTab = 1
                    }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(state: OfflineDashboardState) {
    val todayFullDate = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val displayName = state.name.trim()
    val firstName = if (displayName.isNotEmpty()) displayName.split(" ")[0] else "User"
    val avatarLetters = if (displayName.length >= 2) displayName.take(2).uppercase() else if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "U"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarLetters,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hi, ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$firstName 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = todayFullDate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PersonalStatsGrid(state: OfflineDashboardState, onNavigatePassbook: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.todays_earnings), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${AmountFormatter.format(state.todayEarned)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.monthly_earnings), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${AmountFormatter.format(state.monthEarned)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.view_my_passbook), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PersonalQuickActions(onAddAdvance: () -> Unit) {
    Column {
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.quick_actions), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .clickable { onAddAdvance() }
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF97316).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Add Advance Payment", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Record money received", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PersonalDailyLog(state: OfflineDashboardState, dashboardViewModel: OfflineDashboardViewModel) {
    var overtimeHours by remember { mutableStateOf(state.overtimeHours) }
    var otAmount by remember {
        mutableStateOf(state.todayNote?.let { OvertimeCalculator.extractCustomAmount(it)?.toInt()?.toString() } ?: "")
    }
    var note by remember {
        mutableStateOf(state.todayNote?.let { OvertimeCalculator.cleanNote(it) } ?: "")
    }
    var showAbsentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.overtimeHours, state.todayNote) {
        overtimeHours = state.overtimeHours
        val rawNote = state.todayNote ?: ""
        otAmount = OvertimeCalculator.extractCustomAmount(rawNote)?.toInt()?.toString() ?: ""
        note = OvertimeCalculator.cleanNote(rawNote) ?: ""
    }

    Column {
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_log), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

        if (state.todayStatus == null) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime_hours), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).clickable { if (overtimeHours > 0) overtimeHours-- }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    Text(overtimeHours.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable { overtimeHours++ }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = otAmount,
                onValueChange = { otAmount = it.filter { ch -> ch.isDigit() } },
                placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.ot_amount)) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                visualTransformation = IndianCurrencyVisualTransformation()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.add_note_optional)) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val finalNote = if (otAmount.isNotBlank()) {
                        val otStr = "[OT_WAGE_${otAmount.trim()}]"
                        if (note.isNotBlank()) "$otStr $note" else otStr
                    } else {
                        note
                    }
                    dashboardViewModel.markAttendance("full", overtimeHours, finalNote)
                }, modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_day), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                OutlinedButton(onClick = {
                    val finalNote = if (otAmount.isNotBlank()) {
                        val otStr = "[OT_WAGE_${otAmount.trim()}]"
                        if (note.isNotBlank()) "$otStr $note" else otStr
                    } else {
                        note
                    }
                    dashboardViewModel.markAttendance("half", overtimeHours, finalNote)
                }, modifier = Modifier.weight(1f).height(100.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground, containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.half_day), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { showAbsentDialog = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error, containerColor = MaterialTheme.colorScheme.background), border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.mark_absent), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            val isPresent = state.todayStatus == "present"
            val bgColor = if (isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bgColor.copy(alpha = 0.1f)).border(2.dp, bgColor, RoundedCornerShape(16.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                        if (isPresent) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        } else {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(if (isPresent) "Marked Present" else "Marked Absent", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = bgColor)
                    if (note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { dashboardViewModel.removeAttendance() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error, containerColor = MaterialTheme.colorScheme.background), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.remove_attendance), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        if (showAbsentDialog) {
            AlertDialog(
                onDismissRequest = { showAbsentDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.mark_absent), fontWeight = FontWeight.Bold) },
                text = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.are_you_sure_you_want_to_mark_yourself_a_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            dashboardViewModel.markAbsent("personal", note)
                            showAbsentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAbsentDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(currentRoute: String, onNavigate: (String) -> Unit = {}) {
    data class NavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

    val tabs = listOf(
        NavItem("dashboard", Icons.Default.Home, "Home"),
        NavItem("calendar", Icons.Default.CalendarMonth, "Calendar"),
        NavItem("stats", Icons.Default.BarChart, "Status"),
        NavItem("settings", Icons.Default.Settings, "Setting")
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == currentRoute
            NavigationBarItem(
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        tab.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                selected = selected,
                onClick = { onNavigate(tab.route) },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = Color.White,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
