package com.synfusion.attendance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.ui.theme.DailyWorkTheme
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import com.synfusion.attendance.viewmodel.OfflineViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = OfflinePreferencesRepository(applicationContext)
        val factory = OfflineViewModelFactory(repository, applicationContext)

        lifecycleScope.launch {
            setContent {
                val userThemePreference by repository.darkModeFlow.collectAsState(initial = null)
                val isDarkMode = userThemePreference ?: isSystemInDarkTheme()

                DailyWorkTheme(darkTheme = isDarkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        OfflineDailyWorkApp(factory)
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineDailyWorkApp(factory: OfflineViewModelFactory) {
    val navController = rememberNavController()
    val authViewModel: OfflineAuthViewModel = viewModel(factory = factory)
    val settingsViewModel: OfflineSettingsViewModel = viewModel(factory = factory)

    val tokenState by authViewModel.authTokenFlow.collectAsState(initial = "LOADING")
    val languageState by authViewModel.repository.languageFlow.collectAsState(initial = "LOADING")
    val onboardingCompleted by authViewModel.repository.isOnboardingCompletedFlow.collectAsState(initial = "LOADING")
    val splashEnabled by authViewModel.repository.splashEnabledFlow.collectAsState(initial = "LOADING")

    if (tokenState == "LOADING" || languageState == "LOADING" || onboardingCompleted == "LOADING" || splashEnabled == "LOADING") {
        return
    }

    val startDest = if (splashEnabled == false) {
        if (languageState == null) "language_selection"
        else if (tokenState == null) "login"
        else "dashboard"
    } else {
        "splash"
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    if (languageState == null) {
                        navController.navigate("language_selection") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else if (tokenState == null) {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("language_selection") {
            LanguageSelectionScreen(
                repository = authViewModel.repository,
                onLanguageSelected = {
                    navController.navigate("login") {
                        popUpTo("language_selection") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                selectedLanguage = languageState ?: "en",
                onLoginSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                factory = factory,
                settingsViewModel = settingsViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
