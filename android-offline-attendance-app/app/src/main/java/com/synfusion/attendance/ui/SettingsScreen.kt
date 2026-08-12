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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*

import com.synfusion.attendance.ui.components.InstagramIcon
import com.synfusion.attendance.ui.components.TelegramIcon
import com.synfusion.attendance.ui.components.YouTubeIcon

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing

@OptIn(ExperimentalMaterial3Api::class)
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

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPostPermissionsGranted = isGranted
        val systemEnabled = isAppNotificationsEnabled && isGranted

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

                isAppNotificationsEnabled = actualAppNotificationsEnabled
                isPostPermissionsGranted = actualPostPermissionsGranted

                val systemEnabled = actualAppNotificationsEnabled && actualPostPermissionsGranted
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
    var showNotificationTestCenter by remember { mutableStateOf(false) }

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
            context.startActivity(intent)
            (context as? android.app.Activity)?.finish()
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.app_preferences), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.settings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

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
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                        contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.profile_settings),
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
                                .clickable { showProfileEditSheet = true },
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
                                context.startActivity(intent)
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

            // Notification Settings Card
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.notification_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Status Row


                        // Reminders Switch
                        val actualNotificationState = state.isRemindersEnabled && isAppNotificationsEnabled && isPostPermissionsGranted
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
                                        // Request system permissions if needed.
                                        if (!isAppNotificationsEnabled) {
                                            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.getActivity()?.startActivity(intent)
                                        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            )
                                            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                viewModel.toggleReminders(true)
                                                com.synfusion.attendance.utils.NotificationScheduler.schedulePeriodicWork(context)
                                            }
                                            context.getActivity()?.startActivity(intent)
                                        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !isPostPermissionsGranted) {
                                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth().clickable { showNotificationTestCenter = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Notification Center", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Developer Diagnostics & Test Panel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.backup_and_restore), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val lastDateStr = lastBackupFile?.let {
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it.lastModified()))
                        } ?: "Never"
                        val lastSizeStr = lastBackupFile?.let {
                            val sizeMb = it.length() / 1024.0 / 1024.0
                            String.format(Locale.getDefault(), "%.2f MB", sizeMb)
                        } ?: "N/A"
                        val lastLocStr = lastBackupFile?.let { "External Files" } ?: "N/A"

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
                                onClick = { showBackupSheet = true },
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
                                        val uri = FileProvider.getUriForFile(context, "com.synfusion.attendance.fileprovider", lastBackupFile!!)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Backup"))
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
                        Row(modifier = Modifier.fillMaxWidth().clickable { showDeveloperSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
        if (showNotificationTestCenter) {
            NotificationTestCenterBottomSheet(
                viewModel = viewModel,
                onDismiss = { showNotificationTestCenter = false }
            )
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
                            context.startActivity(Intent.createChooser(intent, "Share Backup ZIP"))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTestCenterBottomSheet(
    viewModel: OfflineSettingsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val settingsState = viewModel.state.collectAsState().value
    val masterEnabled = settingsState.isRemindersEnabled
    var permissionGranted by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var systemPostPermissionGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Refresh permissions on resume just in case
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val actualAppNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                val actualPostPermissionsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                permissionGranted = actualAppNotificationsEnabled
                systemPostPermissionGranted = actualPostPermissionsGranted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val repository = remember { com.synfusion.attendance.data.LocalAttendanceRepository(context) }
    var placeholders by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var todayMarked by remember { mutableStateOf("NOT MARKED") }

    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var diagnosticDialogTitle by remember { mutableStateOf("") }
    var diagnosticDialogDesc by remember { mutableStateOf("") }
    var diagnosticDialogActionText by remember { mutableStateOf("") }
    var diagnosticDialogAction by remember { mutableStateOf<() -> Unit>({}) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        systemPostPermissionGranted = isGranted
        permissionGranted = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    LaunchedEffect(Unit) {
        val user = repository.getUser()
        val allRecords = repository.getPersonalAttendance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        placeholders = com.synfusion.attendance.utils.NotificationRuleEngine.buildPlaceholders(user, allRecords, todayStr)
        val todayRecord = allRecords.find { it.date == todayStr && it.status != "advance" }
        todayMarked = if (todayRecord != null) "MARKED (${todayRecord.status.uppercase()})" else "NOT MARKED"
    }

    var logs by remember { mutableStateOf(com.synfusion.attendance.utils.LocalNotificationManager.getNotificationLogs(context)) }

    var previewTitle by remember { mutableStateOf("WorkDaily") }
    var previewMessage by remember { mutableStateOf("Choose a category to preview below.") }
    var showPreviewCard by remember { mutableStateOf(false) }

    val testCategories = remember {
        listOf(
            TestCategoryItem("Today's Attendance", "daily_attendance", "आज की Attendance", com.synfusion.attendance.utils.NotificationMessagePool.dailyAttendanceMessages),
            TestCategoryItem("2 Days Missed", "missed_two_days", "लगातार 2 दिन Missed", com.synfusion.attendance.utils.NotificationMessagePool.missedTwoDaysMessages),
            TestCategoryItem("3 Days Missed", "missed_three_days", "लगातार 3 दिन Missed", com.synfusion.attendance.utils.NotificationMessagePool.missedThreeDaysMessages),
            TestCategoryItem("7 Days Missed", "missed_seven_plus", "सावधान! 7 दिन Missed", com.synfusion.attendance.utils.NotificationMessagePool.missedSevenPlusDaysMessages),
            TestCategoryItem("10 Days Missed", "missed_ten_days", "गंभीर चेतावनी!", com.synfusion.attendance.utils.NotificationMessagePool.missedTenPlusDaysMessages),
            TestCategoryItem("Weekly Report", "weekly_report", "Weekly Report 📊", com.synfusion.attendance.utils.NotificationMessagePool.weeklyReportMessages),
            TestCategoryItem("Monthly Report", "monthly_report", "Monthly Summary 📅", com.synfusion.attendance.utils.NotificationMessagePool.monthlyReportMessages),
            TestCategoryItem("Earnings", "earnings", "कमाई अपडेट 💵", com.synfusion.attendance.utils.NotificationMessagePool.earningsMessages),
            TestCategoryItem("Overtime", "overtime", "ओवरटाइम धमाका 💪", com.synfusion.attendance.utils.NotificationMessagePool.earningsMessages.filter { it.contains("overtime") || it.contains("घंटे") }.ifEmpty { com.synfusion.attendance.utils.NotificationMessagePool.earningsMessages }),
            TestCategoryItem("Advance", "advance", "एडवांस अपडेट 💰", com.synfusion.attendance.utils.NotificationMessagePool.earningsMessages.filter { it.contains("advance") || it.contains("अग्रिम") }.ifEmpty { com.synfusion.attendance.utils.NotificationMessagePool.earningsMessages }),
            TestCategoryItem("Yearly Report", "yearly_report", "सालाना रिपोर्ट 🏆", com.synfusion.attendance.utils.NotificationMessagePool.yearlyReportMessages),
            TestCategoryItem("Funny", "funny", "ओए भाई सुनो! 😂", com.synfusion.attendance.utils.NotificationMessagePool.funnyRoastMessages),
            TestCategoryItem("Motivational", "motivational", "जीत की तैयारी! 🌟", com.synfusion.attendance.utils.NotificationMessagePool.motivationalMessages),
            TestCategoryItem("Streak", "streak", "Consistency Streak! 🔥", com.synfusion.attendance.utils.NotificationMessagePool.motivationalMessages)
        )
    }

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Notification Test Center", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // 1. Diagnostics Panel
            Text("Why didn't I receive notification? (Diagnostics)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticItemRow("Reminders Preference", if (masterEnabled) "ON ✓" else "OFF ✕")
                    DiagnosticItemRow("System Notifications", if (permissionGranted) "ENABLED ✓" else "DISABLED ✕")
                    val isPostGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else true
                    DiagnosticItemRow("Notification Permission", if (isPostGranted) "GRANTED ✓" else "DENIED ✕")
                    DiagnosticItemRow("Effective Reminders", if (masterEnabled && permissionGranted && isPostGranted) "ACTIVE ✓" else "BLOCKED ✕")
                    DiagnosticItemRow("POST_NOTIFICATIONS", if (systemPostPermissionGranted) "GRANTED ✓" else "DENIED ✕")
                    DiagnosticItemRow("Scheduler Status", if (masterEnabled) "ACTIVE (Every 1 Hour)" else "INACTIVE")
                    DiagnosticItemRow("Today's Attendance Status", todayMarked)
                    DiagnosticItemRow("Eligible Time Window", "6:00 PM - 10:00 PM")
                    DiagnosticItemRow("Device Battery Optimization", "Normal")
                    DiagnosticItemRow("Next Eligible Notification", if (todayMarked.startsWith("NOT MARKED")) "Today's Attendance Reminder (6-10 PM)" else "Weekly / Monthly Report (Time Triggered)")
                    DiagnosticItemRow("Skip Reason if Any", if (!masterEnabled) "Master disabled" else if (!permissionGranted || !systemPostPermissionGranted) "Permission denied" else if (todayMarked.startsWith("MARKED")) "Attendance already marked for today" else "None (Ready for 6:00 PM Window)")
                    DiagnosticItemRow("Latest Evaluation Execution", if (logs.isNotEmpty()) logs.first().time else "Never")
                    DiagnosticItemRow("Last Notification Sent", if (logs.isNotEmpty()) logs.first().category.uppercase() else "None")
                }
            }

            // Reset Rotation & Clear logs controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val prefs = context.getSharedPreferences("notification_selector_history", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        Toast.makeText(context, "Rotation history reset successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Rotation History", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val randomCat = testCategories.random()
                        previewTitle = "Random: " + randomCat.displayName
                        val evaluated = com.synfusion.attendance.utils.NotificationMessageSelector.selectMessage(
                            context, randomCat.category, randomCat.pool, placeholders
                        )
                        previewMessage = evaluated.ifEmpty { "No templates available" }
                        showPreviewCard = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview Random", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Calculated Variables Section
            Text("Calculated Variables Map", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    placeholders.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("{$key}", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            Text(value.ifEmpty { "None" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Preview Panel (Top anchored or visible upon selection)
            if (showPreviewCard) {
                Text("Notification Preview", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                // Draw mockup phone drawer card
                Card(
                    modifier = Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Dark premium phone drawer background
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp).background(Color(0xFF0EA5E9), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WorkDaily", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF22C55E), androidx.compose.foundation.shape.CircleShape)) // green dot
                        }
                        Text(text = previewTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = previewMessage, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }

            // 3. Test Categories List
            Text("Available Notifications Categories", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                testCategories.forEach { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Category: ${cat.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        previewTitle = cat.titlePrefix
                                        val evaluated = com.synfusion.attendance.utils.NotificationMessageSelector.selectMessage(
                                            context, cat.category, cat.pool, placeholders
                                        )
                                        previewMessage = evaluated.ifEmpty { "No templates available" }
                                        showPreviewCard = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.preview_notification), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val channelId = when (cat.category) {
                                            "daily_attendance", "missed_two_days", "missed_three_days", "missed_seven_plus", "missed_ten_days", "long_absence" -> com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_ATTENDANCE
                                            "weekly_report", "monthly_report", "yearly_report" -> com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_REPORTS
                                            "earnings", "overtime", "advance" -> com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_EARNINGS
                                            else -> com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_FUN_MOTIVATION
                                        }

                                        // Refresh permissions
                                        val isAppNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                                        val isPostPermissionsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        } else {
                                            true
                                        }

                                        if (!isAppNotificationsEnabled) {
                                            diagnosticDialogTitle = context.getString(com.synfusion.attendance.R.string.notif_status_disabled)
                                            diagnosticDialogDesc = context.getString(com.synfusion.attendance.R.string.notif_disabled_desc)
                                            diagnosticDialogActionText = context.getString(com.synfusion.attendance.R.string.open_notif_settings)
                                            diagnosticDialogAction = {
                                                val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                }
                                                context.startActivity(intent)
                                            }
                                            showDiagnosticDialog = true
                                            return@Button
                                        }

                                        if (!isPostPermissionsGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            diagnosticDialogTitle = context.getString(com.synfusion.attendance.R.string.notif_status_permission_required)
                                            diagnosticDialogDesc = context.getString(com.synfusion.attendance.R.string.notif_permission_desc)
                                            diagnosticDialogActionText = context.getString(com.synfusion.attendance.R.string.allow_notifications)
                                            diagnosticDialogAction = {
                                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            showDiagnosticDialog = true
                                            return@Button
                                        }

                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                            val channel = manager.getNotificationChannel(channelId)
                                            if (channel != null && channel.importance == android.app.NotificationManager.IMPORTANCE_NONE) {
                                                diagnosticDialogTitle = context.getString(com.synfusion.attendance.R.string.notif_channel_disabled)
                                                diagnosticDialogDesc = context.getString(com.synfusion.attendance.R.string.notif_channel_disabled_desc)
                                                diagnosticDialogActionText = context.getString(com.synfusion.attendance.R.string.open_notif_settings)
                                                diagnosticDialogAction = {
                                                    val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                        putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channel.id)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                                showDiagnosticDialog = true
                                                return@Button
                                            } else if (channel == null) {
                                                com.synfusion.attendance.utils.LocalNotificationManager.initChannels(context)
                                            }
                                        }

                                        val evaluated = com.synfusion.attendance.utils.NotificationMessageSelector.selectMessage(
                                            context, cat.category, cat.pool, placeholders
                                        )
                                        val messageToSend = evaluated.ifEmpty { "No templates available" }

                                        com.synfusion.attendance.utils.LocalNotificationManager.sendNotification(
                                            context = context,
                                            channelId = channelId,
                                            title = cat.titlePrefix,
                                            message = messageToSend,
                                            category = cat.category,
                                            reason = "Manual developer preview test trigger"
                                        )

                                        // Refresh history
                                        logs = com.synfusion.attendance.utils.LocalNotificationManager.getNotificationLogs(context)
                                        Toast.makeText(context, context.getString(com.synfusion.attendance.R.string.notif_sent_successfully), Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.dev_test), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Execution Logs History
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Developer Notification History Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (logs.isNotEmpty()) {
                    TextButton(onClick = {
                        com.synfusion.attendance.utils.LocalNotificationManager.clearNotificationLogs(context)
                        logs = emptyList()
                    }) {
                        Text("Clear Logs", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No send history logs logged yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.forEach { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.category.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(log.time, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(log.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Reason: ${log.reason}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showDiagnosticDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            title = { Text(diagnosticDialogTitle, fontWeight = FontWeight.Bold) },
            text = { Text(diagnosticDialogDesc) },
            confirmButton = {
                Button(onClick = {
                    showDiagnosticDialog = false
                    diagnosticDialogAction()
                }) {
                    Text(diagnosticDialogActionText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel))
                }
            }
        )
    }
}

data class TestCategoryItem(
    val displayName: String,
    val category: String,
    val titlePrefix: String,
    val pool: List<String>
)

@Composable
fun DiagnosticItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
