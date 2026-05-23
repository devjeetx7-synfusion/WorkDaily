package com.dailywork.attedance.ui

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dailywork.attedance.utils.NetworkMonitor
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.ModalBottomSheet

import com.dailywork.attedance.viewmodel.DashboardViewModel
import com.dailywork.attedance.viewmodel.DashboardState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.dailywork.attedance.utils.OvertimeCalculator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavController

import com.dailywork.attedance.viewmodel.CalendarViewModel
import com.dailywork.attedance.viewmodel.StatsViewModel
import com.dailywork.attedance.viewmodel.PassbookViewModel
import com.dailywork.attedance.viewmodel.SettingsViewModel
import com.dailywork.attedance.viewmodel.WorkersViewModel
import com.dailywork.attedance.viewmodel.WorkerDetailViewModel
import com.dailywork.attedance.viewmodel.WorkerHistoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel,
    calendarViewModel: CalendarViewModel,
    statsViewModel: StatsViewModel,
    passbookViewModel: PassbookViewModel,
    settingsViewModel: SettingsViewModel,
    workersViewModel: WorkersViewModel,
    workerDetailViewModel: WorkerDetailViewModel,
    workerHistoryViewModel: WorkerHistoryViewModel,
    onLogout: () -> Unit
) {
        val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    val isConnected by networkMonitor.isConnected.collectAsState(initial = networkMonitor.checkCurrentState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isConnected) {
        dashboardViewModel.onNetworkStateChanged(isConnected)
    }

val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val calendarState by calendarViewModel.calendarState.collectAsState()
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main_pager"
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var advanceAmount by remember { mutableStateOf("") }
    var selectedWorkerId by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    // Auto-refresh on resume/open
    LaunchedEffect(Unit) {
        dashboardViewModel.refresh()
        calendarViewModel.refresh()
        statsViewModel.refresh()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (currentRoute == "main_pager") {
                BottomNavigationBar(
                    currentRoute = when (pagerState.currentPage) {
                        0 -> "dashboard"
                        1 -> "calendar"
                        2 -> "stats"
                        3 -> "settings"
                        else -> "dashboard"
                    },
                    onNavigate = { route ->
                        val page = when (route) {
                            "dashboard" -> 0
                            "calendar" -> 1
                            "stats" -> 2
                            "settings" -> 3
                            else -> 0
                        }
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(navController = bottomNavController, startDestination = "main_pager", modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("main_pager") {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> {
                            if (dashboardState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                val pullRefreshState = rememberPullToRefreshState()
                                if (pullRefreshState.isRefreshing) {
                                    LaunchedEffect(true) {
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item {
                                        HeaderSection(
                                            state = dashboardState,
                                            onNavigateToPremium = { navController.navigate("premium") }
                                        )
                                    }

                                        if (dashboardState.role == "contractor") {
                                            item {
                                                ContractorStatsGrid(
                                                    state = dashboardState
                                                )
                                            }
                                            item {
                                                ContractorQuickActions(
                                                    onManageWorkers = { bottomNavController.navigate("workers") },
                                                    onMarkAttendance = {
                                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                                    },
                                                    onAddAdvance = { showAdvanceDialog = true }
                                                )
                                            }
                                        } else {
                                            item { PersonalStatsGrid(dashboardState, onNavigatePassbook = { bottomNavController.navigate("passbook") }) }
                                            item {
                                                PersonalQuickActions(
                                                    onAddAdvance = { showAdvanceDialog = true }
                                                )
                                            }
                                            item { PersonalDailyLog(dashboardState, dashboardViewModel) }
                                        }

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
                            CalendarScreen(viewModel = calendarViewModel)
                        }
                        2 -> {
                            StatsScreenContent(
                                viewModel = statsViewModel,
                                onNavigateToPremium = { navController.navigate("premium") },
                                onNavigateToWorkerHistory = {
                                    coroutineScope.launch {
                                        bottomNavController.navigate("worker_history")
                                    }
                                }
                            )
                        }
                        3 -> {
                            SettingsScreenContent(
                                viewModel = settingsViewModel,
                                onLogout = onLogout,
                                onNavigateToPremium = { navController.navigate("premium") },
                                onNavigateToWorkerHistory = {
                                    bottomNavController.navigate("worker_history")
                                }
                            )
                        }
                    }
                }
            }
            composable("passbook") {
                PassbookScreenContent(
                    viewModel = passbookViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateToCalendar = {
                        bottomNavController.navigateUp()
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onNavigateToPremium = { navController.navigate("premium") }
                )
            }
            composable("workers") {
                WorkersScreenContent(
                    viewModel = workersViewModel,
                    onNavigateToWorkerDetail = { bottomNavController.navigate("worker_detail/$it") },
                    onNavigateToPremium = { navController.navigate("premium") }
                )
            }
            composable("worker_detail/{workerId}") { backStackEntry ->
                val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
                WorkerDetailScreenContent(
                    workerId = workerId,
                    viewModel = workerDetailViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateToCalendar = {
                        bottomNavController.navigate("worker_calendar/$workerId")
                    },
                    onNavigateToPremium = { navController.navigate("premium") }
                )
            }
            composable("worker_calendar/{workerId}") { backStackEntry ->
                WorkerCalendarScreen(
                    workerId = backStackEntry.arguments?.getString("workerId") ?: "",
                    viewModel = workerDetailViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("worker_history") {
                WorkerHistoryScreen(
                    viewModel = workerHistoryViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateToPremium = { navController.navigate("premium") }
                )
            }
        }

        if (showAdvanceDialog) {
            AlertDialog(
                onDismissRequest = { showAdvanceDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.add_advance_payment), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (dashboardState.role == "contractor") {
                            var expandedWorkerMenu by remember { mutableStateOf(false) }
                            val selectedWorkerName = calendarState.workers.find { it.id == selectedWorkerId }?.name ?: "Select Worker"

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedWorkerMenu = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(selectedWorkerName, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedWorkerMenu,
                                    onDismissRequest = { expandedWorkerMenu = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    calendarState.workers.forEach { worker ->
                                        DropdownMenuItem(
                                            text = { Text(worker.name, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                selectedWorkerId = worker.id
                                                expandedWorkerMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = advanceAmount,
                            onValueChange = { advanceAmount = it },
                            placeholder = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.amount)) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("₹", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = advanceAmount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                if (dashboardState.role == "personal") {
                                    dashboardViewModel.addAdvance(amount)
                                } else if (selectedWorkerId != null) {
                                    dashboardViewModel.addAdvance(amount, selectedWorkerId)
                                }
                            }
                            showAdvanceDialog = false
                            advanceAmount = ""
                            selectedWorkerId = null
                        },
                        enabled = dashboardState.role == "personal" || selectedWorkerId != null
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdvanceDialog = false }) { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cancel)) }
                }
            )
        }
    }

    if (dashboardState.showOfflineBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { dashboardViewModel.dismissOfflineBottomSheet() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.no_internet_connection),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.youre_currently_offline_you_can_still_us_msg),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val currentNetworkState = networkMonitor.checkCurrentState()
                        dashboardViewModel.retryConnection(currentNetworkState)
                        if (!currentNetworkState) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Still no connection")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.try_again), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { dashboardViewModel.dismissOfflineBottomSheet() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.continue_offline),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

}

@Composable
fun HeaderSection(state: DashboardState, onNavigateToPremium: () -> Unit) {
    val todayFullDate = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

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
                    text = state.name.take(2).uppercase(),
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
                        text = "${state.name.split(" ")[0]} 👋",
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

        // Premium Crown Button
        if (!state.isPremium) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)) // amber-100
                    .border(1.dp, Color(0xFFFDE68A), CircleShape) // amber-200
                    .clickable { onNavigateToPremium() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = Color(0xFFD97706)) // amber-600
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium Active", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ContractorStatsGrid(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top row: 2 cards side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("Total Workers", state.totalWorkers, Icons.Default.People, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Today Present", state.todayPresent, Icons.Default.CheckCircle, Color(0xFF10B981), Modifier.weight(1f))
        }

        // Bottom row: 3 cards side by side (smaller width)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCardSmall("Today Absent", state.todayAbsent, Icons.Default.Cancel, Color(0xFFEF4444), Modifier.weight(1f))
            StatCardSmall("Total Paid", "₹${state.totalPaidMonth}", Icons.Default.AccountBalanceWallet, Color(0xFF3B82F6), Modifier.weight(1f))
            StatCardSmall("Pending", "₹${state.pendingAmount}", Icons.Default.AccountBalanceWallet, Color(0xFFF97316), Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(120.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCardSmall(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = modifier
            .height(100.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun ContractorQuickActions(onManageWorkers: () -> Unit, onMarkAttendance: () -> Unit, onAddAdvance: () -> Unit) {
    Column {
        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.quick_actions), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

        QuickActionItem("Manage Workers", "Add, edit or remove workers", Icons.Default.People, MaterialTheme.colorScheme.primary, onClick = onManageWorkers)
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionItem("Mark Attendance", "Daily attendance for all workers", Icons.Default.CheckCircle, Color(0xFF10B981), onClick = onMarkAttendance)
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionItem("Add Advance Payment", "Record payments for workers", Icons.Default.Add, Color(0xFFF97316), onClick = onAddAdvance)
    }
}

@Composable
fun QuickActionItem(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PersonalStatsGrid(state: DashboardState, onNavigatePassbook: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.todays_earnings), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${state.todayEarned}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.monthly_earnings), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${state.monthEarned}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).clickable(onClick = onNavigatePassbook).padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.view_my_passbook), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PersonalQuickActions(onAddAdvance: () -> Unit) {
    Column {
        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.quick_actions), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
            QuickActionItem("Add Advance Payment", "Record money received", Icons.Default.Add, Color(0xFFF97316), onClick = onAddAdvance)
        }
    }
}

@Composable
fun PersonalDailyLog(state: DashboardState, dashboardViewModel: DashboardViewModel) {
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
        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.daily_log), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

        if (state.todayStatus == null) {
            // Overtime Input
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.overtime_hours), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).clickable { if (overtimeHours > 0) overtimeHours-- }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    Text(overtimeHours.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable { overtimeHours++ }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = otAmount,
                onValueChange = { otAmount = it },
                placeholder = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.ot_amount)) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.add_note_optional)) },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
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
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.full_day), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.half_day), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { showAbsentDialog = true }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error, containerColor = MaterialTheme.colorScheme.background), border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.mark_absent), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.remove_attendance), fontWeight = FontWeight.Bold, fontSize = 14.sp)
             }
        }

        if (showAbsentDialog) {
            AlertDialog(
                onDismissRequest = { showAbsentDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.mark_absent), fontWeight = FontWeight.Bold) },
                text = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.are_you_sure_you_want_to_mark_yourself_a_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            dashboardViewModel.markAbsent("personal", note)
                            showAbsentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAbsentDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cancel))
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
