package com.dailywork.admin.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.dailywork.admin.ui.screens.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dailywork.admin.ui.theme.DailyWorkTheme
import com.dailywork.admin.viewmodel.AuthState
import com.dailywork.admin.viewmodel.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyWorkTheme {
                AdminApp()
            }
        }
    }
}

@Composable
fun AdminApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Authenticated -> MainScaffold(authViewModel)
        is AuthState.Loading -> LoadingScreen()
        else -> LoginScreen(authViewModel)
    }
}

@Composable
fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MainScaffold(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Users,
        Screen.Premium,
        Screen.Reports,
        Screen.Settings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = Screen.Dashboard.route,
                Modifier.padding(innerPadding)
            ) {
            composable(Screen.Dashboard.route) {
                LiveDashboardScreen()
            }
            composable(Screen.Users.route) {
                UsersScreen(onUserClick = { userId ->
                    navController.navigate("user_detail/$userId")
                })
            }
            composable(Screen.Premium.route) {
                PremiumManagementScreen(onUserClick = { userId ->
                    navController.navigate("user_detail/$userId")
                })
            }
            composable(Screen.Reports.route) {
                ReportsScreen(onUserClick = { userId ->
                    navController.navigate("user_detail/$userId")
                })
            }
            composable(
                route = Screen.UserDetail.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserDetailScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onNotifyUser = { id ->
                        navController.navigate("${Screen.Notifications.route}?userId=$id")
                    }
                )
            }
            composable(
                route = "${Screen.Notifications.route}?userId={userId}",
                arguments = listOf(
                    navArgument("userId") {
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                NotificationsScreen(targetUserId = userId)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = { authViewModel.logout() },
                    onNavigateToLogs = { navController.navigate(Screen.AdminLogs.route) }
                )
            }
            composable(Screen.AdminLogs.route) {
                AdminLogsScreen(onBack = { navController.popBackStack() })
            }
        }
        }

        // Floating Bottom Navigation Overlaid
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val showBottomNav = items.any { currentDestination?.route == it.route }

        if (showBottomNav) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .wrapContentWidth()
            ) {
                Surface(
                    modifier = Modifier.wrapContentWidth().height(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .fillMaxHeight()
                            .selectableGroup()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            val scale by animateFloatAsState(if (selected) 1.2f else 1.0f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .selectable(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        role = Role.Tab,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = androidx.compose.material.ripple.rememberRipple(
                                            bounded = true,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.scale(scale),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Users : Screen("users", "Users", Icons.Default.People)
    object Premium : Screen("premium", "Premium", Icons.Default.Star)
    object Reports : Screen("reports", "Reports", Icons.Default.Flag)
    object Notifications : Screen("notifications", "Notify", Icons.Default.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object UserDetail : Screen("user_detail/{userId}", "User Detail", Icons.Default.People)
    object AdminLogs : Screen("admin_logs", "Logs", Icons.Default.History)
}
