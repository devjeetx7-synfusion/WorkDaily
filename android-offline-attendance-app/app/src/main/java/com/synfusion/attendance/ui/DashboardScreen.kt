package com.synfusion.attendance.ui

import com.synfusion.attendance.ui.screens.HowToUseScreen
import com.synfusion.attendance.ui.screens.PrivacyPolicyScreen
import com.synfusion.attendance.ui.screens.TermsConditionsScreen

import androidx.compose.foundation.background
import com.synfusion.attendance.ui.components.ShimmerBox
import com.synfusion.attendance.ui.components.ShimmerCircle
import com.synfusion.attendance.ui.components.ShimmerTextLine
import com.synfusion.attendance.ui.components.ExpandableNoteText
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.synfusion.attendance.ui.components.WorkDailyPullToRefreshBox
import com.synfusion.attendance.ui.components.WavingHandIcon
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.viewmodel.compose.viewModel

import com.synfusion.attendance.viewmodel.OfflineCalendarViewModel
import com.synfusion.attendance.viewmodel.OfflineStatsViewModel
import com.synfusion.attendance.viewmodel.OfflinePassbookViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.viewmodel.OfflineDashboardViewModel
import com.synfusion.attendance.viewmodel.OfflineDashboardState
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModelFactory
import com.synfusion.attendance.rewards.ui.screens.*
import com.synfusion.attendance.rewards.ui.components.RewardsPurple
import com.synfusion.attendance.rewards.ui.components.RewardCoinBalance

