package com.synfusion.workdailyadmin.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

@Composable
fun AdminMainLayout(
    authViewModel: AdminAuthViewModel
) {
    val navController = rememberNavController()

    val dashboardViewModel = remember { DashboardViewModel() }
    val userViewModel = remember { UserViewModel() }
    val liveViewModel = remember { LiveViewModel() }
    val socialViewModel = remember { SocialViewModel() }
    val rewardsViewModel = remember { RewardsViewModel() }
    val moderationViewModel = remember { ModerationViewModel() }
    val notificationsViewModel = remember { NotificationsViewModel() }
    val configViewModel = remember { ConfigViewModel() }
    val teamViewModel = remember { TeamViewModel() }
    val auditViewModel = remember { AuditViewModel() }
    val supportViewModel = remember { SupportViewModel() }
    val healthViewModel = remember { HealthViewModel() }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Management Navigation Rail with vertical scrolling for full feature access
        NavigationRail(
            modifier = Modifier.fillMaxHeight()
        ) {
            IconButton(onClick = { authViewModel.logout() }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                AdminNavItem.values().forEach { item ->
                    NavigationRailItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }

        // Main NavHost Container
        Box(modifier = Modifier.weight(1f)) {
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
    }
}
