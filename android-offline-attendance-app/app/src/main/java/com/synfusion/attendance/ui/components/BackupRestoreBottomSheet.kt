package com.synfusion.attendance.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.backup.BackupManager
import com.synfusion.attendance.backup.BackupManifest
import com.synfusion.attendance.backup.ProgressTracker
import com.synfusion.attendance.backup.RestoreManager
import com.synfusion.attendance.backup.RestoreSummaryReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }
    val restoreManager = remember { RestoreManager(context) }
    val progressTracker = remember { ProgressTracker() }

    val progress by progressTracker.progress.collectAsState(initial = 0f)
    val status by progressTracker.status.collectAsState(initial = "")

    var isOperating by remember { mutableStateOf(false) }
    var operationType by remember { mutableStateOf("") } // "BACKUP" or "RESTORE"

    var restorePreview by remember { mutableStateOf<BackupManifest?>(null) }
    var restoreFileToApply by remember { mutableStateOf<File?>(null) }
    var mergeMode by remember { mutableStateOf(false) } // False = Replace, True = Merge

    var operationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var restoreSummaryReport by remember { mutableStateOf<RestoreSummaryReport?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File Create Launcher for SAF Backup
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                isOperating = true
                operationType = "BACKUP"
                errorMessage = null
                operationSuccess = null

                coroutineScope.launch {
                    try {
                        val tempFile = File(context.cacheDir, "AttendanceBackup_temp.zip")
                        backupManager.performBackup(tempFile, progressTracker)

                        // Stream temp zip bytes to output URI via SAF
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                tempFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        tempFile.delete()
                        operationSuccess = true
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: "Backup was interrupted or failed due to low storage."
                        operationSuccess = false
                    } finally {
                        isOperating = false
                    }
                }
            } else {
                Toast.makeText(context, "Backup cancelled by user", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // File Pick Launcher for SAF Restore
    val pickRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        val tempFile = File(context.cacheDir, "RestoreTemp.zip")
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
                            Toast.makeText(context, "Invalid or corrupt backup package", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to load backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Restore selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )

    BackHandler(enabled = isOperating) {
        // Blocks physical back button during restore
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isOperating) onDismiss() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !isOperating }
        ),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { if (!isOperating) BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Backup & Restore",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Export and secure your application state",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            if (isOperating) {
                // Progress Loader view
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(72.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$status (${progress.toInt()}%)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Please do not close or exit the app during this process.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (operationSuccess != null) {
                // Success / Error result screen
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val isSuccess = operationSuccess == true
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isSuccess) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = if (isSuccess) "Success!" else "Failed!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isSuccess) {
                            if (operationType == "BACKUP") "Your application state backup has been generated and saved securely."
                            else "Your data has been completely restored."
                        } else {
                            errorMessage ?: "An unexpected error occurred during the process."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Data Consistency Report summary (Show nice rows with ✓)
                    if (isSuccess && operationType == "RESTORE" && restoreSummaryReport != null) {
                        val report = restoreSummaryReport!!
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Restore Data Report", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                ReportRow(label = "Attendance Database", detail = "${report.attendanceCount}/${report.totalAttendanceRecords} Records", success = true)
                                ReportRow(label = "User Profile", detail = "Imported", success = report.profileRestored)
                                ReportRow(label = "Media Files", detail = "${report.mediaCount}/${report.totalMediaFiles} Files", success = true)
                                ReportRow(label = "Application Settings", detail = "Verified", success = report.settingsRestored)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            operationSuccess = null
                            if (isSuccess && operationType == "RESTORE") {
                                // Trigger app restart on successful restore
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.finish()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isSuccess && operationType == "RESTORE") "Restart App" else "Done", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (restorePreview != null) {
                // Restore preview mode screen
                val preview = restorePreview!!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Backup Package Preview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PreviewItem(label = "Backup Date", value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(preview.backupDate)))
                            PreviewItem(label = "Device Info", value = preview.deviceModel)
                            PreviewItem(label = "App Version", value = preview.appVersion)
                            PreviewItem(label = "Database Version", value = "v${preview.databaseVersion}")
                            PreviewItem(label = "Total Records", value = preview.recordCounts.map { "${it.key}: ${it.value}" }.joinToString(", "))
                            PreviewItem(label = "Backup File Size", value = "${String.format("%.2f", preview.backupSize / 1024.0 / 1024.0)} MB")
                        }
                    }

                    Text(
                        text = "Restore Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mergeMode = false }
                                .border(
                                    width = if (!mergeMode) 2.dp else 1.dp,
                                    color = if (!mergeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = if (!mergeMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Replace All", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Deletes current records and replaces with backup data", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mergeMode = true }
                                .border(
                                    width = if (mergeMode) 2.dp else 1.dp,
                                    color = if (mergeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = if (mergeMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Merge Data", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Combines backup records with your current data safely", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                restorePreview = null
                                restoreFileToApply?.delete()
                                restoreFileToApply = null
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                isOperating = true
                                operationType = "RESTORE"
                                errorMessage = null
                                operationSuccess = null

                                coroutineScope.launch {
                                    val file = restoreFileToApply
                                    if (file != null) {
                                        val report = restoreManager.performRestore(file, mergeMode, progressTracker)
                                        restoreSummaryReport = report
                                        operationSuccess = report != null
                                        if (report == null) {
                                            errorMessage = "Restore verification or execution failed."
                                            isOperating = false
                                        } else {
                                            restoreManager.triggerCleanRestart()
                                        }
                                        file.delete()
                                    } else {
                                        errorMessage = "Internal file resolution error."
                                        operationSuccess = false
                                        isOperating = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isOperating
                        ) {
                            Text("Start Restore", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Actions selection main view
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BackupOptionCard(
                        title = "Create New Backup",
                        subtitle = "Saves your entire application state (profile, attendance database, settings, images) into a ZIP file.",
                        icon = Icons.Default.CloudUpload,
                        onClick = {
                            createBackupLauncher.launch("AttendanceBackup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip")
                        }
                    )

                    BackupOptionCard(
                        title = "Restore from Backup",
                        subtitle = "Import and parse standard Backup packages with atomic validations and rollbacks on failure.",
                        icon = Icons.Default.CloudDownload,
                        onClick = {
                            pickRestoreLauncher.launch(arrayOf("application/zip"))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ReportRow(label: String, detail: String, success: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (success) com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent() else com.synfusion.attendance.ui.theme.AttendanceStatusColors.getAbsent(),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun BackupOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreviewItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