import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.live.viewmodel.LiveViewModelFactory
import com.synfusion.attendance.live.ui.*
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.data.DrillDownType
import com.synfusion.attendance.ui.components.DrillDownBottomSheet
import com.synfusion.attendance.ui.components.FloatingBottomNavigation
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.CurrencyVisualTransformation
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.viewmodel.OfflineViewModelFactory
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.ui.pdf.PdfViewerScreen
import com.synfusion.attendance.notifications.ui.*
import com.synfusion.attendance.notifications.studio.*
import android.net.Uri
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    factory: OfflineViewModelFactory,
    settingsViewModel: OfflineSettingsViewModel,
    externalPdfUri: Uri? = null,
    onClearExternalPdfUri: () -> Unit = {},
    onLogout: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Lazy instantiate the ViewModels needed for DashboardScreen tabs
    val context = LocalContext.current
    val dashboardViewModel: OfflineDashboardViewModel = viewModel(factory = factory)
    val calendarViewModel: OfflineCalendarViewModel = viewModel(factory = factory)
    val statsViewModel: OfflineStatsViewModel = viewModel(factory = factory)
    val rewardsViewModel: RewardsViewModel = viewModel(factory = RewardsViewModelFactory(context.applicationContext as android.app.Application))
    val liveViewModel: LiveViewModel = viewModel(factory = LiveViewModelFactory(context.applicationContext as android.app.Application))

    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle(initialValue = OfflineDashboardState(isLoading = true))
    val walletSummary by rewardsViewModel.walletSummary.collectAsState()
    val liveRooms by liveViewModel.liveRooms.collectAsState()
    val liveCreators by liveViewModel.creators.collectAsState()
    val followedCreatorIds by liveViewModel.followedCreatorIds.collectAsState()
    val liveWallet by liveViewModel.wallet.collectAsState()
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main_pager"
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var advanceAmount by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf(0) }

    val dashboardScrollState = rememberLazyListState()
    val calendarScrollState = rememberLazyListState()
    val statsScrollState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()

    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    var initialRouteHandled by remember { mutableStateOf(false) }

    val windowSizeInfo = LocalWindowSizeInfo.current
    val isCompactPortrait = windowSizeInfo.isCompact && !windowSizeInfo.isLandscape

    val currentNavRoute = if (currentRoute == "all_time_passbook") {
        "all_time_passbook"
    } else if (currentRoute == "passbook") {
        "passbook"
    } else {
        when (selectedTab) {
            0 -> "dashboard"
            1 -> "calendar"
            2 -> "stats"
            3 -> "live_discover"
            else -> "dashboard"
        }
    }

    LaunchedEffect(externalPdfUri) {
        if (externalPdfUri != null) {
            val encodedUri = URLEncoder.encode(externalPdfUri.toString(), "UTF-8")
            val encodedTitle = URLEncoder.encode("", "UTF-8")
            bottomNavController.navigate("pdf_viewer?uri=$encodedUri&title=$encodedTitle") {
                launchSingleTop = true
            }
            onClearExternalPdfUri()
        }
    }

    LaunchedEffect(settingsState.isLoaded) {
        if (settingsState.isLoaded && !initialRouteHandled) {
            val initialRoute = if (settingsState.rememberLastScreen) {
                settingsState.lastActiveScreen
            } else {
                settingsState.startupScreen
            }
            when (initialRoute) {
                "dashboard" -> selectedTab = 0
                "calendar" -> selectedTab = 1
                "stats" -> selectedTab = 2
                "live_discover", "live" -> selectedTab = 3
                "settings" -> {
                    selectedTab = 0
                }
                "passbook" -> {
                    bottomNavController.navigate("passbook")
                }
            }
            initialRouteHandled = true
        }
    }

    // Lazy tab-refresh based on active page selection
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> dashboardViewModel.refresh()
            1 -> calendarViewModel.refresh()
            2 -> statsViewModel.refresh()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val onNavSelected: (String) -> Unit = { route ->
        settingsViewModel.updateLastActiveScreen(route)
        when (route) {
            "dashboard" -> {
                if (currentRoute != "main_pager") {
                    bottomNavController.popBackStack("main_pager", false)
                } else if (selectedTab == 0) {
                    coroutineScope.launch {
                        dashboardScrollState.animateScrollToItem(0)
                    }
                }
                selectedTab = 0
            }
            "calendar" -> {
                if (currentRoute != "main_pager") {
                    bottomNavController.popBackStack("main_pager", false)
                } else if (selectedTab == 1) {
                    coroutineScope.launch {
                        calendarScrollState.animateScrollToItem(0)
                    }
                }
                selectedTab = 1
            }
            "stats" -> {
                if (currentRoute != "main_pager") {
                    bottomNavController.popBackStack("main_pager", false)
                } else if (selectedTab == 2) {
                    coroutineScope.launch {
                        statsScrollState.animateScrollToItem(0)
                    }
                }
                selectedTab = 2
            }
            "live_discover" -> {
                if (currentRoute != "main_pager") {
                    bottomNavController.popBackStack("main_pager", false)
                }
                selectedTab = 3
            }
            "settings" -> {
                if (currentRoute != "settings") {
                    bottomNavController.navigate("settings") {
                        launchSingleTop = true
                    }
                }
            }
            "passbook" -> {
                if (currentRoute != "all_time_passbook") {
                    if (currentRoute != "main_pager") {
                        bottomNavController.popBackStack("main_pager", false)
                    }
                    bottomNavController.navigate("all_time_passbook") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            "all_time_passbook" -> {
                if (currentRoute != "all_time_passbook") {
                    if (currentRoute != "main_pager") {
                        bottomNavController.popBackStack("main_pager", false)
                    }
                    bottomNavController.navigate("all_time_passbook") {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            if (currentRoute == "main_pager" && !isCompactPortrait) {
                AdaptiveNavigationRail(
                    currentRoute = currentNavRoute,
                    onNavigate = onNavSelected
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = bottomNavController,
                    startDestination = "main_pager",
                    modifier = Modifier.fillMaxSize(),
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
                            WorkDailyPullToRefreshBox(
                                isRefreshing = dashboardState.isRefreshing,
                                onRefresh = { dashboardViewModel.refresh() },
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                            ) {
                                Crossfade(
                                    targetState = if (dashboardState.isLoading) "shimmer" else if (dashboardState.errorMessage != null) "error" else "content",
                                    animationSpec = tween(350),
                                    label = "dashboard_crossfade"
                                ) { state ->
                                    when (state) {
                                        "shimmer" -> {
                                            DashboardShimmer()
                                        }
                                        "error" -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                                                    Text(dashboardState.errorMessage ?: "An error occurred", color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                    Button(onClick = { dashboardViewModel.refresh() }) {
                                                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.try_again))
                                                    }
                                                }
                                            }
                                        }
                                        "content" -> {
                                            ResponsiveContainer {
                                                if (isCompactPortrait) {
                                                    LazyColumn(
                                                        state = dashboardScrollState,
                                                        modifier = Modifier
                                                            .fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 16.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, end = 16.dp, bottom = 96.dp),
                                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        item {
                                                            HeaderSection(
                                                                state = dashboardState,
                                                                googlePhotoUrl = settingsState.googlePhotoUrl,
                                                                onProfileClick = { bottomNavController.navigate("profile") },
                                                                onSearchClick = { bottomNavController.navigate("live_people") },
                                                                onMessagesClick = { bottomNavController.navigate("messages") },
                                                                onNotificationClick = { bottomNavController.navigate("notifications_center") }
                                                            )
                                                        }
                                                        item {
                                                            Column(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                                            ) {
                                                                HomeFestivalBanner(state = dashboardState)
                                                                HomeFollowingLiveSection(
                                                                    liveRooms = liveRooms,
                                                                    creators = liveCreators,
                                                                    followedCreatorIds = followedCreatorIds,
                                                                    onNavigateLiveDiscover = { bottomNavController.navigate("live_discover") },
                                                                    onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") },
                                                                    onCreatorLongPress = { creator -> bottomNavController.navigate("live_profile?userId=${creator.id}") }
                                                                )
                                                                PersonalStatsGrid(currencyCode = settingsState.currencyCode,
                                                                    state = dashboardState,
                                                                    onNavigatePassbook = { bottomNavController.navigate("passbook") },
                                                                    onEarningsClick = { dashboardViewModel.openDrillDown(DrillDownType.EARNINGS) }
                                                                )
                                                                AllTimePassbookButton(onNavigate = { bottomNavController.navigate("all_time_passbook") })
                                                                SmartDailyRewardsWidget(
                                                                    rewardsViewModel = rewardsViewModel,
                                                                    onNavigateHub = { bottomNavController.navigate("rewards_hub") },
                                                                    onNavigateDailyCheckIn = { bottomNavController.navigate("rewards_daily_checkin") },
                                                                    onNavigateSpin = { bottomNavController.navigate("rewards_spin") },
                                                                    onNavigateAchievements = { bottomNavController.navigate("rewards_achievements") }
                                                                )
                                                                PersonalQuickActions(currencyCode = settingsState.currencyCode,
                                                                    todayAdvanceAmount = dashboardState.todayAdvanceAmount,
                                                                    onAddAdvance = { showAdvanceDialog = true }
                                                                )
                                                                PersonalDailyLog(dashboardState, settingsState.currencyCode, dashboardViewModel)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    LazyColumn(
                                                        state = dashboardScrollState,
                                                        modifier = Modifier
                                                            .fillMaxSize(),
                                                        contentPadding = PaddingValues(start = 20.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp, end = 20.dp, bottom = 24.dp),
                                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                                    ) {
                                                        item {
                                                            HeaderSection(
                                                                state = dashboardState,
                                                                googlePhotoUrl = settingsState.googlePhotoUrl,
                                                                onProfileClick = { bottomNavController.navigate("profile") },
                                                                onSearchClick = { bottomNavController.navigate("live_people") },
                                                                onNotificationClick = { bottomNavController.navigate("notifications_center") },
                                                                onMessagesClick = { bottomNavController.navigate("messages") }
                                                            )
                                                        }
                                                        item {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                                                verticalAlignment = Alignment.Top
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                                ) {
                                                                    HomeFestivalBanner(state = dashboardState)
                                                                    HomeFollowingLiveSection(
                                                                        liveRooms = liveRooms,
                                                                        creators = liveCreators,
                                                                        followedCreatorIds = followedCreatorIds,
                                                                        onNavigateLiveDiscover = { bottomNavController.navigate("live_discover") },
                                                                        onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") },
                                                                        onCreatorLongPress = { creator -> bottomNavController.navigate("live_profile?userId=${creator.id}") }
                                                                    )
                                                                    PersonalStatsGrid(currencyCode = settingsState.currencyCode,
                                                                        state = dashboardState,
                                                                        onNavigatePassbook = { bottomNavController.navigate("passbook") },
                                                                        onEarningsClick = { dashboardViewModel.openDrillDown(DrillDownType.EARNINGS) }
                                                                    )
                                                                    AllTimePassbookButton(onNavigate = { bottomNavController.navigate("all_time_passbook") })
                                                                    SmartDailyRewardsWidget(
                                                                        rewardsViewModel = rewardsViewModel,
                                                                        onNavigateHub = { bottomNavController.navigate("rewards_hub") },
                                                                        onNavigateDailyCheckIn = { bottomNavController.navigate("rewards_daily_checkin") },
                                                                        onNavigateSpin = { bottomNavController.navigate("rewards_spin") },
                                                                        onNavigateAchievements = { bottomNavController.navigate("rewards_achievements") }
                                                                    )
                                                                    PersonalQuickActions(currencyCode = settingsState.currencyCode,
                                                                        todayAdvanceAmount = dashboardState.todayAdvanceAmount,
                                                                        onAddAdvance = { showAdvanceDialog = true }
                                                                    )
                                                                }
                                                                Column(
                                                                    modifier = Modifier.weight(1f),
                                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                                ) {
                                                                    PersonalDailyLog(dashboardState, settingsState.currencyCode, dashboardViewModel)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            CalendarScreen(viewModel = calendarViewModel, scrollState = calendarScrollState)
                        }
                        2 -> {
                            StatsScreen(
                                viewModel = statsViewModel,
                                currencyCode = settingsState.currencyCode,
                                onNavigatePassbook = {
                                    bottomNavController.navigate("passbook")
                                },
                                scrollState = statsScrollState
                            )
                        }
                        3 -> {
                            LiveDiscoverScreen(
                                viewModel = liveViewModel,
                                onNavigateBack = { selectedTab = 0 },
                                onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") },
                                onNavigateWallet = { bottomNavController.navigate("live_wallet") },
                                onNavigateStudio = { bottomNavController.navigate("live_setup") },
                                onNavigateLeaderboard = { bottomNavController.navigate("live_leaderboard") },
                                onNavigateProfile = { bottomNavController.navigate("profile") },
                                onNavigatePeople = { bottomNavController.navigate("live_people") },
                                onNavigateMessages = { bottomNavController.navigate("live_private_chat/creator_1") },
                                onNavigateDemoControls = { bottomNavController.navigate("live_demo_controls") }
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
                    },
                    onOpenPdfViewer = { uri, title, exportType, monthDate ->
                        val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        bottomNavController.navigate("pdf_viewer?uri=$encodedUri&title=$encodedTitle&exportType=$exportType&monthDate=$monthDate")
                    }
                )
            }
            composable("all_time_passbook") {
                val allTimeViewModel: com.synfusion.attendance.viewmodel.OfflineAllTimePassbookViewModel = viewModel(factory = factory)
                AllTimePassbookScreen(
                    viewModel = allTimeViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onOpenPdfViewer = { uri, title, exportType, monthDate ->
                        val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                        val encodedTitle = URLEncoder.encode(title, "UTF-8")
                        bottomNavController.navigate("pdf_viewer?uri=$encodedUri&title=$encodedTitle&exportType=$exportType&monthDate=$monthDate")
                    }
                )
            }
            composable(
                route = "pdf_viewer?uri={uri}&title={title}&exportType={exportType}&monthDate={monthDate}",
                arguments = listOf(
                    navArgument("uri") { defaultValue = "" },
                    navArgument("title") { defaultValue = "" },
                    navArgument("exportType") { defaultValue = "" },
                    navArgument("monthDate") { defaultValue = "0" }
                )
            ) { backStackEntry ->
                val encodedUri = backStackEntry.arguments?.getString("uri") ?: ""
                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                val exportType = backStackEntry.arguments?.getString("exportType") ?: ""
                val monthDateStr = backStackEntry.arguments?.getString("monthDate") ?: "0"
                val monthDate = monthDateStr.toLongOrNull() ?: 0L

                val decodedUriStr = if (encodedUri.isNotEmpty()) URLDecoder.decode(encodedUri, "UTF-8") else ""
                val decodedTitle = if (encodedTitle.isNotEmpty()) URLDecoder.decode(encodedTitle, "UTF-8") else ""
                val pdfUri = if (decodedUriStr.isNotEmpty()) Uri.parse(decodedUriStr) else Uri.EMPTY

                PdfViewerScreen(
                    pdfUri = pdfUri,
                    title = decodedTitle,
                    exportType = exportType,
                    monthDate = monthDate,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("notifications_center") {
                val notifViewModel: NotificationCenterViewModel = viewModel()
                NotificationCenterScreen(
                    viewModel = notifViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateSettings = { bottomNavController.navigate("notification_settings") },
                    onNavigateDestination = { route -> bottomNavController.navigate(route) }
                )
            }
            composable("messages") {
                ChatListScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigatePrivateChat = { friendId -> bottomNavController.navigate("live_private_chat/$friendId") },
                    onNavigateUserProfile = { userId -> bottomNavController.navigate("live_profile?userId=$userId") }
                )
            }
            composable("notification_settings") {
                NotificationSettingsScreen(
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateStudio = { bottomNavController.navigate("notification_studio") }
                )
            }
            composable("notification_studio") {
                val studioViewModel: NotificationStudioViewModel = viewModel()
                NotificationStudioScreen(
                    viewModel = studioViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("profile") {
                ProfileScreen(
                    viewModel = settingsViewModel,
                    rewardsViewModel = rewardsViewModel,
                    liveViewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateSettings = { bottomNavController.navigate("settings") },
                    onNavigateWallet = { bottomNavController.navigate("rewards_wallet") },
                    onNavigateRewardsHub = { bottomNavController.navigate("rewards_hub") },
                    onNavigateCreatorStudio = { bottomNavController.navigate("live_studio") },
                    onNavigateCreatorEarnings = { bottomNavController.navigate("live_earnings") },
                    onNavigateGoLive = { bottomNavController.navigate("live_setup") },
                    onNavigateLeaderboard = { bottomNavController.navigate("rewards_leaderboard") },
                    onNavigateMessages = { bottomNavController.navigate("live_people") },
                    onNavigatePeople = { bottomNavController.navigate("live_people") },
                    onNavigateUserProfile = { targetUid -> bottomNavController.navigate("live_profile?userId=$targetUid") },
                    onLogout = onLogout
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLogout = onLogout,
                    onNavigate = { route: String -> bottomNavController.navigate(route) },
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("how_to_use") {
                HowToUseScreen(onNavigateBack = { bottomNavController.navigateUp() })
            }
            composable("privacy_policy") {
                PrivacyPolicyScreen(onNavigateBack = { bottomNavController.navigateUp() })
            }
            composable("terms_conditions") {
                TermsConditionsScreen(onNavigateBack = { bottomNavController.navigateUp() })
            }
            composable("rewards_hub") {
                RewardsHubScreen(
                    viewModel = rewardsViewModel,
                    liveViewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateWallet = { bottomNavController.navigate("rewards_wallet") },
                    onNavigateRedeem = { bottomNavController.navigate("rewards_redeem") },
                    onNavigateRefer = { bottomNavController.navigate("rewards_refer") },
                    onNavigateTasks = { bottomNavController.navigate("rewards_tasks") },
                    onNavigateAchievements = { bottomNavController.navigate("rewards_achievements") },
                    onNavigateLeaderboard = { bottomNavController.navigate("rewards_leaderboard") },
                    onNavigateCoinHistory = { bottomNavController.navigate("rewards_coin_history") },
                    onNavigateInfo = { bottomNavController.navigate("rewards_info") },
                    onNavigateDailyCheckIn = { bottomNavController.navigate("rewards_daily_checkin") },
                    onNavigateSpin = { bottomNavController.navigate("rewards_spin") },
                    onSelectTransaction = { bottomNavController.navigate("rewards_transaction_detail") },
                    onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_feed/$roomId") },
                    onNavigateLiveDiscover = { bottomNavController.navigate("live_discover") },
                    onNavigateLiveWallet = { bottomNavController.navigate("live_wallet") },
                    onNavigateLiveEarnings = { bottomNavController.navigate("live_earnings") }
                )
            }
            composable("live_discover") {
                LiveDiscoverScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") },
                    onNavigateWallet = { bottomNavController.navigate("live_wallet") },
                    onNavigateStudio = { bottomNavController.navigate("live_setup") },
                    onNavigateLeaderboard = { bottomNavController.navigate("live_leaderboard") },
                    onNavigateProfile = { bottomNavController.navigate("live_profile") },
                    onNavigatePeople = { bottomNavController.navigate("live_people") },
                    onNavigateMessages = { bottomNavController.navigate("live_private_chat/creator_1") },
                    onNavigateDemoControls = { bottomNavController.navigate("live_demo_controls") }
                )
            }
            composable(
                route = "live_room/{roomId}",
                arguments = listOf(navArgument("roomId") { defaultValue = "room_1" })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: "room_1"
                LiveFeedScreen(
                    initialRoomId = roomId,
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateWallet = { bottomNavController.navigate("live_wallet") },
                    onNavigateSummary = { rId -> bottomNavController.navigate("live_summary") },
                    onNavigatePrivateChat = { friendId -> bottomNavController.navigate("live_private_chat/$friendId") },
                    onNavigateFullProfile = { userId -> bottomNavController.navigate("live_profile?userId=$userId") }
                )
            }
            composable(
                route = "live_feed/{roomId}",
                arguments = listOf(navArgument("roomId") { defaultValue = "room_1" })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: "room_1"
                LiveFeedScreen(
                    initialRoomId = roomId,
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateWallet = { bottomNavController.navigate("live_wallet") },
                    onNavigateSummary = { rId -> bottomNavController.navigate("live_summary") },
                    onNavigatePrivateChat = { friendId -> bottomNavController.navigate("live_private_chat/$friendId") },
                    onNavigateFullProfile = { userId -> bottomNavController.navigate("live_profile?userId=$userId") }
                )
            }
            composable("live_people") {
                PeopleAndFriendsScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateProfile = { userId -> bottomNavController.navigate("live_profile?userId=$userId") },
                    onNavigatePrivateChat = { friendId -> bottomNavController.navigate("live_private_chat/$friendId") }
                )
            }
            composable(
                route = "live_private_chat/{friendId}",
                arguments = listOf(navArgument("friendId") { defaultValue = "creator_1" })
            ) { backStackEntry ->
                val friendId = backStackEntry.arguments?.getString("friendId") ?: "creator_1"
                PrivateChatScreen(
                    friendId = friendId,
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateProfile = { userId -> bottomNavController.navigate("live_profile?userId=$userId") }
                )
            }
            composable("live_wallet") {
                DiamondWalletScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("live_earnings") {
                CreatorEarningsScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("live_studio") {
                CreatorStudioScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") },
                    onNavigateSetup = { bottomNavController.navigate("live_setup") }
                )
            }
            composable("live_summary") {
                LiveEndSummaryScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateStudio = { bottomNavController.navigate("live_studio") },
                    onNavigateSetup = { bottomNavController.navigate("live_setup") }
                )
            }
            composable("live_setup") {
                LiveSetupScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") }
                )
            }
            composable("live_leaderboard") {
                LiveLeaderboardScreen(
                    viewModel = liveViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable(
                route = "live_profile?userId={userId}",
                arguments = listOf(navArgument("userId") { defaultValue = "current_user" })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: "current_user"
                if (userId == "current_user" || userId == "user_me") {
                    LaunchedEffect(Unit) {
                        bottomNavController.navigate("profile") {
                            popUpTo("live_profile?userId=$userId") { inclusive = true }
                        }
                    }
                } else {
                    LiveProfileScreen(
                        viewModel = liveViewModel,
                        userId = userId,
                        onNavigateBack = { bottomNavController.navigateUp() },
                        onNavigateMainProfile = {
                            bottomNavController.navigate("profile") {
                                popUpTo("live_profile?userId=$userId") { inclusive = true }
                            }
                        },
                        onNavigateWallet = { bottomNavController.navigate("live_wallet") },
                        onNavigateEarnings = { bottomNavController.navigate("live_earnings") },
                        onNavigateStudio = { bottomNavController.navigate("live_setup") },
                        onNavigateLiveRoom = { roomId -> bottomNavController.navigate("live_room/$roomId") },
                        onNavigatePrivateChat = { friendId -> bottomNavController.navigate("live_private_chat/$friendId") },
                        onNavigateDemoControls = { bottomNavController.navigate("live_demo_controls") },
                        onNavigatePeople = { bottomNavController.navigate("live_people") },
                        onNavigateUserProfile = { targetUid ->
                            if (targetUid != userId) {
                                bottomNavController.navigate("live_profile?userId=$targetUid")
                            }
                        }
                    )
                }
            }
            composable("live_demo_controls") {
                DeveloperDemoControlsSheet(
                    viewModel = liveViewModel,
                    onDismiss = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_daily_checkin") {
                DailyCheckInScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateSpin = { bottomNavController.navigate("rewards_spin") }
                )
            }
            composable("rewards_spin") {
                ConstructionSpinScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_wallet") {
                WalletScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateRedeem = { bottomNavController.navigate("rewards_redeem") },
                    onNavigateCoinHistory = { bottomNavController.navigate("rewards_coin_history") },
                    onSelectTransaction = { bottomNavController.navigate("rewards_transaction_detail") }
                )
            }
            composable("rewards_coin_history") {
                CoinHistoryScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onSelectTransaction = { bottomNavController.navigate("rewards_transaction_detail") }
                )
            }
            composable("rewards_transaction_detail") {
                TransactionDetailScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_refer") {
                ReferAndEarnScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateReferralHistory = { bottomNavController.navigate("rewards_referral_history") }
                )
            }
            composable("rewards_referral_history") {
                ReferralHistoryScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_tasks") {
                TasksScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onSelectTask = { bottomNavController.navigate("rewards_task_detail") }
                )
            }
            composable("rewards_task_detail") {
                TaskDetailScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_achievements") {
                AchievementsScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onSelectAchievement = { bottomNavController.navigate("rewards_achievement_detail") }
                )
            }
            composable("rewards_achievement_detail") {
                AchievementDetailScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_leaderboard") {
                LeaderboardScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_redeem") {
                RedeemRewardsScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() },
                    onNavigateRedemptionHistory = { bottomNavController.navigate("rewards_redemption_history") }
                )
            }
            composable("rewards_redemption_history") {
                RedemptionHistoryScreen(
                    viewModel = rewardsViewModel,
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }
            composable("rewards_info") {
                RewardsInfoScreen(
                    onNavigateBack = { bottomNavController.navigateUp() }
                )
            }

        }
            }
        }

        if (isCompactPortrait && currentRoute == "main_pager") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                FloatingBottomNavigation(
                    currentRoute = currentNavRoute,
                    onNavigate = onNavSelected
                )
            }
        }

        if (showAdvanceDialog) {
            val existingAmount = dashboardState.todayAdvanceAmount
            LaunchedEffect(showAdvanceDialog) {
                if (existingAmount != null && existingAmount > 0.0) {
                    advanceAmount = if (existingAmount % 1.0 == 0.0) {
                        existingAmount.toInt().toString()
                    } else {
                        existingAmount.toString()
                    }
                } else {
                    advanceAmount = ""
                }
            }

            AlertDialog(
                onDismissRequest = { showAdvanceDialog = false },
                title = {
                    Text(
                        text = if (existingAmount != null && existingAmount > 0.0) {
                            androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.edit_advance_payment)
                        } else {
                            androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.add_advance_payment)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = advanceAmount,
                            onValueChange = { advanceAmount = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.amount)) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                val symbol = CountryRepository.getCountry(settingsState.countryCode).currencySymbol
                                Text(symbol, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = CurrencyVisualTransformation(settingsState.currencyCode)
                        )
                    }
                },
                confirmButton = {
                    val isValidAmount = remember(advanceAmount, settingsState.currencyCode) {
                        val parsed = AmountFormatter.parseToDoubleOrNull(advanceAmount, settingsState.currencyCode)
                        parsed != null && parsed > 0.0 && advanceAmount.trim().isNotEmpty()
                    }
                    Button(
                        onClick = {
                            val amount = AmountFormatter.parseToDoubleOrNull(advanceAmount, settingsState.currencyCode)
                            if (amount != null && amount > 0.0) {
                                dashboardViewModel.addAdvance(amount)
                                showAdvanceDialog = false
                                advanceAmount = ""
                            }
                        },
                        enabled = isValidAmount,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdvanceDialog = false }) { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel)) }
                }
            )
        }


        dashboardState.selectedDrillDownType?.let { type ->
            val titleRes = when(type) {
                DrillDownType.EARNINGS -> com.synfusion.attendance.R.string.drilldown_earnings
                DrillDownType.PRESENT -> com.synfusion.attendance.R.string.drilldown_present
                DrillDownType.ABSENT -> com.synfusion.attendance.R.string.drilldown_absent
                DrillDownType.HALF_DAY -> com.synfusion.attendance.R.string.drilldown_half_day
                DrillDownType.OVERTIME -> com.synfusion.attendance.R.string.drilldown_overtime
                DrillDownType.ADVANCE -> com.synfusion.attendance.R.string.drilldown_advance
            }
            DrillDownBottomSheet(
                title = androidx.compose.ui.res.stringResource(titleRes),
                logs = dashboardState.selectedLogs,
                onDismiss = { dashboardViewModel.closeDrillDown() },
                totalRecordsText = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.records_count, dashboardState.selectedLogs.size),
                totalAmountText = if (type == DrillDownType.EARNINGS || type == DrillDownType.OVERTIME || type == DrillDownType.ADVANCE) {
                    val total = if (type == DrillDownType.ADVANCE) {
                        dashboardState.selectedLogs.sumOf { it.advanceAmount ?: 0.0 }
                    } else if (type == DrillDownType.OVERTIME) {
                        dashboardState.selectedLogs.sumOf { it.overtimeAmount }
                    } else {
                        dashboardState.selectedLogs.sumOf { it.baseEarnings + it.overtimeAmount }
                    }
                    AmountFormatter.formatWithSymbol(total, settingsState.currencyCode)
                } else null
            )
        }
    }
}
}

