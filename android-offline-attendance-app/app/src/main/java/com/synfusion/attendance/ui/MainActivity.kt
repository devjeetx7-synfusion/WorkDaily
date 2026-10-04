package com.synfusion.attendance.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.ui.responsive.ProvideWindowSizeInfo
import com.synfusion.attendance.ui.theme.DailyWorkTheme
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import com.synfusion.attendance.viewmodel.OfflineViewModelFactory
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.synfusion.attendance.R
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private var externalPdfUriState = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handlePdfIntent(intent)

        val repository = OfflinePreferencesRepository(applicationContext)
        val factory = OfflineViewModelFactory(repository, applicationContext)

        // Read language synchronously to avoid initial mismatch and flicker
        val savedLanguage = kotlinx.coroutines.runBlocking { repository.languageFlow.first() } ?: "en"
        val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val currentLang = if (!currentLocales.isEmpty) currentLocales.get(0)?.language else null
        if (currentLang != savedLanguage) {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(savedLanguage)
            )
        }

        val restoreReport = com.synfusion.attendance.backup.RestoreResultStore.getAndClearReport(applicationContext)

        setContent {
            val userThemePreference by repository.darkModeFlow.collectAsState(initial = null)
            val isDarkMode = userThemePreference ?: isSystemInDarkTheme()

            var showRestoreSuccessDialog by remember { mutableStateOf(restoreReport) }
            val currentExternalPdfUri by externalPdfUriState

            DailyWorkTheme(darkTheme = isDarkMode) {
                ProvideWindowSizeInfo {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            OfflineDailyWorkApp(
                                factory = factory,
                                externalPdfUri = currentExternalPdfUri,
                                onClearExternalPdfUri = { externalPdfUriState.value = null }
                            )

                            showRestoreSuccessDialog?.let { report ->
                                RestoreSuccessDetailDialog(
                                    report = report,
                                    onDismiss = { showRestoreSuccessDialog = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePdfIntent(intent)
    }

    private fun handlePdfIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type
        val data = intent.data
        if (action == Intent.ACTION_VIEW && data != null) {
            val mime = type ?: contentResolver.getType(data)
            if (mime == "application/pdf" || data.toString().lowercase(Locale.ROOT).contains(".pdf")) {
                externalPdfUriState.value = data
            }
        }
    }
}

@Composable
fun RestoreSuccessDetailDialog(
    report: Map<String, Any>,
    onDismiss: () -> Unit
) {
    val attendanceCount = (report["attendanceCount"] as? Number)?.toInt() ?: 0
    val totalAttendance = (report["totalAttendanceRecords"] as? Number)?.toInt() ?: 0
    val profileRestored = report["profileRestored"] as? Boolean ?: false
    val mediaCount = (report["mediaCount"] as? Number)?.toInt() ?: 0
    val totalMedia = (report["totalMediaFiles"] as? Number)?.toInt() ?: 0
    val settingsRestored = report["settingsRestored"] as? Boolean ?: false
    val backupDate = (report["backupDate"] as? Number)?.toLong() ?: 0L
    val restoreDate = (report["restoreDate"] as? Number)?.toLong() ?: 0L
    val backupSize = (report["backupSize"] as? Number)?.toLong() ?: 0L
    val databaseVersion = (report["databaseVersion"] as? Number)?.toInt() ?: 2
    val isMerge = report["isMerge"] as? Boolean ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.restore_success),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.restore_data_success_msg),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Detail Rows
                DetailRow(label = stringResource(R.string.restore_mode), value = if (isMerge) "Merge Data" else "Replace All")
                DetailRow(label = "Total Attendance", value = "$attendanceCount Records")
                DetailRow(label = "User Profile", value = if (profileRestored) "Restored ✓" else "Not Restored")
                DetailRow(label = "Total Media", value = "$mediaCount / $totalMedia Files")
                DetailRow(label = "Settings Restored", value = if (settingsRestored) "Yes ✓" else "No")
                DetailRow(label = "Database Version", value = "v$databaseVersion")
                DetailRow(label = "Backup Date", value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(backupDate)))
                DetailRow(label = "Restore Date", value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(restoreDate)))
                DetailRow(label = "Backup Size", value = "${String.format("%.2f", backupSize / 1024.0 / 1024.0)} MB")
                DetailRow(label = "Validation Status", value = "Passed ✓")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun OfflineDailyWorkApp(
    factory: OfflineViewModelFactory,
    externalPdfUri: Uri? = null,
    onClearExternalPdfUri: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: OfflineAuthViewModel = viewModel(factory = factory)
    val settingsViewModel: OfflineSettingsViewModel = viewModel(factory = factory)

    val tokenState by authViewModel.authTokenFlow.collectAsStateWithLifecycle(initialValue = "LOADING")
    val languageState by authViewModel.repository.languageFlow.collectAsStateWithLifecycle(initialValue = "LOADING")
    val splashEnabled by authViewModel.repository.splashEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val onboardingCompleted by authViewModel.repository.isOnboardingCompletedFlow.collectAsStateWithLifecycle(initialValue = false)

    if (tokenState == "LOADING" || languageState == "LOADING") {
        return
    }

    val startDest = if (splashEnabled == false) {
        if (languageState == null) "language_selection"
        else if (!onboardingCompleted) "onboarding"
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
                    } else if (!onboardingCompleted) {
                        navController.navigate("onboarding") {
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
                    if (!onboardingCompleted) {
                        navController.navigate("onboarding") {
                            popUpTo("language_selection") { inclusive = true }
                        }
                    } else if (tokenState == null) {
                        navController.navigate("login") {
                            popUpTo("language_selection") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("language_selection") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("onboarding") {
            val onboardingViewModel: com.synfusion.attendance.viewmodel.OfflineOnboardingViewModel = viewModel(factory = factory)
            com.synfusion.attendance.ui.onboarding.OnboardingScreen(
                viewModel = onboardingViewModel,
                onOnboardingComplete = {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
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
                externalPdfUri = externalPdfUri,
                onClearExternalPdfUri = onClearExternalPdfUri,
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
