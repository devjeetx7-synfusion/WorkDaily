package com.dailywork.attedance.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.billingclient.api.ProductDetails
import com.dailywork.attedance.R
import com.dailywork.attedance.ui.premium.PremiumViewModel
import com.dailywork.attedance.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    premiumViewModel: PremiumViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val uiState by premiumViewModel.uiState.collectAsState()
    val dashboardState by dashboardViewModel.dashboardState.collectAsState()
    val isContractor = dashboardState.role == "contractor"
    val context = LocalContext.current

    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val premiumGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dailywork_premium), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Premium Header Card
            item {
                PremiumHeaderCard(premiumGradient, uiState.isPremium)
            }

            if (uiState.isPremium) {
                item {
                    SubscriptionActiveCard(uiState.premiumType, onManage = { premiumViewModel.manageSubscription(context) })
                }
            }

            // Features Section
            item {
                PremiumFeaturesList(isContractor, textPrimary, textSecondary)
            }

            // Pricing Plans
            if (!uiState.isPremium) {
                item {
                    Text(
                        stringResource(R.string.premium_choose_plan),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Crossfade(targetState = uiState.isLoading, label = "loading_crossfade") { isLoading ->
                        if (isLoading) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                repeat(3) {
                                    LoadingSkeleton()
                                }
                            }
                        } else if (uiState.error != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                    Text(
                                        uiState.error ?: stringResource(R.string.premium_plans_unavailable),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(onClick = { premiumViewModel.refreshProducts() }) {
                                        Text(stringResource(R.string.premium_retry))
                                    }
                                }
                            }
                        } else if (uiState.isFallbackMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.fallbackProducts.forEach { product ->
                                    PremiumPlanItemFallback(
                                        product = product,
                                        primaryColor = primaryColor,
                                        onClick = { /* Handle fallback click */ }
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.products.sortedBy { it.productId }.forEach { product ->
                                    PremiumPlanItem(
                                        product = product,
                                        isSelected = false,
                                        primaryColor = primaryColor,
                                        onClick = { premiumViewModel.buyProduct(context as Activity, product) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Restore Purchases
            item {
                TextButton(
                    onClick = { premiumViewModel.restorePurchases() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.premium_restore_purchases), color = primaryColor)
                }
            }

            // Trust Badges
            item {
                TrustBadges(textSecondary)
            }
        }
    }
}

@Composable
fun PremiumHeaderCard(gradient: Brush, isPremium: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(gradient)) {
            Column(
                modifier = Modifier.padding(24.dp).align(Alignment.CenterStart)
            ) {
                Text(
                    text = if (isPremium) stringResource(R.string.premium_you_are_pro) else stringResource(R.string.premium_go_premium),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isPremium) stringResource(R.string.premium_enjoy_features) else stringResource(R.string.premium_unlock_potential),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp)
                    .graphicsLayer(alpha = 0.2f),
                tint = Color.White
            )
        }
    }
}

@Composable
fun SubscriptionActiveCard(type: String?, onManage: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.premium_active_subscription), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stringResource(R.string.premium_plan_label, type?.replaceFirstChar { it.uppercase() } ?: "Premium"), fontSize = 14.sp)
            }
            Button(onClick = onManage, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.premium_manage_subscription))
            }
        }
    }
}

@Composable
fun PremiumFeaturesList(isContractor: Boolean, textPrimary: Color, textSecondary: Color) {
    val features = if (isContractor) {
        listOf(
            "Unlimited workers management",
            "Full worker history & reports",
            "Advanced export to PDF/Excel",
            "Cloud sync & backup",
            "No advertisements",
            "Priority support"
        )
    } else {
        listOf(
            "Unlimited attendance tracking",
            "Overtime & Wage analytics",
            "PDF Passbook generation",
            "Auto-backup to Cloud",
            "Multiple device sync",
            "Personalized reports"
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(feature, color = textPrimary, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun PremiumPlanItem(
    product: ProductDetails,
    isSelected: Boolean,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val price = if (product.productType == "subs") {
        product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
    } else {
        product.oneTimePurchaseOfferDetails?.formattedPrice
    }

    val title = when (product.productId) {
        "workdaily_premium_monthly" -> stringResource(R.string.premium_monthly_plan)
        "workdaily_premium_yearly" -> stringResource(R.string.premium_yearly_plan)
        "workdaily_premium_lifetime" -> stringResource(R.string.premium_lifetime_access)
        else -> product.name
    }

    val tag = when (product.productId) {
        "workdaily_premium_yearly" -> stringResource(R.string.premium_best_value)
        "workdaily_premium_lifetime" -> stringResource(R.string.premium_one_time)
        else -> null
    }

    val isBestValue = product.productId == "workdaily_premium_yearly"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(if (isBestValue) 2.dp else 1.dp, if (isBestValue) primaryColor else Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Column {
                if (tag != null) {
                    Text(
                        tag,
                        color = primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = if (product.productId == "workdaily_premium_monthly") stringResource(R.string.premium_billed_monthly) else if (product.productId == "workdaily_premium_yearly") stringResource(R.string.premium_billed_annually) else stringResource(R.string.premium_pay_once),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    price ?: "--",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = primaryColor
                )
                if (isBestValue) {
                    Text(
                        stringResource(R.string.premium_save_30),
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumPlanItemFallback(
    product: com.dailywork.attedance.ui.premium.FallbackProduct,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val isBestValue = product.tag == "Best Value"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(if (isBestValue) 2.dp else 1.dp, if (isBestValue) primaryColor else Color.LightGray.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.tag != null) {
                        Text(
                            product.tag,
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        "Test Mode",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (product.tag != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(product.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = product.description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Column(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    product.price,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = primaryColor
                )
                if (isBestValue) {
                    Text(
                        "Save ~33%",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.LightGray.copy(alpha = alpha))
            .padding(vertical = 4.dp)
    )
}

@Composable
fun TrustBadges(textSecondary: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = textSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.premium_secure_payment), fontSize = 12.sp, color = textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = textSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.premium_cancel_anytime), fontSize = 12.sp, color = textSecondary)
            }
        }
        Text(
            stringResource(R.string.premium_google_play_secure),
            fontSize = 11.sp,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
