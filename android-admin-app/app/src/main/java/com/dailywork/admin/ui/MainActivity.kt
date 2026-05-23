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
import com.dailywork.admin.viewmodel.AuthState
import com.dailywork.admin.viewmodel.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
        Screen.Users,
        Screen.Reports,
        Screen.Notifications,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Users.route, Modifier.padding(innerPadding)) {
            composable(Screen.Users.route) {
                UsersScreen(onUserClick = { userId ->
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
            composable(Screen.Settings.route) { SettingsScreen(onLogout = { authViewModel.logout() }) }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Users : Screen("users", "Users", Icons.Default.People)
    object Reports : Screen("reports", "Reports", Icons.Default.Flag)
    object Notifications : Screen("notifications", "Notify", Icons.Default.Notifications)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object UserDetail : Screen("user_detail/{userId}", "User Detail", Icons.Default.People)
}