@Composable
fun DashboardShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerCircle(size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerTextLine(height = 20.dp, fraction = 0.4f)
                ShimmerTextLine(height = 14.dp, fraction = 0.6f)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Stats Grid Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 100.dp, cornerRadius = 16.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 100.dp, cornerRadius = 16.dp)
        }

        // View Passbook Button Shimmer
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 56.dp, cornerRadius = 16.dp)

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Actions Title Shimmer
        ShimmerTextLine(height = 14.dp, fraction = 0.3f)
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 88.dp, cornerRadius = 16.dp)

        Spacer(modifier = Modifier.height(8.dp))

        // Daily Log Title Shimmer
        ShimmerTextLine(height = 14.dp, fraction = 0.2f)
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 72.dp, cornerRadius = 16.dp)
        ShimmerBox(modifier = Modifier.fillMaxWidth(), height = 56.dp, cornerRadius = 12.dp)
    }
}

@Composable
fun HeaderSection(
    state: OfflineDashboardState,
    googlePhotoUrl: String = "",
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val todayCal = remember { Calendar.getInstance() }
    val todayFullDate = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(todayCal.time)
    }

    val displayName = state.name.trim()
    val firstName = if (displayName.isNotEmpty()) displayName.split(" ")[0] else "User"
    val avatarLetters = if (displayName.length >= 2) displayName.take(2).uppercase() else if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "U"

    val imageFile = remember(state.photoUrl) {
        if (state.photoUrl.isNotEmpty()) java.io.File(state.photoUrl) else null
    }
    val showLocalImage = imageFile != null && imageFile.exists()
    val localImageRequest = remember(state.photoUrl, showLocalImage) {
        if (imageFile != null && showLocalImage) {
            coil.request.ImageRequest.Builder(context)
                .data(imageFile)
                .crossfade(true)
                .memoryCacheKey("${state.photoUrl}_${imageFile.lastModified()}")
                .diskCacheKey("${state.photoUrl}_${imageFile.lastModified()}")
                .networkCachePolicy(coil.request.CachePolicy.DISABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        } else null
    }

    // Remote photo URL (Google photo or Firestore public profile photo) as fallback
    val remotePhotoUrl = remember(state.photoUrl, googlePhotoUrl) {
        if (showLocalImage) null else googlePhotoUrl.takeIf { it.isNotBlank() }
    }
    val remoteImageRequest = remember(remotePhotoUrl) {
        if (remotePhotoUrl != null) {
            coil.request.ImageRequest.Builder(context)
                .data(remotePhotoUrl)
                .crossfade(true)
                .memoryCacheKey(remotePhotoUrl)
                .diskCacheKey(remotePhotoUrl)
                .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        } else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onProfileClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLocalImage && localImageRequest != null) {
                        coil.compose.SubcomposeAsyncImage(
                            model = localImageRequest,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            error = {
                                if (remoteImageRequest != null) {
                                    coil.compose.SubcomposeAsyncImage(
                                        model = remoteImageRequest,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        error = {
                                            Text(
                                                text = avatarLetters,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    )
                                } else {
                                    Text(
                                        text = avatarLetters,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        )
                    } else if (remoteImageRequest != null) {
                        coil.compose.SubcomposeAsyncImage(
                            model = remoteImageRequest,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            error = {
                                Text(
                                    text = avatarLetters,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    } else {
                        Text(
                            text = avatarLetters,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Hi, ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = firstName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = WavingHandIcon,
                            contentDescription = "Greeting",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = todayFullDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Replace Coin Pill with User Search + Notification Bell
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = com.synfusion.attendance.ui.components.icons.WorkDailyIcons.Search,
                        contentDescription = "Search People",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = onMessagesClick) {
                    Box {
                        Icon(
                            imageVector = com.synfusion.attendance.ui.components.icons.WorkDailyIcons.Chat,
                            contentDescription = "Messages",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.unreadMessageCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp),
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White,
                                content = { Text(text = kotlin.math.min(state.unreadMessageCount, 99).toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                IconButton(onClick = onNotificationClick) {
                    Box {
                        Icon(
                            imageVector = com.synfusion.attendance.ui.components.icons.WorkDailyIcons.Bell,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
}

@Composable
fun SmartDailyRewardsWidget(
    rewardsViewModel: RewardsViewModel,
    onNavigateHub: () -> Unit,
    onNavigateDailyCheckIn: () -> Unit,
    onNavigateSpin: () -> Unit,
    onNavigateAchievements: () -> Unit
) {
    val checkInState by rewardsViewModel.dailyCheckInState.collectAsState()
    val streakState by rewardsViewModel.streakState.collectAsState()
    val spinTokens by rewardsViewModel.spinTokens.collectAsState()
    val achievements by rewardsViewModel.achievements.collectAsState()

    val isCheckInClaimed = checkInState.isTodayClaimed
    val hasSpins = spinTokens.availableSpins > 0

    val dynamicSubtitle = when {
        !isCheckInClaimed -> "Your daily reward is ready!"
        hasSpins -> "Your spin is waiting!"
        else -> "You're all caught up today!"
    }

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount = achievements.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateHub),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RewardsPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Rewards",
                            tint = RewardsPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Today's Rewards",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dynamicSubtitle,
                            fontSize = 12.sp,
                            color = if (!isCheckInClaimed || hasSpins) com.synfusion.attendance.rewards.ui.components.RewardsSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (!isCheckInClaimed || hasSpins) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigateHub() }
                ) {
                    Text(
                        text = "Explore All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsPurple
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Explore",
                        tint = RewardsPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Two Mini Action Cards Side by Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini Action 1: Daily Check-in
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateDailyCheckIn() },
                    color = if (!isCheckInClaimed) RewardsPurple.copy(alpha = 0.08f) else MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!isCheckInClaimed) RewardsPurple.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.synfusion.attendance.R.drawable.ic_wd_daily_checkin),
                            contentDescription = "Daily Check-in",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Daily Check-in",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCheckInClaimed) "Claimed" else "Ready +5 🪙",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCheckInClaimed) com.synfusion.attendance.rewards.ui.components.RewardsSuccessGreen else RewardsPurple
                            )
                        }
                    }
                }

                // Mini Action 2: Builder's Spin
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateSpin() },
                    color = if (hasSpins) com.synfusion.attendance.rewards.ui.components.GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasSpins) com.synfusion.attendance.rewards.ui.components.GoldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.synfusion.attendance.R.drawable.ic_wd_builder_spin),
                            contentDescription = "Builder's Spin",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Builder's Spin",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasSpins) "${spinTokens.availableSpins} Spin Ready" else "0 Spins Left",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasSpins) com.synfusion.attendance.rewards.ui.components.GoldSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Contextual Achievement / Streak Strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateAchievements() },
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔥 ${streakState.currentStreak} Day Streak",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Badges: $unlockedCount / $totalCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Badges",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeFestivalBanner(state: OfflineDashboardState) {
    if (!state.festivalsEnabled) return

    val festival = state.todayFestival ?: state.tomorrowFestival ?: return
    val isToday = state.todayFestival != null

    val lang = Locale.getDefault().language
    val displayName = if (lang == "hi" && !festival.nameHi.isNullOrBlank()) festival.nameHi else festival.name
    val subText = if (isToday) {
        androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.festival_today)
    } else {
        androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.tomorrow)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = Color(0xFF7C4DFF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = Color(0xFF7C4DFF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PersonalStatsGrid(state: OfflineDashboardState, currencyCode: String, onNavigatePassbook: () -> Unit, onEarningsClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEarningsClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.todays_earnings),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = AmountFormatter.formatWithSymbol(state.todayEarned, currencyCode),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEarningsClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.monthly_earnings),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = AmountFormatter.formatWithSymbol(state.monthEarned, currencyCode),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigatePassbook),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.FileText, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.view_my_passbook),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AllTimePassbookButton(onNavigate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onNavigate)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(com.synfusion.attendance.ui.components.icons.WorkDailyIcons.History, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.all_time_passbook_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun PersonalQuickActions(
    todayAdvanceAmount: Double?,
    currencyCode: String,
    onAddAdvance: () -> Unit
) {
    Column {
        Text(
            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.quick_actions),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay().copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (todayAdvanceAmount != null && todayAdvanceAmount > 0.0) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay(),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    if (todayAdvanceAmount != null && todayAdvanceAmount > 0.0) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_payment),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AmountFormatter.formatWithSymbol(todayAdvanceAmount, currencyCode),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.add_advance_payment),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.advance_payment_rs),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (todayAdvanceAmount != null && todayAdvanceAmount > 0.0) {
                Button(
                    onClick = onAddAdvance,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.edit_advance),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            } else {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PersonalDailyLog(state: OfflineDashboardState, currencyCode: String, dashboardViewModel: OfflineDashboardViewModel) {
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
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_log), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

        if (state.todayStatus == null) {
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.overtime_hours), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent().copy(alpha = 0.1f)).clickable { if (overtimeHours > 0) overtimeHours-- }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Remove, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()) }
                    Text(overtimeHours.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent().copy(alpha = 0.1f)).clickable { overtimeHours++ }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, contentDescription = null, tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()) }
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
                visualTransformation = CurrencyVisualTransformation(currencyCode)
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
            val isHalf = state.todayType == "half"
            val bgColor = if (isPresent) {
                if (isHalf) com.synfusion.attendance.ui.theme.AttendanceStatusColors.getHalfDay() else com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
            } else {
                com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent()
            }

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
                    val markedText = if (isPresent) {
                        if (isHalf) androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.marked_half_day) else androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.marked_present)
                    } else {
                        androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.marked_absent)
                    }
                    Text(markedText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = bgColor)
                    if (note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            ExpandableNoteText(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { dashboardViewModel.removeAttendance() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent(), containerColor = MaterialTheme.colorScheme.background), border = androidx.compose.foundation.BorderStroke(1.dp, com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent())
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptiveNavigationRail(currentRoute: String, onNavigate: (String) -> Unit = {}) {
    data class NavItem(val route: String, val icon: ImageVector, val label: String)

    val tabs = listOf(
        NavItem("dashboard", Icons.Default.Home, "Home"),
        NavItem("calendar", Icons.Default.CalendarMonth, "Calendar"),
        NavItem("stats", Icons.Default.BarChart, "Status"),
        NavItem("live_discover", Icons.Default.LiveTv, "Live")
    )

    val coroutineScope = rememberCoroutineScope()
    val requesters = remember { tabs.map { androidx.compose.foundation.relocation.BringIntoViewRequester() } }

    LaunchedEffect(currentRoute) {
        val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }
        if (selectedIndex >= 0) {
            requesters[selectedIndex].bringIntoView()
        }
    }

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        windowInsets = WindowInsets(0.dp),
        header = {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.synfusion.attendance.R.drawable.logo_transparent),
                    contentDescription = "WorkDaily",
                    modifier = Modifier.size(32.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = tab.route == currentRoute
                val requester = requesters[index]
                NavigationRailItem(
                    modifier = Modifier.bringIntoViewRequester(requester),
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
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
                    onClick = {
                        onNavigate(tab.route)
                        coroutineScope.launch {
                            requester.bringIntoView()
                        }
                    },
                    colors = NavigationRailItemDefaults.colors(
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
}
