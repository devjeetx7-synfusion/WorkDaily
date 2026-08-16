package com.synfusion.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*

import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.responsive.ResponsiveContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDailyAppBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.primary
        ),
        windowInsets = WindowInsets(0.dp)
    )
}

@Composable
fun PrivacyHeaderIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "privacy_illustration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TermsHeaderIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "terms_illustration")
    val translateY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translateY"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                translationY = translateY
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(56.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
                .size(22.dp)
                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.background, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            WorkDailyAppBar(
                title = stringResource(R.string.how_to_use),
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                TutorialStepCard(
                    stepNumber = "1",
                    title = stringResource(R.string.htu_getting_started_title),
                    description = stringResource(R.string.htu_getting_started_desc),
                    icon = Icons.Default.Login
                ) { GettingStartedIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "2",
                    title = stringResource(R.string.htu_attendance_title),
                    description = stringResource(R.string.htu_attendance_desc),
                    icon = Icons.Default.CheckCircle
                ) { AttendanceIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "3",
                    title = stringResource(R.string.htu_calendar_title),
                    description = stringResource(R.string.htu_calendar_desc),
                    icon = Icons.Default.DateRange
                ) { CalendarIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "4",
                    title = stringResource(R.string.htu_earnings_title),
                    description = stringResource(R.string.htu_earnings_desc),
                    icon = Icons.Default.AttachMoney
                ) { EarningsIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "5",
                    title = stringResource(R.string.overtime_title),
                    description = stringResource(R.string.overtime_desc),
                    icon = Icons.Default.AccessTime
                ) { OvertimeIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "6",
                    title = stringResource(R.string.advance_title),
                    description = stringResource(R.string.advance_desc),
                    icon = Icons.Default.AccountBalanceWallet
                ) { AdvanceIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "7",
                    title = stringResource(R.string.htu_passbook_title),
                    description = stringResource(R.string.htu_passbook_desc),
                    icon = Icons.Default.AccountBalanceWallet
                ) { PassbookIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "8",
                    title = stringResource(R.string.htu_all_time_passbook_title),
                    description = stringResource(R.string.htu_all_time_passbook_desc),
                    icon = Icons.Default.History
                ) { AllTimePassbookIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "9",
                    title = stringResource(R.string.htu_statistics_title),
                    description = stringResource(R.string.htu_statistics_desc),
                    icon = Icons.Default.BarChart
                ) { StatisticsIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "10",
                    title = stringResource(R.string.htu_pdf_export_title),
                    description = stringResource(R.string.htu_pdf_export_desc),
                    icon = Icons.Default.PictureAsPdf
                ) { PdfGenerationIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "11",
                    title = stringResource(R.string.htu_backup_restore_title),
                    description = stringResource(R.string.htu_backup_restore_desc),
                    icon = Icons.Default.SettingsBackupRestore
                ) { BackupRestoreIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "12",
                    title = stringResource(R.string.htu_language_title),
                    description = stringResource(R.string.htu_language_desc),
                    icon = Icons.Default.Language
                ) { LanguageIllustration() }
            }

            item {
                TutorialStepCard(
                    stepNumber = "13",
                    title = stringResource(R.string.htu_settings_title),
                    description = stringResource(R.string.htu_settings_desc),
                    icon = Icons.Default.Settings
                ) { SettingsIllustration() }
            }


            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        }
    }
}

