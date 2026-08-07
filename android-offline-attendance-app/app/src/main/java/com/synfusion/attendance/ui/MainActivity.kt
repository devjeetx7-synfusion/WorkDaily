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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.ui.theme.DailyWorkTheme
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import com.synfusion.attendance.viewmodel.OfflineViewModelFactory
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.LocalizationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = OfflinePreferencesRepository(applicationContext)
        val factory = OfflineViewModelFactory(repository, applicationContext)

        lifecycleScope.launch {
            setContent {
                val userThemePreference by repository.darkModeFlow.collectAsState(initial = null)
                val isDarkMode = userThemePreference ?: isSystemInDarkTheme()
                val languageState by repository.languageFlow.collectAsState(initial = "en")
                val activeLanguage = languageState ?: "en"

                val baseActivityContext = LocalContext.current
                val localizedContext = LocalizationManager.applyLanguage(baseActivityContext, activeLanguage)
                val wrappedContext = com.synfusion.attendance.utils.LocalizedContextWrapper(baseActivityContext, localizedContext)

                CompositionLocalProvider(LocalContext provides wrappedContext) {
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
}

@Composable
fun OfflineDailyWorkApp(factory: OfflineViewModelFactory) {
    val navController = rememberNavController()
    val authViewModel: OfflineAuthViewModel = viewModel(factory = factory)
    val settingsViewModel: OfflineSettingsViewModel = viewModel(factory = factory)

    val tokenState by authViewModel.authTokenFlow.collectAsStateWithLifecycle(initialValue = "LOADING")
    val languageState by authViewModel.repository.languageFlow.collectAsStateWithLifecycle(initialValue = "LOADING")
    val splashEnabled by authViewModel.repository.splashEnabledFlow.collectAsStateWithLifecycle(initialValue = true)

    if (tokenState == "LOADING" || languageState == "LOADING") {
        return
    }

    val startDest = if (splashEnabled == false) {
        if (languageState == null) "language_selection"
        else if (tokenState == null) "login"
        else "dashboard"
    } else {
        "splash"
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
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
