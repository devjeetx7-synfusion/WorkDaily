package com.synfusion.attendance.ui
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.content.Intent
import android.provider.Settings
import com.synfusion.attendance.utils.getActivity


import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.synfusion.attendance.backup.RestoreManager
import com.synfusion.attendance.backup.ProgressTracker
import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.data.model.CountryConfig
import com.synfusion.attendance.data.model.CountryRepository
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation

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
    selectedLanguage: String,
    onLoginSuccess: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    val isProcessing = loginState is OfflineLoginState.Loading
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var selectedCountry by remember { mutableStateOf(CountryRepository.DEFAULT_COUNTRY) }
    var showCountryDropdown by remember { mutableStateOf(false) }

    var isConsentChecked by rememberSaveable { mutableStateOf(false) }
    var showTermsSheet by rememberSaveable { mutableStateOf(false) }
    var showPrivacySheet by rememberSaveable { mutableStateOf(false) }

    var detectedBackupFile by remember { mutableStateOf<File?>(null) }
    var showSmartBackupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
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

    // File Pick Launcher for SAF Restore
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
                            Toast.makeText(context, context.getString(com.synfusion.attendance.R.string.restore_invalid_format_msg), Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(com.synfusion.attendance.R.string.restore_read_failed_msg), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, context.getString(com.synfusion.attendance.R.string.restore_cancelled_msg), Toast.LENGTH_SHORT).show()
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
            title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.prev_backup_preview), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_parsed_msg))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.last_backup) + ": ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(preview.backupDate))}", fontSize = 13.sp)
                    Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.device_info_line, preview.deviceModel), fontSize = 13.sp)
                    Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.app_version_line, preview.appVersion), fontSize = 13.sp)
                    Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.records_line, preview.recordCounts.map { "${it.key}: ${it.value}" }.joinToString(", ")), fontSize = 13.sp)
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
                                    errorMessage = context.getString(com.synfusion.attendance.R.string.internal_file_error)
                                    operationSuccess = false
                                    isOperating = false
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: context.getString(com.synfusion.attendance.R.string.restore_failed_msg)
                                operationSuccess = false
                                isOperating = false
                            }
                        }
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.restore_now))
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
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel))
                }
            }
        )
    }

    if (showSmartBackupDialog && detectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showSmartBackupDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_found_dialog), fontWeight = FontWeight.Bold) },
            text = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_found_msg)) },
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
                                    errorMessage = e.localizedMessage ?: context.getString(com.synfusion.attendance.R.string.restore_failed_msg)
                                    operationSuccess = false
                                    isOperating = false
                                }
                            }
                        }
                    }
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.restore_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartBackupDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.skip))
                }
            }
        )
    }

    if (isOperating) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.restoring_backup), fontWeight = FontWeight.Bold) },
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
            title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.restore_failed), fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.unexpected_error_occurred)) },
            confirmButton = {
                Button(onClick = { operationSuccess = null }) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.close))
                }
            }
        )
    }

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

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
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
                    // Permanently denied
                    showOpenSettingsButton = true
                } else {
                    showPermissionDialog = false
                    onLoginSuccess() // Denied but not permanently, just continue
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

    val startText = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.create_offline_workspace)

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
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.allow_notifications), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.login_notif_desc))
            },
            confirmButton = {
                if (showOpenSettingsButton) {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.getActivity()?.startActivity(intent)
                    }) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.open_notif_settings))
                    }
                } else {
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.allow_notifications))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    onLoginSuccess()
                }) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.continue_word))
                }
            }
        )
    }

    val windowSizeInfo = LocalWindowSizeInfo.current
    val isPhoneValid = if (selectedCountry.countryCode == "IN") phone.length >= 10 else phone.length >= 7

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ResponsiveContainer {
            if (windowSizeInfo.isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // CENTER AREA: App Branding
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "WorkDaily",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Welcome back",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // BOTTOM AREA: Local Workspace Setup
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (loginState is OfflineLoginState.Error) {
                            Text(
                                text = (loginState as OfflineLoginState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }

                        // Local Input Fields
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_name_1)) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Country Selector Field
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = "${selectedCountry.flagEmoji} ${selectedCountry.displayName} — ${selectedCountry.currencySymbol}",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCountryDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
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
                                    .clickable { showCountryDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showCountryDropdown,
                                onDismissRequest = { showCountryDropdown = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                CountryRepository.SUPPORTED_COUNTRIES.forEach { country ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${country.flagEmoji} ${country.displayName} — ${country.currencySymbol}", fontSize = 14.sp)
                                        },
                                        onClick = {
                                            selectedCountry = country
                                            showCountryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }.take(15)
                                phone = digits
                            },
                            placeholder = {
                                Text(
                                    if (selectedCountry.countryCode == "IN")
                                        androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.enter_valid_10digit_mobile_number)
                                    else
                                        "Enter mobile number"
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Text(
                                    text = "${selectedCountry.callingCode} ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Mandatory Legal Consent Row
                        LegalConsentRow(
                            isConsentChecked = isConsentChecked,
                            onConsentChange = { isConsentChecked = it },
                            onTermsClick = { showTermsSheet = true },
                            onPrivacyClick = { showPrivacySheet = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (name.isNotBlank() && isPhoneValid && isConsentChecked) {
                                    authViewModel.login(name, email, phone, selectedCountry.countryCode, selectedCountry.currencyCode)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .pressScaleEffect(),
                            shape = RoundedCornerShape(20.dp),
                            enabled = name.isNotBlank() && isPhoneValid && isConsentChecked && !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(startText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.private_offline_footer),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(modifier = Modifier.width(180.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.already_have_backup),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.restore_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { pickRestoreLauncher.launch(arrayOf("application/zip")) }
                                .padding(8.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "WorkDaily",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Welcome back",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (loginState is OfflineLoginState.Error) {
                            Text(
                                text = (loginState as OfflineLoginState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_name_1)) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Country Selector Field
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = "${selectedCountry.flagEmoji} ${selectedCountry.displayName} — ${selectedCountry.currencySymbol}",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCountryDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
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
                                    .clickable { showCountryDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showCountryDropdown,
                                onDismissRequest = { showCountryDropdown = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                CountryRepository.SUPPORTED_COUNTRIES.forEach { country ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${country.flagEmoji} ${country.displayName} — ${country.currencySymbol}", fontSize = 14.sp)
                                        },
                                        onClick = {
                                            selectedCountry = country
                                            showCountryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }.take(15)
                                phone = digits
                            },
                            placeholder = {
                                Text(
                                    if (selectedCountry.countryCode == "IN")
                                        androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.enter_valid_10digit_mobile_number)
                                    else
                                        "Enter mobile number"
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Text(
                                    text = "${selectedCountry.callingCode} ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Mandatory Legal Consent Row
                        LegalConsentRow(
                            isConsentChecked = isConsentChecked,
                            onConsentChange = { isConsentChecked = it },
                            onTermsClick = { showTermsSheet = true },
                            onPrivacyClick = { showPrivacySheet = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (name.isNotBlank() && isPhoneValid && isConsentChecked) {
                                    authViewModel.login(name, email, phone, selectedCountry.countryCode, selectedCountry.currencyCode)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .pressScaleEffect(),
                            shape = RoundedCornerShape(20.dp),
                            enabled = name.isNotBlank() && isPhoneValid && isConsentChecked && !isProcessing
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(startText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.private_offline_footer),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(modifier = Modifier.width(180.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.already_have_backup),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.restore_backup),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { pickRestoreLauncher.launch(arrayOf("application/zip")) }
                                .padding(8.dp)
                        )
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
    }
}

@Composable
fun LegalConsentRow(
    isConsentChecked: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val fullConsentText = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.legal_consent_full)
    val termsString = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.terms_of_use_clickable)
    val privacyString = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.privacy_policy_clickable)

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