@Composable
fun TutorialStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    illustration: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3D / Depth UI Container for Native Compose Animations
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                illustration()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            WorkDailyAppBar(
                title = stringResource(R.string.privacy_policy),
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrivacyHeaderIllustration()

                    Text(
                        text = stringResource(R.string.privacy_intro),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            item {
                LegalSectionCard(
                    title = stringResource(R.string.privacy_local_data_title),
                    description = stringResource(R.string.privacy_local_data_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Storage
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.privacy_user_sharing_title),
                    description = stringResource(R.string.privacy_user_sharing_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Share
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.privacy_third_party_title),
                    description = stringResource(R.string.privacy_third_party_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Security
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.privacy_contact_title),
                    description = stringResource(R.string.privacy_contact_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Email
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            WorkDailyAppBar(
                title = stringResource(R.string.terms_conditions),
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TermsHeaderIllustration()

                    Text(
                        text = stringResource(R.string.rules_guidelines),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_acceptance_title),
                    description = stringResource(R.string.terms_acceptance_desc),
                    icon = androidx.compose.material.icons.Icons.Default.CheckCircle
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_use_title),
                    description = stringResource(R.string.terms_use_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Settings
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_responsibilities_title),
                    description = stringResource(R.string.terms_responsibilities_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Person
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_financial_title),
                    description = stringResource(R.string.terms_financial_desc),
                    icon = androidx.compose.material.icons.Icons.Default.AttachMoney
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_backup_title),
                    description = stringResource(R.string.terms_backup_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Backup
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_branding_title),
                    description = stringResource(R.string.terms_branding_desc),
                    icon = androidx.compose.material.icons.Icons.Default.Business
                )
            }
            item {
                LegalSectionCard(
                    title = stringResource(R.string.terms_contact_title),
                    description = stringResource(R.string.terms_contact_desc),
                    icon = androidx.compose.material.icons.Icons.Default.ContactMail
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        }
    }
}

@Composable
fun LegalSectionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    val parts = remember(title) { title.split(". ", limit = 2) }
    val sectionNumber = if (parts.size == 2) parts[0] else null
    val sectionTitleText = if (parts.size == 2) parts[1] else title

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        border = if (expanded) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (expanded) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (sectionNumber != null) {
                        Text(
                            text = "SECTION $sectionNumber",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = sectionTitleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = rotationState
                        }
                        .size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            letterSpacing = 0.25.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GettingStartedIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Box(modifier = Modifier.width(120.dp).height(12.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.width(80.dp).height(8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.daily_wage), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹ 500", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AttendanceIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val selectedIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 2,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val options = listOf(
        Pair(stringResource(R.string.present_1), Color(0xFF4CAF50)),
        Pair(stringResource(R.string.half_day), Color(0xFFFF9800)),
        Pair(stringResource(R.string.absent_1), Color(0xFFF44336))
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, (label, color) ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun EarningsIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val value by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12500f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.gross_earned), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "₹ ${value.toInt()}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("+ ₹500 OT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
            Box(modifier = Modifier.background(Color(0xFFF44336).copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("- ₹200 Adv", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun OvertimeIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val value by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("2 " + stringResource(R.string.overtime_hours), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "+ ₹ ${value.toInt()}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
fun AdvanceIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val value by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.advance), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "- ₹ ${value.toInt()}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF44336)
        )
    }
}

@Composable
fun CalendarIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(5) { i ->
                Text("D${i+1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(5) { i ->
                val isSelected = i == 2
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(if (isSelected) pulse else 1f)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (i == 1) Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape))
                    if (i == 3) Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF9800), androidx.compose.foundation.shape.CircleShape))
                    if (isSelected) Text("15", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun PassbookIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val slideOffset by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(2) { i ->
            Card(
                modifier = Modifier.fillMaxWidth().offset(y = if (i == 0) slideOffset.dp else (-slideOffset).dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (i==0) "15 Oct" else "14 Oct", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(if (i==0) stringResource(R.string.present_1) else stringResource(R.string.half_day), style = MaterialTheme.typography.labelSmall, color = if(i==0) Color(0xFF4CAF50) else Color(0xFFFF9800))
                    }
                    Text(if (i==0) "₹ 500" else "₹ 250", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun AllTimePassbookIllustration() {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(1500)
            expanded = !expanded
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("October 2023", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                }
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("₹ 12,500", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 6.dp
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(80.dp).height(8.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.width(60.dp).height(8.dp).background(Color(0xFF4CAF50), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            Box(modifier = Modifier.width(40.dp).height(8.dp).background(Color(0xFFFF9800), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
fun PdfGenerationIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val translationY by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(y = translationY.dp)
                .background(Color(0xFFE53935).copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFFE53935), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { 1f - (translationY / 20f) },
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BackupRestoreIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val isUploading by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.offset(x = ((isUploading - 0.5f) * 60).dp).size(20.dp))
        }
        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun LanguageIllustration() {
    var isHindi by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            delay(1500)
            isHindi = !isHindi
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Box(modifier = Modifier.background(if (!isHindi) MaterialTheme.colorScheme.primary else Color.Transparent, androidx.compose.foundation.shape.CircleShape).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("A", style = MaterialTheme.typography.labelLarge, color = if (!isHindi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.background(if (isHindi) MaterialTheme.colorScheme.primary else Color.Transparent, androidx.compose.foundation.shape.CircleShape).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("अ", style = MaterialTheme.typography.labelLarge, color = if (isHindi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SettingsIllustration() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.graphicsLayer { rotationZ = rotation })
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.width(80.dp).height(12.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            }
            Switch(checked = true, onCheckedChange = null, modifier = Modifier.scale(0.8f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.width(60.dp).height(12.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            }
            Switch(checked = false, onCheckedChange = null, modifier = Modifier.scale(0.8f))
        }
    }
}
