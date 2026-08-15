package com.synfusion.attendance.ui

import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.synfusion.attendance.backup.BackupManager
import com.synfusion.attendance.backup.ProgressTracker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ActivityNotFoundException
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

import com.synfusion.attendance.ui.components.InstagramIcon
import com.synfusion.attendance.ui.components.TelegramIcon
import com.synfusion.attendance.ui.components.YouTubeIcon
import com.synfusion.attendance.ui.components.CropScreenDialog

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.synfusion.attendance.utils.getActivity
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.IndianCurrencyVisualTransformation
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing

enum class SettingsCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PROFILE("Profile Settings", Icons.Default.Person),
    STAY_CONNECTED("Stay Connected", Icons.Default.Favorite),
    PREFERENCES("App Preferences", Icons.Default.Settings),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    BACKUP("Backup & Restore", Icons.Default.Backup),
    INFO_SUPPORT("Info & Support", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: OfflineSettingsViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }


    var isAppNotificationsEnabled by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var isPostPermissionsGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var isChannelEnabled by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channel = manager.getNotificationChannel(com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE)
                channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
            } else {
                true
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPostPermissionsGranted = isGranted
        val systemEnabled = isAppNotificationsEnabled && isGranted && isChannelEnabled

        if (!isGranted) {
            Toast.makeText(context, context.getString(com.synfusion.attendance.R.string.notif_disabled_desc), Toast.LENGTH_LONG).show()
        }

        if (state.isRemindersEnabled != systemEnabled) {
            viewModel.toggleReminders(systemEnabled)
        }

        if (systemEnabled) {
            com.synfusion.attendance.utils.NotificationScheduler.schedulePeriodicWork(context)
        } else {
            com.synfusion.attendance.utils.NotificationScheduler.cancelAllScheduledWork(context)
        }
    }



    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val actualAppNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                val actualPostPermissionsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                val actualChannelEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val channel = manager.getNotificationChannel(com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE)
                    channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
                } else {
                    true
                }

                isAppNotificationsEnabled = actualAppNotificationsEnabled
                isPostPermissionsGranted = actualPostPermissionsGranted
                isChannelEnabled = actualChannelEnabled

                val systemEnabled = actualAppNotificationsEnabled && actualPostPermissionsGranted && actualChannelEnabled
                if (state.isRemindersEnabled != systemEnabled) {
                    viewModel.toggleReminders(systemEnabled)
                    if (systemEnabled) {
                        com.synfusion.attendance.utils.NotificationScheduler.schedulePeriodicWork(context)
                    } else {
                        com.synfusion.attendance.utils.NotificationScheduler.cancelAllScheduledWork(context)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    var showDeveloperSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    LaunchedEffect(state.triggerRestart) {
        if (state.triggerRestart) {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            val activity = context.getActivity()
            if (activity != null) {
                activity.startActivity(intent)
                activity.finish()
            } else {
                context.startActivity(intent)
            }
        }
    }
    var showLogoutBottomSheet by remember { mutableStateOf(false) }
    var showPreDeleteReminderDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    var isBackingUpForLogout by remember { mutableStateOf(false) }
    val backupProgressTracker = remember { ProgressTracker() }
    val backupProgress by backupProgressTracker.progress.collectAsState(initial = 0f)
    val backupStatus by backupProgressTracker.status.collectAsState(initial = "")
    var backupSuccessDialogDetails by remember { mutableStateOf<File?>(null) }

    var lastBackupFile by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dir = context.getExternalFilesDir(null)
            if (dir != null && dir.exists()) {
                val files = dir.listFiles { _, name -> name.startsWith("AttendanceBackup_") && name.endsWith(".zip") }
                if (!files.isNullOrEmpty()) {
                    lastBackupFile = files.maxByOrNull { it.lastModified() }
                }
            }
        }
    }

    var showProfileEditSheet by remember { mutableStateOf(false) }

    val supportedLanguages = mapOf("en" to "English", "hi" to "हिंदी")

    val hasChanges = state.name != state.originalName ||
            state.dailyWage != state.originalWage ||
            state.role != state.originalRole ||
            state.phone != state.originalPhone ||
            state.profileImageUri != null

    val windowSizeInfo = LocalWindowSizeInfo.current
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.PROFILE) }

    val actualNotificationState = state.isRemindersEnabled && isAppNotificationsEnabled && isPostPermissionsGranted && isChannelEnabled

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        ResponsiveContainer {
            if (windowSizeInfo.isCompact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.app_preferences), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.settings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }

                    ProfileCardSection(state = state, viewModel = viewModel, hasChanges = hasChanges, onEditPhoto = { showProfileEditSheet = true })
                    StayConnectedCardSection(context = context)
                    AppPreferencesCardSection(state = state, viewModel = viewModel, supportedLanguages = supportedLanguages, context = context)
                    NotificationSettingsCardSection(
                        state = state,
                        viewModel = viewModel,
                        actualNotificationState = actualNotificationState,
                        isAppNotificationsEnabled = isAppNotificationsEnabled,
                        isPostPermissionsGranted = isPostPermissionsGranted,
                        isChannelEnabled = isChannelEnabled,
                        permissionLauncher = permissionLauncher,
                        context = context
                    )
                    BackupRestoreCardSection(state = state, lastBackupFile = lastBackupFile, context = context, onManageBackup = { showBackupSheet = true })
                    InfoSupportCardSection(onNavigate = onNavigate, onDeveloperContact = { showDeveloperSheet = true })

                    Button(
                        onClick = { showPreDeleteReminderDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.1f), contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Adaptive Split-Pane View for Medium & Expanded Windows / Tablets
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Pane: Navigation List
                    Card(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.settings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text(state.name.ifEmpty { "User Profile" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                val categoryListState = rememberScrollState()
                                val categoryRequesters = remember { SettingsCategory.values().map { androidx.compose.foundation.relocation.BringIntoViewRequester() } }

                                LaunchedEffect(selectedCategory) {
                                    val index = SettingsCategory.values().indexOf(selectedCategory)
                                    if (index >= 0) {
                                        categoryRequesters[index].bringIntoView()
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(categoryListState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SettingsCategory.values().forEachIndexed { index, category ->
                                        val isSelected = selectedCategory == category
                                        val requester = categoryRequesters[index]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bringIntoViewRequester(requester)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                .clickable { selectedCategory = category }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = category.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = category.title,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showPreDeleteReminderDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.1f), contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Log Out", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Right Pane: Detail View for Selected Category
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Text(
                                text = selectedCategory.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            when (selectedCategory) {
                                SettingsCategory.PROFILE -> {
                                    ProfileCardSection(state = state, viewModel = viewModel, hasChanges = hasChanges, onEditPhoto = { showProfileEditSheet = true })
                                }
                                SettingsCategory.STAY_CONNECTED -> {
                                    StayConnectedCardSection(context = context)
                                }
                                SettingsCategory.PREFERENCES -> {
                                    AppPreferencesCardSection(state = state, viewModel = viewModel, supportedLanguages = supportedLanguages, context = context)
                                }
                                SettingsCategory.NOTIFICATIONS -> {
                                    NotificationSettingsCardSection(
                                        state = state,
                                        viewModel = viewModel,
                                        actualNotificationState = actualNotificationState,
                                        isAppNotificationsEnabled = isAppNotificationsEnabled,
                                        isPostPermissionsGranted = isPostPermissionsGranted,
                                        isChannelEnabled = isChannelEnabled,
                                        permissionLauncher = permissionLauncher,
                                        context = context
                                    )
                                }
                                SettingsCategory.BACKUP -> {
                                    BackupRestoreCardSection(state = state, lastBackupFile = lastBackupFile, context = context, onManageBackup = { showBackupSheet = true })
                                }
                                SettingsCategory.INFO_SUPPORT -> {
                                    InfoSupportCardSection(onNavigate = onNavigate, onDeveloperContact = { showDeveloperSheet = true })
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showProfileEditSheet) {
            var wasSaved by remember { mutableStateOf(false) }
            OfflineProfileEditBottomSheet(
                state = state,
                viewModel = viewModel,
                onDismiss = {
                    if (!wasSaved) {
                        viewModel.discardChanges()
                    }
                    showProfileEditSheet = false
                },
                onSave = {
                    wasSaved = true
                    viewModel.saveChanges()
                }
            )
        }



        if (showDeveloperSheet) {
            DeveloperContactBottomSheet(onDismiss = { showDeveloperSheet = false })
        }
        if (showBackupSheet) {
            com.synfusion.attendance.ui.components.BackupRestoreBottomSheet(onDismiss = { showBackupSheet = false })
        }
        if (showPreDeleteReminderDialog) {
            AlertDialog(
                onDismissRequest = { showPreDeleteReminderDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.pre_delete_reminder), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.pre_delete_msg))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPreDeleteReminderDialog = false
                            isBackingUpForLogout = true
                            coroutineScope.launch {
                                try {
                                    val backupDir = context.getExternalFilesDir(null) ?: context.cacheDir
                                    val backupFile = File(backupDir, "AttendanceBackup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip")
                                    backupManager.performBackup(backupFile, backupProgressTracker)
                                    if (backupFile.exists() && backupFile.length() > 0) {
                                        val size = backupFile.length()
                                        val timestamp = System.currentTimeMillis()
                                        val prefsRepo = com.synfusion.attendance.data.OfflinePreferencesRepository(context)
                                        prefsRepo.saveBackupMetadata(timestamp, size, "Local Storage")
                                    }
                                    backupSuccessDialogDetails = backupFile
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isBackingUpForLogout = false
                                }
                            }
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_now))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showPreDeleteReminderDialog = false
                                showLogoutBottomSheet = true
                            }
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.continue_action), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = { showPreDeleteReminderDialog = false }
                        ) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        }
        if (showLogoutBottomSheet) {
            LogoutBottomSheet(
                onDismiss = { showLogoutBottomSheet = false },
                onBackupAndLogout = {
                    showLogoutBottomSheet = false
                    isBackingUpForLogout = true
                    coroutineScope.launch {
                        try {
                            val backupDir = context.getExternalFilesDir(null) ?: context.cacheDir
                            val backupFile = File(backupDir, "AttendanceBackup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip")
                            backupManager.performBackup(backupFile, backupProgressTracker)
                            if (backupFile.exists() && backupFile.length() > 0) {
                                val size = backupFile.length()
                                val timestamp = System.currentTimeMillis()
                                val prefsRepo = com.synfusion.attendance.data.OfflinePreferencesRepository(context)
                                prefsRepo.saveBackupMetadata(timestamp, size, "Local Storage")
                            }
                            backupSuccessDialogDetails = backupFile
                        } catch (e: Exception) {
                            Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            // Still logout or stay? Stay so they can try again.
                        } finally {
                            isBackingUpForLogout = false
                        }
                    }
                },
                onLogoutWithoutBackup = {
                    showLogoutBottomSheet = false
                    com.synfusion.attendance.utils.NotificationScheduler.cancelAllScheduledWork(context)
                    com.synfusion.attendance.utils.LocalNotificationManager.clearNotificationLogs(context)
                    viewModel.logout()
                    onLogout()
                }
            )
        }

        if (isBackingUpForLogout) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.creating_backup), fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        CircularProgressIndicator(progress = { backupProgress / 100f }, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("$backupStatus (${backupProgress.toInt()}%)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                confirmButton = {}
            )
        }

        if (backupSuccessDialogDetails != null) {
            val file = backupSuccessDialogDetails!!
            val fileSizeMb = String.format(Locale.getDefault(), "%.2f", file.length() / 1024.0 / 1024.0)
            val createdTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
            AlertDialog(
                onDismissRequest = {},
                title = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_success_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_success_details))
                        Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_size) + ": $fileSizeMb MB", fontSize = 13.sp)
                        Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.last_backup) + ": $createdTime", fontSize = 13.sp)
                        Text("• " + androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_location) + ": ${file.parentFile?.name ?: "Downloads"}", fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.getActivity()?.startActivity(Intent.createChooser(intent, "Share Backup ZIP"))
                        }
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.share))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            backupSuccessDialogDetails = null
                            com.synfusion.attendance.utils.NotificationScheduler.cancelAllScheduledWork(context)
                            com.synfusion.attendance.utils.LocalNotificationManager.clearNotificationLogs(context)
                            viewModel.logout()
                            onLogout()
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.done))
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileCardSection(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel,
    hasChanges: Boolean,
    onEditPhoto: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.profile_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                AnimatedVisibility(visible = hasChanges) {
                    IconButton(onClick = { viewModel.saveChanges() }, enabled = !state.isSaving) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save_changes), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                val imageFile = remember(state.profileImageUrl) {
                    if (state.profileImageUrl.isNotEmpty()) java.io.File(state.profileImageUrl) else null
                }
                val showLocalImage = imageFile != null && imageFile.exists()
                val imageRequest = remember(state.profileImageUrl, showLocalImage) {
                    if (imageFile != null && showLocalImage) {
                        coil.request.ImageRequest.Builder(context)
                            .data(imageFile)
                            .crossfade(true)
                            .memoryCacheKey("${state.profileImageUrl}_${imageFile.lastModified()}")
                            .diskCacheKey("${state.profileImageUrl}_${imageFile.lastModified()}")
                            .networkCachePolicy(coil.request.CachePolicy.DISABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                    } else null
                }

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLocalImage && imageRequest != null) {
                        coil.compose.SubcomposeAsyncImage(
                            model = imageRequest,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            error = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.profile_settings),
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
                        .clickable { onEditPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.edit_profile), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = state.name.ifEmpty { "User Name" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (state.phone.isNotEmpty()) {
                    Text(text = "+91 ${state.phone}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.dailyWage > 0) {
                    Text(text = "₹${AmountFormatter.format(state.dailyWage)} / day", fontSize = 14.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StayConnectedCardSection(context: Context) {
    val startStayConnectedAnimation = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startStayConnectedAnimation.value = true
    }
    val cardStayConnectedAlpha by animateFloatAsState(
        targetValue = if (startStayConnectedAnimation.value) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "stayConnectedAlpha"
    )
    val cardStayConnectedTranslationY by animateFloatAsState(
        targetValue = if (startStayConnectedAnimation.value) 0f else 15f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "stayConnectedTranslation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardStayConnectedAlpha
                translationY = cardStayConnectedTranslationY
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stay_connected),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        val isDark = isSystemInDarkTheme()
        val glassBgColor = if (isDark) Color(0xFF25D366).copy(alpha = 0.12f) else Color(0xFF25D366).copy(alpha = 0.08f)
        val glassBorderColor = if (isDark) Color(0xFF25D366).copy(alpha = 0.25f) else Color(0xFF25D366).copy(alpha = 0.15f)
        val iconContainerColor = if (isDark) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFF25D366).copy(alpha = 0.12f)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = glassBgColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, glassBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = com.synfusion.attendance.ui.components.WhatsAppIcon,
                        contentDescription = "WhatsApp Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.whatsapp_channel),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.follow_workdaily_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val buttonInteractionSource = remember { MutableInteractionSource() }
                val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "btnScale"
                )

                Button(
                    onClick = {
                        com.synfusion.attendance.ui.components.SafeExternalLinkHandler.openLink(
                            context,
                            "https://whatsapp.com/channel/0029Vb8bS4U23n3lJwvpbP3H"
                        )
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF11824A),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    interactionSource = buttonInteractionSource,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .defaultMinSize(minWidth = 80.dp, minHeight = 44.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.follow_channel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AppPreferencesCardSection(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel,
    supportedLanguages: Map<String, String>,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.app_preferences), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.theme), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Switch(checked = state.isDarkMode, onCheckedChange = { viewModel.toggleTheme(it) })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.enable_splash_screen), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Switch(checked = state.isSplashEnabled, onCheckedChange = { viewModel.toggleSplash(it) })
                }
                if (state.isLanguageEnabled && state.isLanguageActivityEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        val intent = android.content.Intent(context, LanguageSelectorActivity::class.java)
                        context.getActivity()?.startActivity(intent)
                    }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.language_2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(supportedLanguages[state.language] ?: "English", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsCardSection(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel,
    actualNotificationState: Boolean,
    isAppNotificationsEnabled: Boolean,
    isPostPermissionsGranted: Boolean,
    isChannelEnabled: Boolean,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.notification_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.reminders), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            val statusText = if (actualNotificationState) {
                                androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.notif_status_enabled)
                            } else {
                                androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.notif_status_disabled)
                            }
                            Text(statusText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = actualNotificationState,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!isAppNotificationsEnabled) {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.getActivity()?.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.getActivity()?.startActivity(fallbackIntent)
                                        } catch (ex: Exception) {
                                            Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !isPostPermissionsGranted) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else if (!isChannelEnabled) {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE)
                                            }
                                            context.getActivity()?.startActivity(intent)
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.getActivity()?.startActivity(intent)
                                        } catch (ex: Exception) {
                                            Toast.makeText(context, "Unable to open settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    viewModel.toggleReminders(true)
                                    com.synfusion.attendance.utils.NotificationScheduler.schedulePeriodicWork(context)
                                }
                            } else {
                                viewModel.toggleReminders(false)
                                com.synfusion.attendance.utils.NotificationScheduler.cancelAllScheduledWork(context)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BackupRestoreCardSection(
    state: OfflineSettingsState,
    lastBackupFile: File?,
    context: Context,
    onManageBackup: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_and_restore), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val lastDateStr = if (state.lastBackupTime != null && state.lastBackupTime!! > 0L) {
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(state.lastBackupTime!!))
                } else {
                    lastBackupFile?.let {
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it.lastModified()))
                    } ?: "Never"
                }

                val lastSizeStr = if (state.lastBackupSize != null && state.lastBackupSize!! > 0L) {
                    val sizeKb = state.lastBackupSize!! / 1024.0
                    if (sizeKb >= 1024.0) {
                        String.format(Locale.getDefault(), "%.2f MB", sizeKb / 1024.0)
                    } else {
                        String.format(Locale.getDefault(), "%.2f KB", sizeKb)
                    }
                } else {
                    lastBackupFile?.let {
                        val sizeMb = it.length() / 1024.0 / 1024.0
                        String.format(Locale.getDefault(), "%.2f MB", sizeMb)
                    } ?: "N/A"
                }

                val lastLocStr = state.lastBackupLoc ?: (lastBackupFile?.let { "External Files" } ?: "N/A")

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.last_backup) + ":", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastDateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_size) + ":", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastSizeStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_location) + ":", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastLocStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onManageBackup,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.manage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (lastBackupFile != null) {
                        OutlinedButton(
                            onClick = {
                                val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", lastBackupFile)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.getActivity()?.startActivity(Intent.createChooser(intent, "Share Backup"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.share), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoSupportCardSection(
    onNavigate: (String) -> Unit,
    onDeveloperContact: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.information_support), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate("how_to_use") }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.how_to_use_dailywork), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.quick_guide_tutorials), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate("privacy_policy") }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.privacy_policy), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.stored_safely_offline), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth().clickable { onNavigate("terms_conditions") }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.terms_conditions), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.rules_guidelines), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth().clickable { onDeveloperContact() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.developer_contact), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.reach_out_support), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoutBottomSheet(
    onDismiss: () -> Unit,
    onBackupAndLogout: () -> Unit,
    onLogoutWithoutBackup: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Logout",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Would you like to create a backup before logging out?",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onBackupAndLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_logout), fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onLogoutWithoutBackup,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.logout_without_backup), fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.privacy_policy), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "WorkDaily respects your privacy and is designed to help users manage work attendance, payments, notes, and reports securely.\n\nInformation We May Store:\n• Name, phone number, wage details, attendance records, payment records, advances, notes, and app preferences.\n\nHow We Use Information:\n• To show your attendance, payment details, passbooks, and reports inside the app.\n\nData Safety:\n• User data should not be sold.\n• Sensitive user records should only be used for app functionality.\n• Users are responsible for entering accurate payment/attendance information.\n\nBrand:\nSynFusion",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.close), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.terms_conditions), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "By using WorkDaily, you agree to use the app responsibly for managing attendance, wages, payments, notes, passbooks, and related work records.\n\nUser Responsibility:\n• Users are responsible for the accuracy of attendance, wage, payment, and due records entered in the app.\n• WorkDaily is a record-management tool and does not verify real-world payments or attendance automatically.\n\nBrand:\nSynFusion",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.close), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperContactBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.SubcomposeAsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data("https://i.postimg.cc/7PNN0DMg/file-000000006e788243a3aaf94a91b6f9db.png")
                        .crossfade(true)
                        .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "SynFusion Profile",
                    modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    loading = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Loading Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(24.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Error Loading Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SynFusion", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("@getsynfusion", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("workdaily.synfusion@gmail.com", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactIconItem(
                    icon = InstagramIcon,
                    contentDescription = "Instagram",
                    onClick = {
                        com.synfusion.attendance.ui.components.SafeExternalLinkHandler.openLink(context, "https://www.instagram.com/getsynfusion")
                    }
                )

                ContactIconItem(
                    icon = TelegramIcon,
                    contentDescription = "Telegram",
                    onClick = {
                        com.synfusion.attendance.ui.components.SafeExternalLinkHandler.openLink(context, "https://t.me/getsynfusion")
                    }
                )

                ContactIconItem(
                    icon = YouTubeIcon,
                    contentDescription = "YouTube",
                    onClick = {
                        com.synfusion.attendance.ui.components.SafeExternalLinkHandler.openLink(context, "https://www.youtube.com/@getsynfusion")
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.Email,
                    contentDescription = "Email",
                    onClick = {
                        com.synfusion.attendance.ui.components.SafeExternalLinkHandler.openEmail(context, "workdaily.synfusion@gmail.com")
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ContactIconItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineProfileEditBottomSheet(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val nameFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.edit_profile), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val context = LocalContext.current
                var selectedUriForCrop by remember { mutableStateOf<Uri?>(null) }
                var showActionSheet by remember { mutableStateOf(false) }

                val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        selectedUriForCrop = uri
                    }
                }

                val displayUri = state.profileImageUri
                val currentPhotoPath = state.profileImageUrl

                val imageModel: Any? = remember(displayUri, currentPhotoPath) {
                    if (displayUri != null) displayUri
                    else if (currentPhotoPath.isNotEmpty()) java.io.File(currentPhotoPath)
                    else null
                }

                // Premium visual transition for the profile image
                var animatePhoto by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    animatePhoto = true
                }
                val animatedScale by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (animatePhoto) 1f else 0.7f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "photoScale"
                )
                val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (animatePhoto) 1f else 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 350),
                    label = "photoAlpha"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = animatedScale
                                scaleY = animatedScale
                                alpha = animatedAlpha
                            }
                            .padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageRequest = remember(imageModel) {
                                if (imageModel != null) {
                                    val builder = coil.request.ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .crossfade(true)
                                    if (imageModel is java.io.File) {
                                        builder.memoryCacheKey("${imageModel.absolutePath}_${imageModel.lastModified()}")
                                            .diskCacheKey("${imageModel.absolutePath}_${imageModel.lastModified()}")
                                    }
                                    builder.networkCachePolicy(coil.request.CachePolicy.DISABLED)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build()
                                } else null
                            }

                            if (imageModel != null && imageRequest != null) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Preview Photo",
                                    modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    error = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Avatar",
                                            modifier = Modifier.size(50.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Circular edit/pencil button in the bottom-right corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .minimumInteractiveComponentSize()
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
                                .clickable { showActionSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile Photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Show the 1:1 Image Cropper when an image has been selected
                if (selectedUriForCrop != null) {
                    CropScreenDialog(
                        imageUri = selectedUriForCrop!!,
                        onCropSuccess = { croppedUri ->
                            viewModel.onProfileImageSelected(croppedUri)
                            selectedUriForCrop = null
                        },
                        onCancel = {
                            selectedUriForCrop = null
                        }
                    )
                }

                // Compact Action Sheet Modal Bottom Sheet
                if (showActionSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showActionSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Profile Photo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Change Photo option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showActionSheet = false
                                        photoPickerLauncher.launch("image/*")
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Change Photo",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Remove Photo option (only visible if profile photo exists)
                            if (imageModel != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showActionSheet = false
                                            viewModel.onProfileImageSelected(null)
                                            viewModel.onRemoveProfilePhoto()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Remove Photo",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChange(it) },
                modifier = Modifier.fillMaxWidth().focusRequester(nameFocusRequester),
                label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.full_name)) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = if (state.dailyWage > 0) state.dailyWage.toInt().toString() else "",
                onValueChange = { viewModel.onWageChange(it.filter { ch -> ch.isDigit() }) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.daily_wage)) },
                leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = IndianCurrencyVisualTransformation()
            )

            OutlinedTextField(
                value = state.phone,
                onValueChange = { newValue ->
                    val digits = newValue.filter { it.isDigit() }.take(10)
                    viewModel.onPhoneChange(digits)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.phone_number)) },
                leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel))
                }
                Button(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.save_changes), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
