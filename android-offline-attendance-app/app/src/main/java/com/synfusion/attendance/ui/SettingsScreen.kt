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
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = OfflineSettingsState())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showHowToUseSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }
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
                                    Icon(Icons.Default.Check, contentDescription = "Save Changes", tint = MaterialTheme.colorScheme.primary)
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
                                contentDescription = "User Profile",
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
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
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
                Text("General Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup & Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
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
                                onClick = { showBackupSheet = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Text("Share", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Information & Support", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { showHowToUseSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                Text("How to Use WorkDaily", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Quick guide & tutorials", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth().clickable { showPrivacySheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                Text("Privacy Policy", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Stored safely offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth().clickable { showTermsSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Terms & Conditions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Rules and guidelines for using the app", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text("Developer Contact", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Reach out to us for support", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        if (showHowToUseSheet) {
            HowToUseBottomSheet(onDismiss = { showHowToUseSheet = false })
        }
        if (showPrivacySheet) {
            PrivacyPolicyBottomSheet(onDismiss = { showPrivacySheet = false })
        }
        if (showTermsSheet) {
            TermsConditionsBottomSheet(onDismiss = { showTermsSheet = false })
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
                title = { Text("Pre-Delete Reminder", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Text("To avoid losing your attendance history, create a backup before removing the app.")
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
                        Text("Backup Now")
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
                            Text("Continue", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = { showPreDeleteReminderDialog = false }
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
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
                    viewModel.logout()
                    onLogout()
                }
            )
        }

        if (isBackingUpForLogout) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Creating Backup...", fontWeight = FontWeight.Bold) },
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
                title = { Text("Backup Created Successfully", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your backup details:")
                        Text("• Size: $fileSizeMb MB", fontSize = 13.sp)
                        Text("• Created Time: $createdTime", fontSize = 13.sp)
                        Text("• Location: ${file.parentFile?.name ?: "Downloads"}", fontSize = 13.sp)
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
                        Text("Share")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            backupSuccessDialogDetails = null
                            viewModel.logout()
                            onLogout()
                        }
                    ) {
                        Text("Done")
                    }
                }
            )
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
                    Text("Backup & Logout", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onLogoutWithoutBackup,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Logout Without Backup", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("How to Use WorkDaily", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "WorkDaily helps users manage personal attendance, daily wages, advances, payments, notes, and records.\n\n• Personal users can track their own attendance, work days, payments, and notes safely.\n\nPassbook and reports help users review records clearly.",
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
                Text("Close", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
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
                Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                Text("Close", fontWeight = FontWeight.Bold)
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
                Text("Terms & Conditions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                Text("Close", fontWeight = FontWeight.Bold)
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
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Developer Avatar",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SynFusion", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("workdaily.synfusion@gmail.com", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactIconItem(
                    icon = Icons.Default.Chat,
                    contentDescription = "WhatsApp",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/918250000000")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = "Instagram",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/devjeetx7")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle Instagram", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.Send,
                    contentDescription = "Telegram",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/synfusion_coder")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.Email,
                    contentDescription = "Email",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:devjeetx7@gmail.com")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle Email", Toast.LENGTH_SHORT).show()
                        }
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
                Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                    Text("Cancel")
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
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
