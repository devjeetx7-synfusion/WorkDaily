package com.synfusion.workdailyadmin.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.synfusion.workdailyadmin.auth.AdminAuthState
import com.synfusion.workdailyadmin.auth.AdminAuthViewModel
import com.synfusion.workdailyadmin.ui.audit.AuditLogsScreen
import com.synfusion.workdailyadmin.ui.audit.AuditViewModel
import com.synfusion.workdailyadmin.ui.config.AppConfigScreen
import com.synfusion.workdailyadmin.ui.config.ConfigViewModel
import com.synfusion.workdailyadmin.ui.dashboard.DashboardScreen
import com.synfusion.workdailyadmin.ui.dashboard.DashboardViewModel
import com.synfusion.workdailyadmin.ui.health.HealthViewModel
import com.synfusion.workdailyadmin.ui.health.SystemHealthScreen
import com.synfusion.workdailyadmin.ui.live.LiveManagementScreen
import com.synfusion.workdailyadmin.ui.live.LiveViewModel
import com.synfusion.workdailyadmin.ui.notifications.NotificationsScreen
import com.synfusion.workdailyadmin.ui.notifications.NotificationsViewModel
import com.synfusion.workdailyadmin.ui.reports.ModerationViewModel
import com.synfusion.workdailyadmin.ui.reports.ReportsScreen
import com.synfusion.workdailyadmin.ui.rewards.RewardsManagementScreen
import com.synfusion.workdailyadmin.ui.rewards.RewardsViewModel
import com.synfusion.workdailyadmin.ui.social.SocialManagementScreen
import com.synfusion.workdailyadmin.ui.social.SocialViewModel
import com.synfusion.workdailyadmin.ui.support.SupportTicketsScreen
import com.synfusion.workdailyadmin.ui.support.SupportViewModel
import com.synfusion.workdailyadmin.ui.team.AdminTeamScreen
import com.synfusion.workdailyadmin.ui.team.TeamViewModel
import com.synfusion.workdailyadmin.ui.users.UserDetailScreen
import com.synfusion.workdailyadmin.ui.users.UserManagementScreen
import com.synfusion.workdailyadmin.ui.users.UserViewModel
import kotlinx.coroutines.launch

