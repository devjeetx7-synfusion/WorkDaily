package com.synfusion.attendance.rewards.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.*

// Colors
val RewardsPurple = Color(0xFF7C4DFF)
val RewardsPurpleContainer = Color(0xFFF3E8FF)
val RewardsGold = Color(0xFFFFB300)
val StatusGreen = Color(0xFF2E7D32)
val StatusOrange = Color(0xFFEF6C00)
val StatusRed = Color(0xFFC62828)

@Composable
fun RewardsSectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RewardsPurple,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun CoinAmount(
    amount: Int,
    fontSize: Int = 18,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    showSign: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayStr = if (showSign && amount > 0) "+$amount" else "$amount"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(text = "🪙", fontSize = (fontSize * 0.9).sp)
        Text(
            text = displayStr,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun RewardStatusChip(
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = statusText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
fun RewardsBalanceCard(
    availableCoins: Int,
    pendingCoins: Int,
    onViewWallet: () -> Unit,
    onRedeem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsPurpleContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "MY WORKDAILY COINS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = RewardsPurple
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🪙", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%,d", availableCoins + pendingCoins),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1F1047)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Available", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = String.format("%,d", availableCoins), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                }
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(Color.LightGray)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Pending", fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = String.format("%,d", pendingCoins), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewWallet,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RewardsPurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple)
                ) {
                    Text("View Wallet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = onRedeem,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RewardsPurple)
                ) {
                    Text("Redeem", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RewardFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RewardsPurple.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RewardTransactionRow(
    transaction: RewardTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (transaction.status) {
        RewardTransactionStatus.CONFIRMED, RewardTransactionStatus.COMPLETED -> StatusGreen
        RewardTransactionStatus.PENDING -> StatusOrange
        RewardTransactionStatus.REVERSED, RewardTransactionStatus.REJECTED -> StatusRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (transaction.amount >= 0) Icons.Default.AddCircleOutline else Icons.Default.RemoveCircleOutline
                Icon(imageVector = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = transaction.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.timestamp,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            CoinAmount(
                amount = transaction.amount,
                fontSize = 15,
                showSign = true,
                textColor = if (transaction.amount < 0) StatusRed else statusColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            RewardStatusChip(
                statusText = transaction.status.name,
                statusColor = statusColor
            )
        }
    }
}
