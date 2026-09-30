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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.synfusion.attendance.R
import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.backup.ProgressTracker
import com.synfusion.attendance.backup.RestoreManager
import com.synfusion.attendance.data.model.CountryConfig
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.getActivity
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
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
    var dailyWageText by remember { mutableStateOf("500") }

    var selectedCountry by remember { mutableStateOf(CountryRepository.DEFAULT_COUNTRY) }
    var showCountryBottomSheet by remember { mutableStateOf(false) }

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

    val startText = stringResource(R.string.create_offline_workspace)

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

    val isPhoneValid = if (selectedCountry.countryCode == "IN") phone.length >= 10 else phone.length >= 7

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                // 1. TOP BANNER CONTAINER
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.login_banner),
                        contentDescription = "WorkDaily Banner",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. OVERLAY FORM CONTAINER / SHEET
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-24).dp),
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

                        // 3. FULL NAME FIELD
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

                        // 4. COUNTRY FIELD
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = "${selectedCountry.flagEmoji} ${selectedCountry.displayName} — ${selectedCountry.currencySymbol}",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.country_label)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCountryBottomSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Public,
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
                                    .clickable { showCountryBottomSheet = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. DAILY WAGE FIELD
                        OutlinedTextField(
                            value = dailyWageText,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }.take(7)
                                dailyWageText = digits
                            },
                            label = { Text(stringResource(R.string.daily_wage)) },
                            placeholder = { Text("500") },
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // 6. PHONE FIELD
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }.take(15)
                                phone = digits
                            },
                            label = { Text(stringResource(R.string.phone_number)) },
                            placeholder = { Text(stringResource(R.string.enter_mobile_number_hint)) },
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. TERMS / PRIVACY CONSENT ROW
                        LegalConsentRow(
                            isConsentChecked = isConsentChecked,
                            onConsentChange = { isConsentChecked = it },
                            onTermsClick = { showTermsSheet = true },
                            onPrivacyClick = { showPrivacySheet = true }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 7. PRIMARY ACTION BUTTON
                        Button(
                            onClick = {
                                val dailyWageDouble = dailyWageText.toDoubleOrNull() ?: 500.0
                                if (name.isNotBlank() && dailyWageText.isNotBlank() && isPhoneValid && isConsentChecked) {
                                    authViewModel.login(
                                        name = name,
                                        email = email,
                                        phoneNumber = phone,
                                        countryCode = selectedCountry.countryCode,
                                        currencyCode = selectedCountry.currencyCode,
                                        dailyWage = dailyWageDouble
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
                            enabled = name.isNotBlank() && dailyWageText.isNotBlank() && isPhoneValid && isConsentChecked && !isProcessing
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
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        startText,
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

                        Spacer(modifier = Modifier.height(22.dp))

                        // 8. TRUST / OFFLINE INFO ROW (3-COLUMN EQUAL WEIGHT)
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

                        // 9. RESTORE BACKUP SECTION
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

        if (showCountryBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCountryBottomSheet = false },
                windowInsets = WindowInsets(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_select_country_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        items(CountryRepository.SUPPORTED_COUNTRIES) { country ->
                            val isSelected = country.countryCode == selectedCountry.countryCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                                    .clickable {
                                        selectedCountry = country
                                        showCountryBottomSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = country.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = country.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${country.currencyCode} (${country.currencySymbol}) • ${country.callingCode}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