enum class AdminNavItem(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    USERS("users", "Users", Icons.Default.People),
    LIVE("live", "Live", Icons.Default.LiveTv),
    SOCIAL("social", "Social", Icons.Default.Public),
    REWARDS("rewards", "Rewards", Icons.Default.MonetizationOn),
    REPORTS("reports", "Reports", Icons.Default.ReportProblem),
    NOTIFICATIONS("notifications", "Notify", Icons.Default.Notifications),
    CONFIG("config", "Config", Icons.Default.Settings),
    TEAM("team", "Team", Icons.Default.SupervisorAccount),
    AUDIT("audit", "Audit", Icons.Default.History),
    SUPPORT("support", "Support", Icons.AutoMirrored.Filled.HelpCenter),
    HEALTH("health", "Health", Icons.Default.HealthAndSafety)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainLayout(
    authViewModel: AdminAuthViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val dashboardViewModel: DashboardViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()
    val liveViewModel: LiveViewModel = viewModel()
    val socialViewModel: SocialViewModel = viewModel()
    val rewardsViewModel: RewardsViewModel = viewModel()
    val moderationViewModel: ModerationViewModel = viewModel()
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val configViewModel: ConfigViewModel = viewModel()
    val teamViewModel: TeamViewModel = viewModel()
    val auditViewModel: AuditViewModel = viewModel()
    val supportViewModel: SupportViewModel = viewModel()
    val healthViewModel: HealthViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AdminNavItem.DASHBOARD.route

    val authState by authViewModel.authState.collectAsState()
    val adminEmail = (authState as? AdminAuthState.Authenticated)?.email ?: "admin@workdaily.com"
    val adminRole = (authState as? AdminAuthState.Authenticated)?.role?.name ?: "ADMIN"

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isCompact = maxWidth < 700.dp

        if (isCompact) {
            // Mobile Layout with ModalNavigationDrawer
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .width(290.dp)
                            .fillMaxHeight(),
                        windowInsets = WindowInsets.statusBars
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "WorkDaily",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Admin Control Center",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = adminEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Role: $adminRole",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            AdminNavItem.values().forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            item.icon,
                                            contentDescription = item.label,
                                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    label = {
                                        Text(
                                            item.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f, fill = false))
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sign Out",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                label = {
                                    Text(
                                        "Sign Out",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    authViewModel.logout()
                                },
                                modifier = Modifier.padding(12.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        val activeItem = AdminNavItem.values().find { it.route == currentRoute }
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = activeItem?.label ?: "WorkDaily Admin",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Administration & Oversight",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = { authViewModel.logout() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sign Out",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AdminNavHost(
                            navController = navController,
                            dashboardViewModel = dashboardViewModel,
                            userViewModel = userViewModel,
                            liveViewModel = liveViewModel,
                            socialViewModel = socialViewModel,
                            rewardsViewModel = rewardsViewModel,
                            moderationViewModel = moderationViewModel,
                            notificationsViewModel = notificationsViewModel,
                            configViewModel = configViewModel,
                            teamViewModel = teamViewModel,
                            auditViewModel = auditViewModel,
                            supportViewModel = supportViewModel,
                            healthViewModel = healthViewModel
                        )
                    }
                }
            }
        } else {
            // Tablet & Desktop Layout with Persistent Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin Brand",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(modifier = Modifier.width(36.dp))
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AdminNavItem.values().forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // Bottom Sign Out Action
                    IconButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AdminNavHost(
                        navController = navController,
                        dashboardViewModel = dashboardViewModel,
                        userViewModel = userViewModel,
                        liveViewModel = liveViewModel,
                        socialViewModel = socialViewModel,
                        rewardsViewModel = rewardsViewModel,
                        moderationViewModel = moderationViewModel,
                        notificationsViewModel = notificationsViewModel,
                        configViewModel = configViewModel,
                        teamViewModel = teamViewModel,
                        auditViewModel = auditViewModel,
                        supportViewModel = supportViewModel,
                        healthViewModel = healthViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminNavHost(
    navController: androidx.navigation.NavHostController,
    dashboardViewModel: DashboardViewModel,
    userViewModel: UserViewModel,
    liveViewModel: LiveViewModel,
    socialViewModel: SocialViewModel,
    rewardsViewModel: RewardsViewModel,
    moderationViewModel: ModerationViewModel,
    notificationsViewModel: NotificationsViewModel,
    configViewModel: ConfigViewModel,
    teamViewModel: TeamViewModel,
    auditViewModel: AuditViewModel,
    supportViewModel: SupportViewModel,
    healthViewModel: HealthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = AdminNavItem.DASHBOARD.route
    ) {
        composable("dashboard") {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateUsers = { navController.navigate("users") },
                onNavigateReports = { navController.navigate("reports") },
                onNavigateLive = { navController.navigate("live") },
                onNavigateRewards = { navController.navigate("rewards") }
            )
        }
        composable("users") {
            UserManagementScreen(
                viewModel = userViewModel,
                onSelectUser = { uid -> navController.navigate("user_detail/$uid") }
            )
        }
        composable(
            route = "user_detail/{targetUid}",
            arguments = listOf(navArgument("targetUid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("targetUid") ?: ""
            UserDetailScreen(
                targetUid = uid,
                viewModel = userViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("live") { LiveManagementScreen(viewModel = liveViewModel) }
        composable("social") { SocialManagementScreen(viewModel = socialViewModel) }
        composable("rewards") { RewardsManagementScreen(viewModel = rewardsViewModel) }
        composable("reports") { ReportsScreen(viewModel = moderationViewModel) }
        composable("notifications") { NotificationsScreen(viewModel = notificationsViewModel) }
        composable("config") { AppConfigScreen(viewModel = configViewModel) }
        composable("team") { AdminTeamScreen(viewModel = teamViewModel) }
        composable("audit") { AuditLogsScreen(viewModel = auditViewModel) }
        composable("support") { SupportTicketsScreen(viewModel = supportViewModel) }
        composable("health") { SystemHealthScreen(viewModel = healthViewModel) }
    }
}
