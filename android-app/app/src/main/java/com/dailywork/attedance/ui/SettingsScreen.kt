package com.dailywork.attedance.ui

import android.net.Uri
import android.widget.Toast
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.auth.FirebaseAuth
import com.dailywork.attedance.viewmodel.SettingsViewModel
import androidx.compose.material3.SnackbarHostState
import com.dailywork.attedance.ui.components.ShareBottomSheet

import com.dailywork.attedance.viewmodel.SettingsState
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

    var showHowToUseSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }
    var showDeveloperSheet by remember { mutableStateOf(false) }
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
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRoleBottomSheet by remember { mutableStateOf(false) }
    var showProfileEditSheet by remember { mutableStateOf(false) }
    var showRoleConfirmationDialog by remember { mutableStateOf(false) }

    var pendingRole by remember { mutableStateOf("") }
    val supportedLanguages = mapOf("en" to "English", "hi" to "हिंदी")

    val nameFocusRequester = remember { FocusRequester() }


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

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {

                        // Profile Image (App Logo)
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.dailywork.attedance.R.mipmap.ic_launcher),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(60.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            )
                        }

                            Box(modifier = Modifier.align(Alignment.BottomEnd).size(28.dp).offset(x = 4.dp, y = 4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary).border(2.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape).clickable { showProfileEditSheet = true }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp)) }
                        }
                        // Name
                        Text(text = state.name.ifEmpty { "User Name" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = FirebaseAuth.getInstance().currentUser?.email ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.dailyWage > 0) {
                            Text(text = "Daily Wage: ₹${state.dailyWage.toInt()}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        if (state.phone.isNotEmpty()) {
                            Text(text = "+91 ${state.phone}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                                    Text("Current: ${if (state.role.isNotEmpty()) state.role.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
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
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                Toast.makeText(context, "Generate PDF from worker views", Toast.LENGTH_SHORT).show()
                            }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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

                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Download WorkDaily: https://play.google.com/store/apps/details?id=com.dailywork.attedance")
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                            }
                        }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        Row(modifier = Modifier.fillMaxWidth().clickable { showHowToUseSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { showPrivacySheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { showTermsSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Terms & Conditions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text("Rules and guidelines for using the app", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(modifier = Modifier.fillMaxWidth().clickable { showDeveloperSheet = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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


            if (showProfileEditSheet) {
                ProfileEditBottomSheet(state = state, viewModel = viewModel, onDismiss = { showProfileEditSheet = false })
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
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(text = "Log Out?", fontWeight = FontWeight.Bold) },
                    text = { Text(text = "Are you sure you want to log out?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showLogoutDialog = false
                                viewModel.logout()
                                onLogout()
                            }
                        ) {
                            Text("Log Out", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )
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
                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.05f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("How to Use DailyWork", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "DailyWork helps users manage attendance, daily wages, advances, payments, dues, notes, and worker records.\n\n• Personal users can track their own attendance, work days, payments, and notes.\n• Contractors can manage workers/helpers/mistry records, attendance, advance, half payment, full payment, overtime, dues, and history.\n\nPassbook and reports help users review records clearly.\nPremium features may unlock advanced reports, PDF/share options, or extended worker history if enabled in the app.",
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
                text = "DailyWork respects your privacy and is designed to help users manage work attendance, worker records, payments, dues, notes, and reports securely.\n\nInformation We May Store:\n• Name, email, phone number, wage details, attendance records, worker details, payment records, advances, dues, notes, and app preferences.\n• Login/account information required for authentication.\n• Premium or subscription status if premium features are used.\n\nHow We Use Information:\n• To show your attendance, worker records, payment details, passbooks, and reports inside the app.\n• To sync your data across devices when cloud features are enabled.\n• To provide support, bug fixes, and app improvements.\n\nData Safety:\n• User data should not be sold.\n• Sensitive user records should only be used for app functionality.\n• Users are responsible for entering accurate worker/payment information.\n\nSupport & Bug Reports:\nFor help, bug reports, privacy questions, or account support, contact:\nworkdaily.synfusion@gmail.com\n\nBrand:\nSynFusion",
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
                Box(modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Terms & Conditions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "By using DailyWork, you agree to use the app responsibly for managing attendance, wages, payments, worker details, notes, dues, passbooks, and related work records.\n\nUser Responsibility:\n• Users are responsible for the accuracy of attendance, wage, payment, worker, and due records entered in the app.\n• DailyWork is a record-management tool and does not verify real-world payments or worker attendance automatically.\n• Users should review records before sharing passbooks, reports, or PDFs.\n\nPremium Features:\n• Some features may require premium access.\n• Premium availability, pricing, and features may change depending on app configuration.\n• If Google Play Billing is used, purchases are handled through Google Play policies.\n\nLimitations:\n• The app is provided as a productivity and record-management tool.\n• SynFusion is not responsible for incorrect entries, payment disputes, business losses, or misuse of generated records.\n\nSupport:\nFor help, bug reports, or account-related questions, contact:\nworkdaily.synfusion@gmail.com\n\nBrand:\nSynFusion",
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.dailywork.attedance.R.mipmap.ic_launcher),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(60.dp).clip(androidx.compose.foundation.shape.CircleShape)
                )
            }

            Text("SynFusion", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("workdaily.synfusion@gmail.com", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Instagram
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFFE1306C).copy(alpha=0.1f))
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/devjeetx7")))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No app found to handle link", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Instagram", tint = Color(0xFFE1306C))
                }

                // Telegram
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFF0088cc).copy(alpha=0.1f))
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/synfusion_coder")))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No app found to handle link", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Telegram", tint = Color(0xFF0088cc))
                }

                // Gmail
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color(0xFFD44638).copy(alpha=0.1f))
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:devjeetx7@gmail.com")))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "No app found to handle email", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFFD44638))
                }
            }

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
fun ProfileEditBottomSheet(
    state: SettingsState,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
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
                label = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.full_name)) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

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
                        viewModel.saveChanges()
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
