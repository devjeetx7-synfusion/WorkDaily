package com.synfusion.attendance.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.synfusion.attendance.backup.RestoreManager
import com.synfusion.attendance.backup.ProgressTracker
import com.synfusion.attendance.backup.BackupManifest
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.viewmodel.OfflineAuthViewModel
import com.synfusion.attendance.viewmodel.OfflineLoginState
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

    if (restorePreview != null) {
        val preview = restorePreview!!
        AlertDialog(
            onDismissRequest = {
                restorePreview = null
                restoreFileToApply?.delete()
                restoreFileToApply = null
            },
            title = { Text("Backup Package Preview", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A backup package was parsed. Preview details:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Backup Date: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(preview.backupDate))}", fontSize = 13.sp)
                    Text("• Device Info: ${preview.deviceModel}", fontSize = 13.sp)
                    Text("• App Version: ${preview.appVersion}", fontSize = 13.sp)
                    Text("• Records: ${preview.recordCounts.map { "${it.key}: ${it.value}" }.joinToString(", ")}", fontSize = 13.sp)
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
                                    val report = restoreManager.performRestore(file, false, progressTracker)
                                    if (report != null) {
                                        operationSuccess = true
                                        // Auto-login since the backup is restored!
                                        com.synfusion.attendance.data.AttendanceDatabase.resetInstance()
                                        authViewModel.checkForUserAndLogin()
                                    } else {
                                        errorMessage = "Failed to restore backup database."
                                        operationSuccess = false
                                    }
                                    file.delete()
                                } else {
                                    errorMessage = "Internal backup file error."
                                    operationSuccess = false
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Restore process failed."
                                operationSuccess = false
                            } finally {
                                isOperating = false
                            }
                        }
                    }
                ) {
                    Text("Restore Now")
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
                    Text("Cancel")
                }
            }
        )
    }

    if (showSmartBackupDialog && detectedBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showSmartBackupDialog = false },
            title = { Text("Backup Found", fontWeight = FontWeight.Bold) },
            text = { Text("A previous backup was detected.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSmartBackupDialog = false
                        val file = detectedBackupFile
                        if (file != null) {
                            isOperating = true
                            scope.launch {
                                try {
                                    val report = restoreManager.performRestore(file, false, progressTracker)
                                    if (report != null) {
                                        operationSuccess = true
                                        com.synfusion.attendance.data.AttendanceDatabase.resetInstance()
                                        authViewModel.checkForUserAndLogin()
                                    } else {
                                        errorMessage = "Failed to restore backup database."
                                        operationSuccess = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Restore process failed."
                                    operationSuccess = false
                                } finally {
                                    isOperating = false
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartBackupDialog = false }) {
                    Text("Skip")
                }
            }
        )
    }

    if (isOperating) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restoring Backup...", fontWeight = FontWeight.Bold) },
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
            title = { Text("Restore Failed", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "An unexpected error occurred during the restore.") },
            confirmButton = {
                Button(onClick = { operationSuccess = null }) {
                    Text("OK")
                }
            }
        )
    }

    val startText = when (selectedLanguage) {
        "hi" -> "स्थानीय कार्यक्षेत्र शुरू करें"
        else -> "Create Offline Workspace"
    }

    LaunchedEffect(loginState) {
        if (loginState is OfflineLoginState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // CENTER AREA: SaaS Feature Showcase
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SaaSFeatureShowcase()
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
                    placeholder = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        val digits = newValue.filter { it.isDigit() }.take(10)
                        phone = digits
                    },
                    placeholder = { Text("Phone Number (10 digits) *") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.length >= 10) {
                            authViewModel.login(name, email, phone)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .pressScaleEffect(),
                    shape = RoundedCornerShape(20.dp),
                    enabled = name.isNotBlank() && phone.length >= 10 && !isProcessing
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
                    text = "100% Private & Offline Attendance",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.width(180.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Already have a Backup?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Restore Backup",
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

@Composable
fun SaaSFeatureShowcase() {
    var currentState by remember { mutableIntStateOf(0) }
    val totalStates = 4

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            currentState = (currentState + 1) % totalStates
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        contentAlignment = Alignment.Center
    ) {
        val glowColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(300.dp)
                .alpha(0.2f)
                .blur(100.dp)
                .background(glowColor.copy(alpha = 0.15f), CircleShape)
        )

        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(700, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(700)))
                    .togetherWith(fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 1.02f, animationSpec = tween(500)))
            },
            label = "saas_feature_transition"
        ) { state ->
            when (state) {
                0 -> PayoutsWidget()
                1 -> AttendanceStatsWidget()
                2 -> PdfReportWidget()
                3 -> AdvanceTrackWidget()
            }
        }
    }
}

@Composable
fun SaaSWidget(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )

    Surface(
        modifier = modifier
            .graphicsLayer { this.translationY = translationY }
            .width(280.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                .shadow(
                    elevation = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) 20.dp else 0.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.05f),
                    ambientColor = Color.Black.copy(alpha = 0.05f)
                )
                .padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
fun PayoutsWidget() {
    SaaSWidget {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Payments, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Total Payouts", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            CountingText("₹ 1,28,450", MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("12% increase from last month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AttendanceStatsWidget() {
    SaaSWidget {
        Column {
            Text("Attendance Stats", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 0.75f)
                heights.forEachIndexed { i, h ->
                    val animatedHeight = animateFloatAsState(targetValue = h, animationSpec = tween(1000, delayMillis = i * 100), label = "bar").value
                    Box(modifier = Modifier.width(18.dp).fillMaxHeight(animatedHeight * 0.5f).clip(RoundedCornerShape(6.dp)).background(if (i == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Present", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("94%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Absent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("6%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PdfReportWidget() {
    SaaSWidget {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Monthly Passbook", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("Ready to export", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdvanceTrackWidget() {
    SaaSWidget {
        Column {
            Text("Advance Tracking", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)).padding(12.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary), contentAlignment = Alignment.Center) {
                    Text("R", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rahul Kumar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Pending Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹ 2,400", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Manage credits with ease", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CountingText(text: String, style: androidx.compose.ui.text.TextStyle, color: Color) {
    Text(text = text, style = style, color = color, fontWeight = FontWeight.Black)
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
