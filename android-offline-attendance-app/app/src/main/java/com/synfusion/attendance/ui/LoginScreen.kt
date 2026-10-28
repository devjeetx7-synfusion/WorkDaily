@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.synfusion.attendance.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.SubcomposeAsyncImage
import com.synfusion.attendance.R
import com.synfusion.attendance.auth.OnlineAccount
import com.synfusion.attendance.auth.OnlineAuthState
import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.backup.ProgressTracker
import com.synfusion.attendance.backup.RestoreManager
import com.synfusion.attendance.data.entities.UserEntity
import com.synfusion.attendance.data.model.CountryConfig
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.getActivity
import com.synfusion.attendance.viewmodel.AccountResolutionState
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
import com.synfusion.attendance.viewmodel.OnlineAuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Modifier.pressScaleEffect() = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "pressScale")

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

@Composable
fun LoginScreen(
    authViewModel: OfflineAuthViewModel,
    onlineAuthViewModel: OnlineAuthViewModel,
    onlineProfileViewModel: com.synfusion.attendance.viewmodel.OnlineProfileViewModel? = null,
    selectedLanguage: String,
    onLoginSuccess: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    val onlineAuthState by onlineAuthViewModel.authState.collectAsState()
    val accountResolutionState by authViewModel.accountResolutionState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)

    var authenticatedAccount by remember { mutableStateOf<OnlineAccount?>(null) }
    var existingUser by remember { mutableStateOf<UserEntity?>(null) }

    var name by remember { mutableStateOf("") }
    var usernameText by remember { mutableStateOf("") }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailability by remember { mutableStateOf<com.synfusion.attendance.online.UsernameAvailabilityResult?>(null) }
    var usernameValidationReason by remember { mutableStateOf<String?>(null) }
    var suggestedUsername by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var dailyWageText by remember { mutableStateOf("500") }
    var selectedGender by remember { mutableStateOf("Male") }
    var selectedProfession by remember { mutableStateOf("Site Engineer") }
    var bioText by remember { mutableStateOf("") }
    var showProfessionSheet by remember { mutableStateOf(false) }

    var selectedCountry by remember { mutableStateOf(CountryRepository.DEFAULT_COUNTRY) }

    var isConsentChecked by rememberSaveable { mutableStateOf(false) }
    var showTermsSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacySheet by rememberSaveable { mutableStateOf(false) }

    var detectedBackupFile by remember { mutableStateOf<File?>(null) }
    var showSmartBackupDialog by remember { mutableStateOf(false) }

    // Detect whether a local workspace exists for "Continue Offline" without leaking fields across accounts
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val user = authViewModel.getExistingUser()
            // Country is resolved only from data the app owns, in priority order:
            //   1. the country already saved on this device's workspace -- reused verbatim, so an
            //      existing user's stored value is never overwritten on launch;
            //   2. CountryRepository.DEFAULT_COUNTRY, the single supported configuration.
            // getCountry() returns DEFAULT_COUNTRY for a null or unsupported code, so one call
            // covers both the saved-value and the fallback case.
            //
            // Device locale is deliberately not consulted. Locale reports language and region
            // preferences, not verified physical location, so treating it as proof of country is
            // unsound. It was also inert here: SUPPORTED_COUNTRIES holds only India, so
            // getCountry(locale) already returned DEFAULT_COUNTRY for every non-IN locale.
            val resolvedCountry = CountryRepository.getCountry(user?.countryCode)
            withContext(Dispatchers.Main) {
                existingUser = user
                selectedCountry = resolvedCountry
            }

            // Scan for smart backup
            val scanDirs = listOfNotNull(
                context.getExternalFilesDir(null),
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            )

            var foundFile: File? = null
            for (dir in scanDirs) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { _, fileName ->
                        fileName.startsWith("AttendanceBackup_") && fileName.endsWith(".zip")
                    }
                    if (!files.isNullOrEmpty()) {
                        foundFile = files.maxByOrNull { it.lastModified() }
                        break
                    }
                }
            }

            if (foundFile != null) {
                detectedBackupFile = foundFile
                showSmartBackupDialog = true
            }
        }
    }

    // Resolve authenticated Google account (existing account auto-login vs new registration vs conflict)
    LaunchedEffect(onlineAuthState) {
        when (val state = onlineAuthState) {
            is OnlineAuthState.Authenticated -> {
                authViewModel.resolveAuthenticatedAccount(state.account)
            }
            is OnlineAuthState.Unauthenticated, is OnlineAuthState.Idle -> {
                authenticatedAccount = null
                authViewModel.resetAccountResolutionState()
            }
            else -> {}
        }
    }

    LaunchedEffect(accountResolutionState) {
        when (val res = accountResolutionState) {
            is AccountResolutionState.SignedInExisting -> {
                authenticatedAccount = null
            }
            is AccountResolutionState.NeedsProfileSetup -> {
                authenticatedAccount = res.account
                name = res.prefillName
                phone = res.prefillPhone
                dailyWageText = if (res.prefillDailyWage > 0.0) res.prefillDailyWage.toInt().toString() else "500"
                selectedCountry = CountryRepository.getCountry(res.prefillCountryCode)
                selectedGender = res.prefillGender
                selectedProfession = res.prefillProfession
                bioText = res.prefillBio
                usernameValidationReason = null
                usernameAvailability = null
                suggestedUsername = null
                usernameText = res.prefillUsername.ifBlank {
                    com.synfusion.attendance.online.UsernameValidator.generateCandidate(
                        res.prefillName.ifBlank { res.account.displayName },
                        res.account.uid
                    )
                }
            }
            else -> {}
        }
    }

    // Debounced username availability check & syntax validation
    LaunchedEffect(usernameText, authenticatedAccount?.uid) {
        val raw = usernameText.trim().removePrefix("@")
        if (raw.isBlank()) {
            usernameValidationReason = null
            usernameAvailability = null
            isCheckingUsername = false
            return@LaunchedEffect
        }

        val syntaxCheck = com.synfusion.attendance.online.UsernameValidator.validate(raw)
        if (syntaxCheck is com.synfusion.attendance.online.UsernameValidationResult.Invalid) {
            usernameValidationReason = syntaxCheck.reason
            usernameAvailability = null
            isCheckingUsername = false
            return@LaunchedEffect
        }

        usernameValidationReason = null
        isCheckingUsername = true
        kotlinx.coroutines.delay(350)

        val currentUid = authenticatedAccount?.uid
        val repo = com.synfusion.attendance.online.FirestorePublicProfileRepository()
        val result = repo.checkUsernameAvailability(raw, currentUid)
        usernameAvailability = result
        isCheckingUsername = false

        if (result is com.synfusion.attendance.online.UsernameAvailabilityResult.AlreadyTaken) {
            val uid = currentUid ?: "9999"
            val candidate = com.synfusion.attendance.online.UsernameValidator.generateCandidate(raw, uid)
            suggestedUsername = if (candidate != raw) candidate else "${raw}_1"
        } else {
            suggestedUsername = null
        }
    }

    val restoreManager = remember { RestoreManager(context) }
    val progressTracker = remember { ProgressTracker() }
    val progress by progressTracker.progress.collectAsState(initial = 0f)
    val status by progressTracker.status.collectAsState(initial = "")

    var isOperating by remember { mutableStateOf(false) }

    BackHandler(enabled = isOperating) {
        // Block back button during restore
    }

    var restorePreview by remember { mutableStateOf<BackupManifest?>(null) }
    var restoreFileToApply by remember { mutableStateOf<File?>(null) }

    var operationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // SAF File Picker launcher
    val pickRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val tempFile = File(context.cacheDir, "RestoreTempLogin.zip")
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                FileOutputStream(tempFile).use { fos ->
                                    inputStream.copyTo(fos)
                                }
                            }
                        }

                        val manifest = restoreManager.getBackupPreview(tempFile)
                        if (manifest != null) {
                            restorePreview = manifest
                            restoreFileToApply = tempFile
                        } else {
                            tempFile.delete()
                            Toast.makeText(context, context.getString(R.string.restore_invalid_format_msg), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.restore_read_failed_msg), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, context.getString(R.string.restore_cancelled_msg), Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (restorePreview != null) {
        val preview = restorePreview!!
        AlertDialog(
            onDismissRequest = {
                restorePreview = null
                restoreFileToApply?.delete()
                restoreFileToApply = null
            },
            title = { Text(stringResource(R.string.prev_backup_preview), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_parsed_msg))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• " + stringResource(R.string.last_backup) + ": ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(preview.backupDate))}", fontSize = 13.sp)
                    Text("• " + stringResource(R.string.device_info_line, preview.deviceModel), fontSize = 13.sp)
                    Text("• " + stringResource(R.string.app_version_line, preview.appVersion), fontSize = 13.sp)
                    Text("• " + stringResource(R.string.records_line, preview.recordCounts.map { "${it.key}: ${it.value}" }.joinToString(", ")), fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isOperating = true
                        val file = restoreFileToApply
                        restorePreview = null
                        scope.launch {
                            try {
                                if (file != null) {
                                    when (val result = restoreManager.performRestoreDetailed(file, false, progressTracker)) {
                                        is com.synfusion.attendance.backup.RestoreResult.Success -> {
                                            operationSuccess = true
                                            restoreManager.triggerCleanRestart()
                                        }
                                        is com.synfusion.attendance.backup.RestoreResult.Error -> {
                                            errorMessage = result.userMessage
                                            operationSuccess = false
                                            isOperating = false
                                        }
                                    }
                                    file.delete()
                                } else {
                                    errorMessage = context.getString(R.string.internal_file_error)
                                    operationSuccess = false
                                    isOperating = false
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: context.getString(R.string.restore_failed_msg)
                                operationSuccess = false
                                isOperating = false
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.restore_now))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        restorePreview = null
                        restoreFileToApply?.delete()
                        restoreFileToApply = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSmartBackupDialog && detectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showSmartBackupDialog = false },
            title = { Text(stringResource(R.string.backup_found_dialog), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.backup_found_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSmartBackupDialog = false
                        val file = detectedBackupFile
                        if (file != null) {
                            isOperating = true
                            scope.launch {
                                try {
                                    when (val result = restoreManager.performRestoreDetailed(file, false, progressTracker)) {
                                        is com.synfusion.attendance.backup.RestoreResult.Success -> {
                                            operationSuccess = true
                                            restoreManager.triggerCleanRestart()
                                        }
                                        is com.synfusion.attendance.backup.RestoreResult.Error -> {
                                            errorMessage = result.userMessage
                                            operationSuccess = false
                                            isOperating = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: context.getString(R.string.restore_failed_msg)
                                    operationSuccess = false
                                    isOperating = false
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.restore_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartBackupDialog = false }) {
                    Text(stringResource(R.string.skip))
                }
            }
        )
    }

    val conflictState = accountResolutionState as? AccountResolutionState.WorkspaceConflict
    if (conflictState != null) {
        AlertDialog(
            onDismissRequest = {
                authViewModel.resetAccountResolutionState()
                onlineAuthViewModel.signOut()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.account_mismatch_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(conflictState.message)
            },
            confirmButton = {
                if (conflictState.canSwitchToAccountWorkspace) {
                    Button(onClick = {
                        authViewModel.switchFromUnlinkedToRegisteredAccount(conflictState.account)
                    }) {
                        Text("Open Account")
                    }
                } else {
                    Button(onClick = {
                        authViewModel.resetAccountResolutionState()
                        onlineAuthViewModel.signOut()
                    }) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (conflictState.canSwitchToAccountWorkspace) {
                        TextButton(onClick = {
                            authViewModel.resetAccountResolutionState()
                            onlineAuthViewModel.signOut()
                        }) {
                            Text(stringResource(R.string.try_again))
                        }
                    }
                    if (existingUser != null) {
                        TextButton(onClick = {
                            authViewModel.resetAccountResolutionState()
                            onlineAuthViewModel.signOut()
                            authViewModel.resumeExistingWorkspace()
                        }) {
                            Text(stringResource(R.string.continue_offline))
                        }
                    }
                }
            }
        )
    }

    if (isOperating) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.restoring_backup), fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CircularProgressIndicator(progress = { progress / 100f }, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("$status (${progress.toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            },
            confirmButton = {}
        )
    }

    if (operationSuccess == false) {
        AlertDialog(
            onDismissRequest = { operationSuccess = null },
            title = { Text(stringResource(R.string.restore_failed), fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: stringResource(R.string.unexpected_error_occurred)) },
            confirmButton = {
                Button(onClick = { operationSuccess = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Notification Permission Handling
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isPostPermissionsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showOpenSettingsButton by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPostPermissionsGranted = isGranted
        if (isGranted) {
            showPermissionDialog = false
            onLoginSuccess()
        } else {
            val activity = context.getActivity()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
                val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
                if (!shouldShowRationale) {
                    showOpenSettingsButton = true
                } else {
                    showPermissionDialog = false
                    onLoginSuccess()
                }
            } else {
                showPermissionDialog = false
                onLoginSuccess()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isPostPermissionsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    isPostPermissionsGranted = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is OfflineLoginState.Success) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPostPermissionsGranted) {
                showPermissionDialog = true
            } else {
                onLoginSuccess()
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                onLoginSuccess()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.allow_notifications), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(stringResource(R.string.login_notif_desc))
            },
            confirmButton = {
                if (showOpenSettingsButton) {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.getActivity()?.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.open_notif_settings))
                    }
                } else {
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) {
                        Text(stringResource(R.string.allow_notifications))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onLoginSuccess()
                }) {
                    Text(stringResource(R.string.continue_word))
                }
            }
        )
    }

    val parsedWage = dailyWageText.toDoubleOrNull()
    val isWageValid = parsedWage != null && parsedWage > 0.0
    val isPhoneValid = if (selectedCountry.countryCode == "IN") phone.length == 10 else phone.length in 7..15
    val isUsernameValidForSubmit = usernameValidationReason == null &&
        usernameAvailability !is com.synfusion.attendance.online.UsernameAvailabilityResult.AlreadyTaken &&
        !isCheckingUsername
    val isProcessing = loginState is OfflineLoginState.Loading ||
        onlineAuthState is OnlineAuthState.Loading ||
        accountResolutionState is AccountResolutionState.Resolving

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ResponsiveContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // TOP BANNER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.login_banner),
                        contentDescription = "WorkDaily Banner",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // OVERLAY FORM SURFACE
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    ),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (authenticatedAccount == null) {
                            // --- VIEW 1: ACCOUNT ENTRY (GOOGLE FIRST) ---
                            Text(
                                text = stringResource(R.string.workdaily_account),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.manage_your_daily_labor_earnings),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Error states display
                            when (val state = onlineAuthState) {
                                is OnlineAuthState.Error -> {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                            .padding(vertical = 10.dp, horizontal = 14.dp)
                                    )
                                }
                                is OnlineAuthState.NetworkUnavailable -> {
                                    Text(
                                        text = stringResource(R.string.network_unavailable_msg),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                            .padding(vertical = 10.dp, horizontal = 14.dp)
                                    )
                                }
                                else -> {}
                            }

                            if (accountResolutionState is AccountResolutionState.Error) {
                                val resError = (accountResolutionState as AccountResolutionState.Error).message
                                Text(
                                    text = resError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                        .padding(vertical = 10.dp, horizontal = 14.dp)
                                )
                            }

                            // TERMS / PRIVACY CONSENT ROW
                            LegalConsentRow(
                                isConsentChecked = isConsentChecked,
                                onConsentChange = { isConsentChecked = it },
                                onTermsClick = { showTermsSheet = true },
                                onPrivacyClick = { showPrivacySheet = true }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // MAIN CTA: CONTINUE WITH GOOGLE
                            Button(
                                onClick = {
                                    if (isConsentChecked && !isProcessing) {
                                        authViewModel.resetAccountResolutionState()
                                        scope.launch {
                                            if (onlineAuthState is OnlineAuthState.Authenticated) {
                                                onlineAuthViewModel.signOut().join()
                                            }
                                            onlineAuthViewModel.signInWithGoogle(context, webClientId)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .pressScaleEffect(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = isConsentChecked && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_google_logo),
                                            contentDescription = "Google G Logo",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            stringResource(R.string.continue_with_google),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // OFFLINE CONTINUATION BUTTON FOR EXISTING WORKSPACE
                            if (existingUser != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        authViewModel.resumeExistingWorkspace()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WifiOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.continue_offline),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // TRUST / OFFLINE INFO ROW (3-COLUMN EQUAL WEIGHT)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrustColumnItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Shield,
                                    title = stringResource(R.string.trust_offline_private)
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                TrustColumnItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.PhoneAndroid,
                                    title = stringResource(R.string.trust_data_on_device)
                                )
                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                TrustColumnItem(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Engineering,
                                    title = stringResource(R.string.trust_for_daily_workers)
                                )
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(0.6f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // RESTORE BACKUP SECTION
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.already_have_backup),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { pickRestoreLauncher.launch(arrayOf("application/zip")) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.restore_backup),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // --- VIEW 2: COMPLETE PROFILE ---
                            val account = authenticatedAccount!!

                            Text(
                                text = stringResource(R.string.complete_your_profile),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // GOOGLE IDENTITY SUMMARY CARD
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!account.photoUrl.isNullOrBlank()) {
                                            SubcomposeAsyncImage(
                                                model = account.photoUrl,
                                                contentDescription = account.displayName ?: "Google Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.displayName ?: name.ifBlank { "WorkDaily User" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!account.email.isNullOrBlank()) {
                                            Text(
                                                text = account.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.signed_in_as, "Google"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (loginState is OfflineLoginState.Error) {
                                Text(
                                    text = (loginState as OfflineLoginState.Error).message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                        .padding(vertical = 10.dp, horizontal = 14.dp)
                                )
                            }

                            // FULL NAME FIELD
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.full_name)) },
                                placeholder = { Text(stringResource(R.string.enter_full_name_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // UNIQUE USERNAME FIELD
                            val isUsernameTaken = usernameAvailability is com.synfusion.attendance.online.UsernameAvailabilityResult.AlreadyTaken
                            val isUsernameAvailable = usernameAvailability is com.synfusion.attendance.online.UsernameAvailabilityResult.Available
                            val isUsernameInvalid = usernameValidationReason != null

                            OutlinedTextField(
                                value = usernameText,
                                onValueChange = { newValue ->
                                    val cleaned = newValue.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
                                    usernameText = cleaned
                                },
                                label = { Text("Unique Username") },
                                placeholder = { Text("e.g. rahul_builder") },
                                isError = isUsernameTaken || isUsernameInvalid,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Text(
                                        text = "@",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(start = 14.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (isCheckingUsername) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else if (isUsernameAvailable) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = com.synfusion.attendance.live.ui.components.LivePrimaryEmerald)
                                    } else if (isUsernameTaken || isUsernameInvalid) {
                                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isUsernameAvailable) com.synfusion.attendance.live.ui.components.LivePrimaryEmerald else MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            // Username Status & Suggestion Messages
                            if (usernameValidationReason != null) {
                                Text(
                                    text = usernameValidationReason!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp)
                                )
                            } else if (isUsernameTaken) {
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp)) {
                                    Text(
                                        text = "Username @$usernameText is already taken",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (suggestedUsername != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Suggested:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Surface(
                                                modifier = Modifier.clickable { usernameText = suggestedUsername!! },
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "@${suggestedUsername}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (isUsernameAvailable) {
                                Text(
                                    text = "Username @$usernameText is available",
                                    color = com.synfusion.attendance.live.ui.components.LivePrimaryEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // GENDER SELECTION (3 SELECTABLE CARDS: Male, Female, Transgender)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Gender",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Male", "Female", "Transgender").forEach { genderOption ->
                                        val isSelected = selectedGender == genderOption
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedGender = genderOption },
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = genderOption,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // PROFESSION SELECTION FIELD
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedProfession,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Profession") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showProfessionSheet = true },
                                    shape = RoundedCornerShape(16.dp),
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showProfessionSheet = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ABOUT / BIO FIELD
                            OutlinedTextField(
                                value = bioText,
                                onValueChange = { if (it.length <= 150) bioText = it },
                                label = { Text("About / Bio") },
                                placeholder = { Text("Brief intro about your work experience...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // COUNTRY (read-only)
                            //
                            // No longer a control. The dropdown chevron and the tap targets are
                            // gone because there is nothing to choose: SUPPORTED_COUNTRIES holds a
                            // single configuration, so the sheet could only ever re-select the
                            // country already in use. The field stays as a read-only statement of
                            // the resolved country and its currency, and the countryCode /
                            // currencyCode values it displays are still persisted and sent to the
                            // backend exactly as before -- only the ability to change them by hand
                            // is removed.
                            OutlinedTextField(
                                value = "${selectedCountry.flagEmoji} ${selectedCountry.displayName} — ${selectedCountry.currencySymbol}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.country_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // DAILY WAGE FIELD
                            OutlinedTextField(
                                value = dailyWageText,
                                onValueChange = { newValue ->
                                    val digits = newValue.filter { it.isDigit() }.take(7)
                                    dailyWageText = digits
                                },
                                label = { Text(stringResource(R.string.daily_wage)) },
                                placeholder = { Text("500") },
                                isError = dailyWageText.isNotBlank() && !isWageValid,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Text(
                                        text = selectedCountry.currencySymbol,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (dailyWageText.isNotBlank() && !isWageValid) {
                                Text(
                                    text = stringResource(R.string.err_daily_wage_greater_than_zero),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // PHONE FIELD
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { newValue ->
                                    val digits = newValue.filter { it.isDigit() }.take(15)
                                    phone = digits
                                },
                                label = { Text(stringResource(R.string.phone_number)) },
                                placeholder = { Text(stringResource(R.string.enter_mobile_number_hint)) },
                                isError = phone.isNotBlank() && !isPhoneValid,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedCountry.callingCode,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "|",
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            if (phone.isNotBlank() && !isPhoneValid) {
                                val phoneErrMsg = if (selectedCountry.countryCode == "IN") {
                                    stringResource(R.string.err_valid_10_digit_mobile)
                                } else {
                                    stringResource(R.string.err_valid_phone_digits)
                                }
                                Text(
                                    text = phoneErrMsg,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // SUBMIT / COMPLETE SETUP BUTTON
                            Button(
                                onClick = {
                                    if (isWageValid && isPhoneValid && isUsernameValidForSubmit) {
                                        authViewModel.completeProfile(
                                            googleAccount = account,
                                            enteredName = name,
                                            enteredUsername = usernameText,
                                            enteredPhone = phone,
                                            countryCode = selectedCountry.countryCode,
                                            currencyCode = selectedCountry.currencyCode,
                                            dailyWage = parsedWage!!,
                                            gender = selectedGender,
                                            profession = selectedProfession,
                                            bio = bioText,
                                            onComplete = { _, _ -> }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .pressScaleEffect(),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = isWageValid && isPhoneValid && isUsernameValidForSubmit && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            stringResource(R.string.setup_workspace),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        authViewModel.resetAccountResolutionState()
                                        onlineAuthViewModel.signOut().join()
                                        authenticatedAccount = null
                                    }
                                },
                                enabled = !isProcessing
                            ) {
                                Text(
                                    text = "Use a different Google account",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showTermsSheet) {
            TermsConditionsBottomSheet(onDismiss = { showTermsSheet = false })
        }
        if (showPrivacySheet) {
            PrivacyPolicyBottomSheet(onDismiss = { showPrivacySheet = false })
        }

        if (showProfessionSheet) {
            ProfessionSelectionBottomSheet(
                selectedProfession = selectedProfession,
                onProfessionSelected = { profession ->
                    selectedProfession = profession
                    showProfessionSheet = false
                },
                onDismiss = { showProfessionSheet = false }
            )
        }
    }
}

@Composable
fun ProfessionSelectionBottomSheet(
    selectedProfession: String,
    onProfessionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customProfession by remember { mutableStateOf("") }

    val defaultProfessions = listOf(
        "Construction Worker", "Mason", "Electrician", "Plumber",
        "Carpenter", "Painter", "Welder", "Helper", "Supervisor",
        "Site Engineer", "Contractor", "Driver", "Technician", "Other"
    )

    val filtered = defaultProfessions.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        windowInsets = WindowInsets(0.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Profession",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search profession...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.forEach { profession ->
                    val isSelected = profession == selectedProfession
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (profession != "Other") {
                                    onProfessionSelected(profession)
                                }
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profession,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (filtered.contains("Other")) {
                OutlinedTextField(
                    value = customProfession,
                    onValueChange = { customProfession = it },
                    label = { Text("Custom Profession (Other)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        if (customProfession.isNotBlank()) {
                            IconButton(onClick = { onProfessionSelected(customProfession.trim()) }) {
                                Icon(Icons.Default.Check, contentDescription = "Apply Custom Profession")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrustColumnItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2
        )
    }
}

@Composable
fun LegalConsentRow(
    isConsentChecked: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val fullConsentText = stringResource(R.string.legal_consent_full)
    val termsString = stringResource(R.string.terms_of_use_clickable)
    val privacyString = stringResource(R.string.privacy_policy_clickable)

    val termsStart = fullConsentText.indexOf(termsString)
    val termsEnd = if (termsStart != -1) termsStart + termsString.length else -1

    val privacyStart = fullConsentText.indexOf(privacyString)
    val privacyEnd = if (privacyStart != -1) privacyStart + privacyString.length else -1

    val annotatedString = buildAnnotatedString {
        append(fullConsentText)
        if (termsStart != -1) {
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                start = termsStart,
                end = termsEnd
            )
            addStringAnnotation(
                tag = "URL",
                annotation = "terms",
                start = termsStart,
                end = termsEnd
            )
        }
        if (privacyStart != -1) {
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                ),
                start = privacyStart,
                end = privacyEnd
            )
            addStringAnnotation(
                tag = "URL",
                annotation = "privacy",
                start = privacyStart,
                end = privacyEnd
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isConsentChecked,
            onCheckedChange = onConsentChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        if (annotation.item == "terms") {
                            onTermsClick()
                        } else if (annotation.item == "privacy") {
                            onPrivacyClick()
                        }
                    }
            }
        )
    }
}
