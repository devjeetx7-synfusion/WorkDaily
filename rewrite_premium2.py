import os

new_code = """package com.dailywork.attedance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

    // EXACT SAME PRICES as before
    val basePrice = 49
    val plans = listOf(
        Plan("monthly", "Monthly", basePrice),
        Plan("half-yearly", "6 Months", basePrice * 5, "Save 16%"),
        Plan("yearly", "Yearly", basePrice * 10, "Save 16%"),
        Plan("lifetime", "Lifetime", basePrice * 20, "Best Value")
    )

    var selectedPlanId by remember { mutableStateOf("lifetime") }
    val selectedPlan = plans.find { it.id == selectedPlanId } ?: plans.last()

    // Fixed App UI colors for premium
    val premiumPrimary = Color(0xFF22C55E) // Green 500
    val premiumBackground = Color.White
    val textPrimary = Color(0xFF1E293B) // Slate 800
    val textSecondary = Color(0xFF64748B) // Slate 500
    val premiumBadgeColor = Color(0xFFF59E0B) // Amber 500

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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // STRICTLY SEPARATE SECTIONS BASED ON ROLE
            if (isContractor) {
                // CONTRACTOR PREMIUM
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(premiumPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = premiumPrimary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Contractor Premium",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manage workers and payments with advanced contractor tools",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = premiumBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val contractorFeatures = listOf(
                                "Unlimited workers",
                                "Worker attendance history",
                                "Worker passbook export",
                                "Contractor statistics",
                                "Advance payment tracking",
                                "Worker overtime reports",
                                "Cloud backup"
                            )
                            contractorFeatures.forEach { feature ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = premiumPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(feature, fontSize = 14.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } else {
                // PERSONAL PREMIUM
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(premiumPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = premiumPrimary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Personal Premium",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unlock advanced tools for your personal work tracking",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = premiumBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val personalFeatures = listOf(
                                "Unlimited passbook history",
                                "PDF export",
                                "Cloud backup",
                                "Overtime history",
                                "Advanced statistics",
                                "Faster sync",
                                "Backup restore"
                            )
                            personalFeatures.forEach { feature ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = premiumPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(feature, fontSize = 14.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Plans - 2x2 Grid
            item {
                Text(
                    "Select your billing cycle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Using a Column of 2 Rows to simulate 2x2 grid simply
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PremiumPlanCardFixed(plans[0], selectedPlanId == plans[0].id, premiumPrimary, premiumBackground, textPrimary, premiumBadgeColor) { selectedPlanId = plans[0].id }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PremiumPlanCardFixed(plans[1], selectedPlanId == plans[1].id, premiumPrimary, premiumBackground, textPrimary, premiumBadgeColor) { selectedPlanId = plans[1].id }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            PremiumPlanCardFixed(plans[2], selectedPlanId == plans[2].id, premiumPrimary, premiumBackground, textPrimary, premiumBadgeColor) { selectedPlanId = plans[2].id }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PremiumPlanCardFixed(plans[3], selectedPlanId == plans[3].id, premiumPrimary, premiumBackground, textPrimary, premiumBadgeColor) { selectedPlanId = plans[3].id }
                        }
                    }
                }
            }

            // CTA Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = premiumBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Get ${selectedPlan.label} Plan", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Export your passbook and keep cloud backups safe", fontSize = 13.sp, color = textSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Implement Upgrade */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = premiumPrimary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Upgrade for ₹${selectedPlan.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumPlanCardFixed(
    plan: Plan,
    isSelected: Boolean,
    primaryColor: Color,
    bgColor: Color,
    textColor: Color,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryColor else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) primaryColor.copy(alpha = 0.05f) else bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (plan.tag != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(bottomStart = 8.dp, topEnd = 16.dp))
                        .background(if (plan.tag == "Best Value") badgeColor else primaryColor.copy(alpha=0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        plan.tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.tag == "Best Value") Color.White else primaryColor
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    plan.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹${plan.price}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = textColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
"""

with open('android-app/app/src/main/java/com/dailywork/attedance/ui/PremiumScreen.kt', 'w') as f:
    f.write(new_code)
