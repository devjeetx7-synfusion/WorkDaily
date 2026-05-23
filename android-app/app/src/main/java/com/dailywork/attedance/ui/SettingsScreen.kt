package com.dailywork.attedance.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import coil.compose.AsyncImage
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.dailywork.attedance.ui.MainActivity
import com.dailywork.attedance.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onNavigateToPremium: () -> Unit = {},
    onNavigateToWorkerHistory: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.triggerRestart) {
        if (state.triggerRestart) {
            // Give a short delay for the Toast/Snackbar to be seen if desired,
            // but the user wants immediate restart
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
            (context as? android.app.Activity)?.finish()
        }
    }

    var showHowToUseBottomSheet by remember { mutableStateOf(false) }
    var showPrivacyBottomSheet by remember { mutableStateOf(false) }
    var showDeveloperBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showRoleBottomSheet by remember { mutableStateOf(false) }
    var showRoleConfirmationDialog by remember { mutableStateOf(false) }
    var pendingRole by remember { mutableStateOf("") }
    val supportedLanguages = mapOf("en" to "English", "hi" to "हिंदी")

    val hasChanges = state.name != state.originalName ||
            state.dailyWage != state.originalWage ||
            state.role != state.originalRole ||
            state.phone != state.originalPhone ||
            state.profileImageUri != null

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.settings), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }

                // Profile Settings
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.profile_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                        AnimatedVisibility(visible = hasChanges) {
                            IconButton(onClick = { viewModel.saveChanges() }, enabled = !state.isSaving) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = "Save Changes", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                        // Profile Image (Role-Based Icon)
                        val personalIcons = listOf(Icons.Default.Construction, Icons.Default.FormatPaint, Icons.Default.Build, Icons.Default.Plumbing)
                        val randomPersonalIcon = remember(state.role) { personalIcons.random() }

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.role == "contractor") {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                            } else {
                                Icon(randomPersonalIcon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Name
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.onNameChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.full_name)) },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Email
                        OutlinedTextField(
                            value = FirebaseAuth.getInstance().currentUser?.email ?: "",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            label = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.email)) },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Daily Wage
                        OutlinedTextField(
                            value = if (state.dailyWage > 0) state.dailyWage.toInt().toString() else "",
                            onValueChange = { viewModel.onWageChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.daily_wage)) },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Phone Number
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }.take(10)
                                viewModel.onPhoneChange(digits)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.phone_number)) },
                            leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // General App Settings
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.app_preferences), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.theme), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Switch(checked = state.isDarkMode, onCheckedChange = { viewModel.toggleTheme(it) })
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.reminders), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.daily_attendance_reminder), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(checked = state.isRemindersEnabled, onCheckedChange = { viewModel.toggleReminders(it) })
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.enable_splash_screen), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Switch(checked = state.isSplashEnabled, onCheckedChange = { viewModel.toggleSplash(it) })
                        }
                        if (state.isLanguageEnabled && state.isLanguageActivityEnabled) {
                            Divider(color = MaterialTheme.colorScheme.outline)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth().clickable {
                                    val intent = android.content.Intent(context, LanguageSelectorActivity::class.java)
                                    context.startActivity(intent)
                                }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Blue.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Language, contentDescription = null, tint = Color.Blue)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.language_2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text(supportedLanguages[state.language] ?: "English", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (state.isRoleUiEnabled) {
                            Divider(color = MaterialTheme.colorScheme.outline)
                            Row(modifier = Modifier.fillMaxWidth().clickable { showRoleBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.role_management), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Current: ${state.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { if (!state.isPremium) onNavigateToPremium() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (state.isPremium) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEAB308).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    if (state.isPremium) {
                                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF10B981))
                                    } else {
                                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFEAB308))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    if (state.isPremium) {
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.dailywork_premium), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.thank_you_for_your_purchase), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.upgrade_to_premium), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.unlock_all_features), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (!state.isPremium) {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Data & Export
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.data_export), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (state.role == "contractor") {
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToWorkerHistory() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.worker_history), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.attendance_payment_logs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Divider(color = MaterialTheme.colorScheme.outline)
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.export_as_pdf), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.only_contractor_premium), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Divider(color = MaterialTheme.colorScheme.outline)
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF25D366).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF25D366))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.share_via_whatsapp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.send_digital_passbook), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Data & Support
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.data_support), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().clickable { showHowToUseBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.05f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.how_to_use_dailywork), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.quick_guide_tutorials), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { showPrivacyBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.data_privacy_policy), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.stored_securely_in_cloud), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { showDeveloperBottomSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF16A34A).copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF16A34A))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.developer_contact), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.reach_out_for_feedback), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (com.dailywork.attedance.BuildConfig.DEBUG) {
                Button(
                    onClick = { throw RuntimeException("Test Crash - Crashlytics Verification") },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha=0.1f), contentColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Test Crash", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.1f), contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.log_out), fontWeight = FontWeight.Bold) },
                text = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.are_you_sure_you_want_to_log_out_of_your_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.log_out), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cancel))
                    }
                }
            )
        }

        if (showRoleConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showRoleConfirmationDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.change_role), fontWeight = FontWeight.Bold) },
                text = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.changing_role_will_refresh_the_app_and_s_msg)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showRoleConfirmationDialog = false
                            viewModel.onRoleChange(pendingRole)
                            viewModel.saveChanges()
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRoleConfirmationDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cancel))
                    }
                }
            )
        }

        if (showHowToUseBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showHowToUseBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.how_to_use_dailywork), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.a_quick_guide_to_tracking_your_work_effe_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.num_1_marking_attendance), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.on_the_dashboard_click_full_day_or_half__msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.num_2_net_payable_earnings), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.your_earnings_are_automatically_calculat_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.num_3_advance_payments), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.if_you_receive_money_ahead_of_time_click_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.num_4_calendar_history), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.use_the_calendar_to_edit_past_records_eg_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showPrivacyBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPrivacyBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.data_privacy_policy), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.stored_securely_in_the_cloud), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.data_collection), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.we_collect_your_name_phone_number_and_at_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.storage_method), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.your_data_is_securely_stored_on_firebase_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.security_practices), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.access_to_your_data_is_restricted_to_you_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.user_control), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.you_can_delete_your_data_by_deleting_you_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showDeveloperBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDeveloperBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.developer_contact), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.reach_out_to_us_for_any_feedback_or_supp_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.supportdailyworkcom), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.num_91_98765_43210), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.wwwdailyworkcom), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        if (showRoleBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRoleBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.role_management), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.switch_between_contractor_and_personal_m_msg), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Divider(color = MaterialTheme.colorScheme.outline)

                    listOf("contractor", "personal").forEach { roleOption ->
                        val isSelected = state.role == roleOption
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isSelected) {
                                        pendingRole = roleOption
                                        showRoleConfirmationDialog = true
                                    }
                                    showRoleBottomSheet = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = roleOption.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
