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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToUseScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.how_to_use), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "2",
                    title = stringResource(R.string.htu_attendance_title),
                    description = stringResource(R.string.htu_attendance_desc),
                    icon = Icons.Default.CheckCircle
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "3",
                    title = stringResource(R.string.htu_calendar_title),
                    description = stringResource(R.string.htu_calendar_desc),
                    icon = Icons.Default.DateRange
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "4",
                    title = stringResource(R.string.htu_passbook_title),
                    description = stringResource(R.string.htu_passbook_desc),
                    icon = Icons.Default.AccountBalanceWallet
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "5",
                    title = stringResource(R.string.htu_all_time_passbook_title),
                    description = stringResource(R.string.htu_all_time_passbook_desc),
                    icon = Icons.Default.History
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "6",
                    title = stringResource(R.string.htu_statistics_title),
                    description = stringResource(R.string.htu_statistics_desc),
                    icon = Icons.Default.BarChart
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "7",
                    title = stringResource(R.string.htu_pdf_export_title),
                    description = stringResource(R.string.htu_pdf_export_desc),
                    icon = Icons.Default.PictureAsPdf
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "8",
                    title = stringResource(R.string.htu_backup_restore_title),
                    description = stringResource(R.string.htu_backup_restore_desc),
                    icon = Icons.Default.SettingsBackupRestore
                )
            }

            item {
                TutorialStepCard(
                    stepNumber = "9",
                    title = stringResource(R.string.htu_language_title),
                    description = stringResource(R.string.htu_language_desc),
                    icon = Icons.Default.Language
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun TutorialStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Simulated Phone Frame Placeholder (in lieu of actual images/illustrations for now)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Illustration for Step $stepNumber",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_policy), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyLarge)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_conditions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
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



@Composable
fun LegalSectionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
