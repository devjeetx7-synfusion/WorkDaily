package com.synfusion.attendance.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.synfusion.attendance.backup.BackupManager
import com.synfusion.attendance.backup.ProgressTracker
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.components.CountrySelectionBottomSheet
import com.synfusion.attendance.ui.components.CropScreenDialog
import com.synfusion.attendance.ui.components.InstagramIcon
import com.synfusion.attendance.ui.components.WhatsAppIcon
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.AmountFormatter
import com.synfusion.attendance.utils.CurrencyVisualTransformation
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.getActivity
import com.synfusion.attendance.viewmodel.OfflineSettingsState
import com.synfusion.attendance.viewmodel.OfflineSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsDestination {
    MAIN,
    EDIT_PROFILE,
    PERSONAL_DETAILS,
    PRIVACY_SAFETY,
    APPEARANCE,
    NOTIFICATIONS,
    LANGUAGE,
    BACKUP_RESTORE,
    HELP_SUPPORT,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OfflineSettingsViewModel,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    initialDestination: SettingsDestination = SettingsDestination.MAIN,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDestination by rememberSaveable { mutableStateOf(initialDestination) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var isAppNotificationsEnabled by remember {
        mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var isPostPermissionsGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var isChannelEnabled by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val channel = manager.getNotificationChannel(com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE)
                channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
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
                } else true
                val actualChannelEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    val channel = manager.getNotificationChannel(com.synfusion.attendance.utils.LocalNotificationManager.CHANNEL_OFFLINE_ATTENDANCE)
                    channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
                } else true

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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showLogoutBottomSheet by remember { mutableStateOf(false) }
    var showPreDeleteReminderDialog by remember { mutableStateOf(false) }
    var showDeveloperSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    val topBarTitle = when (currentDestination) {
        SettingsDestination.MAIN -> "Settings and activity"
        SettingsDestination.EDIT_PROFILE -> "Edit profile"
        SettingsDestination.PERSONAL_DETAILS -> "Personal & work details"
        SettingsDestination.PRIVACY_SAFETY -> "Privacy & safety"
        SettingsDestination.APPEARANCE -> "Appearance"
        SettingsDestination.NOTIFICATIONS -> "Notifications & reminders"
        SettingsDestination.LANGUAGE -> "Language"
        SettingsDestination.BACKUP_RESTORE -> "Backup & restore"
        SettingsDestination.HELP_SUPPORT -> "Help & support"
        SettingsDestination.ABOUT -> "About WorkDaily"
    }

    val handleBackPress = {
        if (currentDestination != SettingsDestination.MAIN) {
            currentDestination = SettingsDestination.MAIN
        } else {
            onNavigateBack()
        }
    }

    androidx.activity.compose.BackHandler(onBack = handleBackPress)

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ResponsiveContainer {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        if (targetState != SettingsDestination.MAIN) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "SettingsNavigationTransition"
                ) { destination ->
                    when (destination) {
                        SettingsDestination.MAIN -> {
                            MainSettingsContent(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onNavigateDestination = { currentDestination = it },
                                onLogoutClick = { showPreDeleteReminderDialog = true }
                            )
                        }
                        SettingsDestination.EDIT_PROFILE -> {
                            EditProfileDestination(state = state, viewModel = viewModel)
                        }
                        SettingsDestination.PERSONAL_DETAILS -> {
                            PersonalWorkDetailsDestination(state = state, viewModel = viewModel)
                        }
                        SettingsDestination.PRIVACY_SAFETY -> {
                            PrivacySafetyDestination()
                        }
                        SettingsDestination.APPEARANCE -> {
                            AppearanceDestination(state = state, viewModel = viewModel)
                        }
                        SettingsDestination.NOTIFICATIONS -> {
                            NotificationsDestination(
                                state = state,
                                viewModel = viewModel,
                                isAppNotificationsEnabled = isAppNotificationsEnabled,
                                isPostPermissionsGranted = isPostPermissionsGranted,
                                isChannelEnabled = isChannelEnabled,
                                permissionLauncher = permissionLauncher,
                                context = context
                            )
                        }
                        SettingsDestination.LANGUAGE -> {
                            LanguageDestination(state = state, viewModel = viewModel, context = context)
                        }
                        SettingsDestination.BACKUP_RESTORE -> {
                            BackupRestoreDestination(state = state, lastBackupFile = lastBackupFile, context = context, onManageBackup = { showBackupSheet = true })
                        }
                        SettingsDestination.HELP_SUPPORT -> {
                            HelpSupportDestination(onNavigate = onNavigate, onDeveloperContact = { showDeveloperSheet = true }, context = context)
                        }
                        SettingsDestination.ABOUT -> {
                            AboutWorkDailyDestination(onNavigate = onNavigate)
                        }
                    }
                }
            }
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
                text = { Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.pre_delete_msg)) },
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
                        TextButton(onClick = { showPreDeleteReminderDialog = false }) {
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

// Reusable Instagram-inspired Settings Navigation Row Component
@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun MainSettingsContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateDestination: (SettingsDestination) -> Unit,
    onLogoutClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    val allRows = listOf(
        Triple(SettingsDestination.EDIT_PROFILE, "Edit profile", "Photo, name, username and bio"),
        Triple(SettingsDestination.PERSONAL_DETAILS, "Personal & work details", "Country, currency, wage and phone"),
        Triple(SettingsDestination.PRIVACY_SAFETY, "Privacy & safety", "Blocked accounts and privacy"),
        Triple(SettingsDestination.APPEARANCE, "Appearance", "Theme and splash screen"),
        Triple(SettingsDestination.NOTIFICATIONS, "Notifications & reminders", "Alerts and reminder settings"),
        Triple(SettingsDestination.LANGUAGE, "Language", "Current selected language"),
        Triple(SettingsDestination.BACKUP_RESTORE, "Backup & restore", "Manage saved data"),
        Triple(SettingsDestination.HELP_SUPPORT, "Help & support", "Guides, contact and community"),
        Triple(SettingsDestination.ABOUT, "About WorkDaily", "Version, policies and terms")
    )

    val isSearching = searchQuery.trim().isNotEmpty()
    val filteredRows = if (isSearching) {
        allRows.filter {
            it.second.contains(searchQuery, ignoreCase = true) || it.third.contains(searchQuery, ignoreCase = true)
        }
    } else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search settings", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (isSearching) {
            if (filteredRows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching settings found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                filteredRows.forEach { (dest, title, subtitle) ->
                    val icon = when (dest) {
                        SettingsDestination.EDIT_PROFILE -> Icons.Default.Person
                        SettingsDestination.PERSONAL_DETAILS -> Icons.Default.Work
                        SettingsDestination.PRIVACY_SAFETY -> Icons.Default.Security
                        SettingsDestination.APPEARANCE -> Icons.Default.Palette
                        SettingsDestination.NOTIFICATIONS -> Icons.Default.Notifications
                        SettingsDestination.LANGUAGE -> Icons.Default.Language
                        SettingsDestination.BACKUP_RESTORE -> Icons.Default.Backup
                        SettingsDestination.HELP_SUPPORT -> Icons.Default.Help
                        SettingsDestination.ABOUT -> Icons.Default.Info
                        else -> Icons.Default.Settings
                    }
                    SettingsNavigationRow(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        onClick = { onNavigateDestination(dest) }
                    )
                }
            }
        } else {
            // Category 1: ACCOUNT
            SettingsSectionHeader("ACCOUNT")
            SettingsNavigationRow(
                title = "Edit profile",
                subtitle = "Photo, name, username and bio",
                icon = Icons.Default.Person,
                onClick = { onNavigateDestination(SettingsDestination.EDIT_PROFILE) }
            )
            SettingsNavigationRow(
                title = "Personal & work details",
                subtitle = "Country, currency, wage and phone",
                icon = Icons.Default.Work,
                onClick = { onNavigateDestination(SettingsDestination.PERSONAL_DETAILS) }
            )
            SettingsNavigationRow(
                title = "Privacy & safety",
                subtitle = "Blocked accounts and privacy",
                icon = Icons.Default.Security,
                onClick = { onNavigateDestination(SettingsDestination.PRIVACY_SAFETY) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Category 2: PREFERENCES
            SettingsSectionHeader("PREFERENCES")
            SettingsNavigationRow(
                title = "Appearance",
                subtitle = "Theme and splash screen",
                icon = Icons.Default.Palette,
                onClick = { onNavigateDestination(SettingsDestination.APPEARANCE) }
            )
            SettingsNavigationRow(
                title = "Notifications & reminders",
                subtitle = "Alerts and reminder settings",
                icon = Icons.Default.Notifications,
                onClick = { onNavigateDestination(SettingsDestination.NOTIFICATIONS) }
            )
            SettingsNavigationRow(
                title = "Language",
                subtitle = "Current selected language",
                icon = Icons.Default.Language,
                onClick = { onNavigateDestination(SettingsDestination.LANGUAGE) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Category 3: DATA & SUPPORT
            SettingsSectionHeader("DATA & SUPPORT")
            SettingsNavigationRow(
                title = "Backup & restore",
                subtitle = "Manage saved data",
                icon = Icons.Default.Backup,
                onClick = { onNavigateDestination(SettingsDestination.BACKUP_RESTORE) }
            )
            SettingsNavigationRow(
                title = "Help & support",
                subtitle = "Guides, contact and community",
                icon = Icons.Default.Help,
                onClick = { onNavigateDestination(SettingsDestination.HELP_SUPPORT) }
            )
            SettingsNavigationRow(
                title = "About WorkDaily",
                subtitle = "Version, policies and terms",
                icon = Icons.Default.Info,
                onClick = { onNavigateDestination(SettingsDestination.ABOUT) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Category 4: ACCOUNT ACTION
            SettingsSectionHeader("ACCOUNT ACTION")
            SettingsNavigationRow(
                title = "Log out",
                subtitle = "Log out of WorkDaily on this device",
                icon = Icons.AutoMirrored.Filled.Logout,
                iconContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                iconTint = MaterialTheme.colorScheme.error,
                onClick = onLogoutClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDestination(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel
) {
    val context = LocalContext.current
    var selectedUriForCrop by remember { mutableStateOf<Uri?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedUriForCrop = uri
    }

    val displayUri = state.profileImageUri
    val currentPhotoPath = state.profileImageUrl
    val googlePhotoUrl = state.googlePhotoUrl

    val imageModel: Any? = remember(displayUri, currentPhotoPath, googlePhotoUrl) {
        if (displayUri != null) displayUri
        else if (currentPhotoPath.isNotEmpty()) java.io.File(currentPhotoPath)
        else if (googlePhotoUrl.isNotEmpty()) googlePhotoUrl
        else null
    }

    var usernameText by remember { mutableStateOf(state.name.lowercase().replace(" ", "").ifEmpty { "user" }) }
    var professionText by remember { mutableStateOf(state.profession) }
    var bioText by remember { mutableStateOf(state.bio) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Photo Section
        Box(modifier = Modifier.padding(vertical = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
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
                                .networkCachePolicy(coil.request.CachePolicy.DISABLED)
                        } else {
                            builder.networkCachePolicy(coil.request.CachePolicy.ENABLED)
                        }
                        builder.diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .build()
                    } else null
                }

                if (imageModel != null && imageRequest != null) {
                    coil.compose.SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        error = {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { showActionSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.onNameChange(it) },
            label = { Text("Display Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = usernameText,
            onValueChange = { newValue ->
                val cleaned = newValue.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
                usernameText = cleaned
            },
            label = { Text("Unique Username") },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = professionText,
            onValueChange = { professionText = it },
            label = { Text("Profession") },
            leadingIcon = { Icon(Icons.Default.Badge, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = bioText,
            onValueChange = { bioText = it },
            label = { Text("Bio") },
            leadingIcon = { Icon(Icons.Default.Description, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                viewModel.saveProfileInfo(
                    name = state.name,
                    profession = professionText,
                    bio = bioText
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isSaving
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (selectedUriForCrop != null) {
        CropScreenDialog(
            imageUri = selectedUriForCrop!!,
            onCropSuccess = { croppedUri ->
                viewModel.onProfileImageSelected(croppedUri)
                selectedUriForCrop = null
            },
            onCancel = { selectedUriForCrop = null }
        )
    }

    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Profile Photo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        showActionSheet = false
                        photoPickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Photo")
                }
                if (imageModel != null) {
                    OutlinedButton(
                        onClick = {
                            showActionSheet = false
                            viewModel.onProfileImageSelected(null)
                            viewModel.onRemoveProfilePhoto()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove Photo")
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalWorkDetailsDestination(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel
) {
    var showCountryPickerSheet by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf(state.gender) }
    var showGenderConfirmDialog by remember { mutableStateOf(false) }
    var pendingGenderChange by remember { mutableStateOf<String?>(null) }
    val activeCountry = remember(state.countryCode) { CountryRepository.getCountry(state.countryCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GENDER SELECTION
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
                            .clickable {
                                if (genderOption != selectedGender) {
                                    pendingGenderChange = genderOption
                                    showGenderConfirmDialog = true
                                }
                            },
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
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = "${activeCountry.flagEmoji} ${activeCountry.displayName} (${activeCountry.currencySymbol})",
                onValueChange = {},
                readOnly = true,
                label = { Text("Country & Region") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showCountryPickerSheet = true }
            )
        }

        OutlinedTextField(
            value = if (state.dailyWage > 0) state.dailyWage.toInt().toString() else "",
            onValueChange = { viewModel.onWageChange(it.filter { ch -> ch.isDigit() }) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Daily Wage") },
            leadingIcon = { Text(CountryRepository.getCurrencySymbol(state.currencyCode), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CurrencyVisualTransformation(state.currencyCode)
        )

        OutlinedTextField(
            value = state.phone,
            onValueChange = { newValue ->
                val digits = newValue.filter { it.isDigit() }.take(15)
                viewModel.onPhoneChange(digits)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Private Phone Number") },
            leadingIcon = { Text("${activeCountry.callingCode} ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveChanges() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !state.isSaving
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save Details", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCountryPickerSheet) {
        CountrySelectionBottomSheet(
            selectedCountryCode = state.countryCode,
            onCountrySelected = { country ->
                viewModel.onCountryChange(country.countryCode)
                showCountryPickerSheet = false
            },
            onDismiss = { showCountryPickerSheet = false }
        )
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
        windowInsets = WindowInsets(0.dp),
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
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.privacy_policy), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Text(
                text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.privacy_intro),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            val privacySections = listOf(
                Pair(com.synfusion.attendance.R.string.privacy_local_data_title, com.synfusion.attendance.R.string.privacy_local_data_desc),
                Pair(com.synfusion.attendance.R.string.privacy_notifications_title, com.synfusion.attendance.R.string.privacy_notifications_desc),
                Pair(com.synfusion.attendance.R.string.privacy_exports_title, com.synfusion.attendance.R.string.privacy_exports_desc),
                Pair(com.synfusion.attendance.R.string.privacy_sharing_title, com.synfusion.attendance.R.string.privacy_sharing_desc),
                Pair(com.synfusion.attendance.R.string.privacy_deletion_title, com.synfusion.attendance.R.string.privacy_deletion_desc),
                Pair(com.synfusion.attendance.R.string.privacy_children_title, com.synfusion.attendance.R.string.privacy_children_desc),
                Pair(com.synfusion.attendance.R.string.privacy_contact_title, com.synfusion.attendance.R.string.privacy_contact_desc)
            )

            privacySections.forEach { (titleRes, descRes) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(titleRes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(descRes),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

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
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.terms_conditions), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            val termsSections = listOf(
                Pair(com.synfusion.attendance.R.string.terms_acceptance_title, com.synfusion.attendance.R.string.terms_acceptance_desc),
                Pair(com.synfusion.attendance.R.string.terms_use_title, com.synfusion.attendance.R.string.terms_use_desc),
                Pair(com.synfusion.attendance.R.string.terms_responsibilities_title, com.synfusion.attendance.R.string.terms_responsibilities_desc),
                Pair(com.synfusion.attendance.R.string.terms_financial_title, com.synfusion.attendance.R.string.terms_financial_desc),
                Pair(com.synfusion.attendance.R.string.terms_notifications_title, com.synfusion.attendance.R.string.terms_notifications_desc),
                Pair(com.synfusion.attendance.R.string.terms_backup_title, com.synfusion.attendance.R.string.terms_backup_desc),
                Pair(com.synfusion.attendance.R.string.terms_branding_title, com.synfusion.attendance.R.string.terms_branding_desc),
                Pair(com.synfusion.attendance.R.string.terms_contact_title, com.synfusion.attendance.R.string.terms_contact_desc)
            )

            termsSections.forEach { (titleRes, descRes) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(titleRes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(descRes),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

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
fun PrivacySafetyDestination() {
    var showBlockedSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column {
                SettingsNavigationRow(
                    title = "Blocked Users",
                    subtitle = "View and manage blocked accounts",
                    icon = Icons.Default.Block,
                    onClick = { showBlockedSheet = true }
                )
            }
        }

        Text(
            text = "WorkDaily respects user privacy. All attendance, wage, and financial data remain 100% stored offline on your local device SQLite Room database.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    if (showBlockedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBlockedSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Blocked Users", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("No blocked accounts currently.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AppearanceDestination(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel
) {
    val isSystemDark = isSystemInDarkTheme()
    val effectiveDarkMode = state.isDarkMode ?: isSystemDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Theme Preference", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTheme(false) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.isDarkMode == false, onClick = { viewModel.toggleTheme(false) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Light Mode", fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleTheme(true) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = state.isDarkMode == true, onClick = { viewModel.toggleTheme(true) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dark Mode", fontSize = 14.sp)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Splash Screen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Show WorkDaily startup intro logo screen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.isSplashEnabled, onCheckedChange = { viewModel.toggleSplash(it) })
                }
            }
        }
    }
}

@Composable
fun NotificationsDestination(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel,
    isAppNotificationsEnabled: Boolean,
    isPostPermissionsGranted: Boolean,
    isChannelEnabled: Boolean,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context
) {
    val actualNotificationState = state.isRemindersEnabled && isAppNotificationsEnabled && isPostPermissionsGranted && isChannelEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Reminders", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (actualNotificationState) "Notifications enabled" else "Notifications disabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                        Toast.makeText(context, "Open system notification settings", Toast.LENGTH_SHORT).show()
                                    }
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
            }
        }
    }
}

@Composable
fun LanguageDestination(
    state: OfflineSettingsState,
    viewModel: OfflineSettingsViewModel,
    context: Context
) {
    val supportedLanguages = mapOf("en" to "English", "hi" to "हिंदी")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Selected Language", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = supportedLanguages[state.language] ?: "English",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = {
                        val intent = Intent(context, LanguageSelectorActivity::class.java)
                        context.getActivity()?.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Change App Language")
                }
            }
        }
    }
}

@Composable
fun BackupRestoreDestination(
    state: OfflineSettingsState,
    lastBackupFile: File?,
    context: Context,
    onManageBackup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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

                val lastLocStr = state.lastBackupLoc ?: (lastBackupFile?.let { "External Storage" } ?: "N/A")

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Backup:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastDateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Backup Size:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lastSizeStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Backup Location:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Manage Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Text("Share Backup", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSupportDestination(
    onNavigate: (String) -> Unit,
    onDeveloperContact: () -> Unit,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column {
                SettingsNavigationRow(
                    title = "How to Use WorkDaily",
                    subtitle = "Quick guide & app features",
                    icon = Icons.Default.Info,
                    onClick = { onNavigate("how_to_use") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsNavigationRow(
                    title = "WhatsApp Channel",
                    subtitle = "Follow updates & announcements",
                    icon = WhatsAppIcon,
                    onClick = {
                        com.synfusion.attendance.ui.components.SafeExternalLinkHandler.openLink(
                            context,
                            "https://whatsapp.com/channel/0029Vb8bS4U23n3lJwvpbP3H"
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsNavigationRow(
                    title = "Developer Contact",
                    subtitle = "Reach out for support & feedback",
                    icon = Icons.Default.Code,
                    onClick = onDeveloperContact
                )
            }
        }
    }
}

@Composable
fun AboutWorkDailyDestination(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("WorkDaily", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Version 1.0.0 • Offline First", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column {
                SettingsNavigationRow(
                    title = "Privacy Policy",
                    subtitle = "Read WorkDaily privacy policy",
                    icon = Icons.Default.Security,
                    onClick = { onNavigate("privacy_policy") }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsNavigationRow(
                    title = "Terms & Conditions",
                    subtitle = "Terms of service and legal notice",
                    icon = Icons.Default.Description,
                    onClick = { onNavigate("terms_conditions") }
                )
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
    val isLandscape = com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo.current.isLandscape
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        windowInsets = WindowInsets(0.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(0.45f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(44.dp)
                    )

                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.logout_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.logout_backup_question),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBackupAndLogout,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.logout_without_backup), fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.logout_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.logout_backup_question),
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
        windowInsets = WindowInsets(0.dp),
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
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
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
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
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
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
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
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
