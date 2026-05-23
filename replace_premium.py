import os

new_code = """package com.dailywork.attedance.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailywork.attedance.viewmodel.DashboardViewModel

data class Plan(val id: String, val label: String, val price: Int, val tag: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel
) {
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val isContractor = dashboardState.role == "contractor"

    val basePrice = if (isContractor) 99 else 49

    val plans = listOf(
        Plan("monthly", "Monthly", basePrice),
        Plan("half-yearly", "6 Months", basePrice * 5, "Save 16%"),
        Plan("yearly", "Yearly", basePrice * 10, "Save 16%"),
        Plan("lifetime", "Lifetime", basePrice * 20, "Best Value")
    )

    var selectedPlanId by remember { mutableStateOf("lifetime") }
    val selectedPlan = plans.find { it.id == selectedPlanId } ?: plans.last()

    // Core App UI Colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val premiumBadgeColor = Color(0xFFF59E0B) // Amber

    val personalFeatures = listOf(
        "Unlimited passbook history",
        "PDF export",
        "Cloud backup",
        "Overtime history",
        "Advanced statistics",
        "Faster sync",
        "Backup restore"
    )

    val contractorFeatures = listOf(
        "Unlimited workers",
        "Worker attendance history",
        "Worker passbook export",
        "Contractor statistics",
        "Advance payment tracking",
        "Worker overtime reports",
        "Cloud backup"
    )

    val currentFeatures = if (isContractor) contractorFeatures else personalFeatures

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.dailywork_premium), fontWeight = FontWeight.Bold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Header Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Unlock advanced tools for smarter work management",
                        fontSize = 15.sp,
                        color = textSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Features Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = borderStroke(1.dp, outlineColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Premium Features",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        currentFeatures.forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(feature, fontSize = 14.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Billing Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Your Plan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumPlanCardClean(plans[0], selectedPlanId == plans[0].id, primaryColor, surfaceColor, textPrimary, textSecondary, outlineColor, premiumBadgeColor) { selectedPlanId = plans[0].id }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumPlanCardClean(plans[1], selectedPlanId == plans[1].id, primaryColor, surfaceColor, textPrimary, textSecondary, outlineColor, premiumBadgeColor) { selectedPlanId = plans[1].id }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumPlanCardClean(plans[2], selectedPlanId == plans[2].id, primaryColor, surfaceColor, textPrimary, textSecondary, outlineColor, premiumBadgeColor) { selectedPlanId = plans[2].id }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumPlanCardClean(plans[3], selectedPlanId == plans[3].id, primaryColor, surfaceColor, textPrimary, textSecondary, outlineColor, premiumBadgeColor) { selectedPlanId = plans[3].id }
                            }
                        }
                    }
                }
            }

            // CTA Bottom Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = borderStroke(1.dp, outlineColor.copy(alpha=0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Get ${selectedPlan.label} Plan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Secure your data with export and cloud backup", fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { /* Implement Upgrade */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Upgrade for ₹${selectedPlan.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.cancel_anytime_no_hidden_fees),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumPlanCardClean(
    plan: Plan,
    isSelected: Boolean,
    primaryColor: Color,
    bgColor: Color,
    textColor: Color,
    textSecondary: Color,
    outlineColor: Color,
    badgeColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(if (isSelected) primaryColor else outlineColor)
    val cardBgColor by animateColorAsState(if (isSelected) primaryColor.copy(alpha = 0.04f) else bgColor)
    val elevation by animateDpAsState(if (isSelected) 4.dp else 1.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // Selection Check Circle inside corner
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {

                // Best Value Badge
                if (plan.tag == "Best Value") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            plan.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else if (plan.tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha=0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            plan.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    plan.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹${plan.price}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = textColor)
                }
            }
        }
    }
}

@Composable
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
"""

with open('android-app/app/src/main/java/com/dailywork/attedance/ui/PremiumScreen.kt', 'w') as f:
    f.write(new_code)
