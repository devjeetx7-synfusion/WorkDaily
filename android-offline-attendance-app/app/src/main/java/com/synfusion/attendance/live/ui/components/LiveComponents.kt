package com.synfusion.attendance.live.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// WorkDaily Live Design Tokens
val LivePrimaryEmerald = Color(0xFF0F9F7A)
val LivePrimaryMint = Color(0xFF10B981)
val LiveDarkBg = Color(0xFF0F172A)
val LiveSurfaceDark = Color(0xFF1E293B)
val LiveDiamondColor = Color(0xFF00E5FF)
val LiveGold = Color(0xFFFFBE0B)
val LiveRed = Color(0xFFEF4444)
val LivePurple = Color(0xFF8B5CF6)

val LiveEmeraldGradient = Brush.horizontalGradient(
    colors = listOf(LivePrimaryEmerald, LivePrimaryMint)
)

@Composable
fun PulsingLiveAvatar(
    name: String,
    avatarUrl: String = "",
    size: Dp = 56.dp,
    isLive: Boolean = true,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringColor = if (isLive) LiveRed else LivePrimaryMint

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 12.dp)
            .clickable { onClick() }
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(size + 8.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.3f))
            )
        }

        Box(
            modifier = Modifier
                .size(size + 4.dp)
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(LivePrimaryEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val initials = if (name.isNotBlank()) {
                    name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                } else "WD"

                Text(
                    text = initials,
                    fontSize = (size.value * 0.35f).sp,
                    fontWeight = FontWeight.Bold,
                    color = LivePrimaryMint
                )
            }
        }

        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(LiveRed)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "LIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DiamondBalanceChip(
    balance: Int,
    onClick: () -> Unit = {},
    fontSize: Int = 13,
    iconSize: Dp = 16.dp
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = LiveDiamondColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LiveDiamondColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Diamonds",
                tint = LiveDiamondColor,
                modifier = Modifier.size(iconSize)
            )
            Text(
                text = "$balance",
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = LiveDiamondColor
            )
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Top Up",
                tint = LiveDiamondColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun FollowButton(
    isFollowing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onToggle,
            modifier = modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryMint),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LivePrimaryMint),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Following", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onToggle,
            modifier = modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LivePrimaryEmerald,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiveBadge(
    text: String = "LIVE",
    bgColor: Color = LiveRed
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = text.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ViewerCountChip(
    count: Int,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun RoomCategoryChip(category: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LivePrimaryEmerald.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f))
    ) {
        Text(
            text = category,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = LivePrimaryMint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun getGiftIcon(giftName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        giftName.contains("Helmet", ignoreCase = true) || giftName.contains("Hardhat", ignoreCase = true) -> Icons.Default.Shield
        giftName.contains("Tape", ignoreCase = true) -> Icons.Default.Straighten
        giftName.contains("Brick", ignoreCase = true) -> Icons.Default.HomeWork
        giftName.contains("Trowel", ignoreCase = true) -> Icons.Default.Construction
        giftName.contains("Drill", ignoreCase = true) -> Icons.Default.Build
        giftName.contains("Scaffold", ignoreCase = true) -> Icons.Default.Apartment
        giftName.contains("Truck", ignoreCase = true) || giftName.contains("Hauler", ignoreCase = true) -> Icons.Default.LocalShipping
        giftName.contains("Mixer", ignoreCase = true) -> Icons.Default.Autorenew
        giftName.contains("Excavator", ignoreCase = true) || giftName.contains("Bulldozer", ignoreCase = true) -> Icons.Default.Agriculture
        giftName.contains("Crane", ignoreCase = true) -> Icons.Default.Architecture
        giftName.contains("Saw", ignoreCase = true) -> Icons.Default.ContentCut
        giftName.contains("Solar", ignoreCase = true) -> Icons.Default.WbSunny
        giftName.contains("Beam", ignoreCase = true) -> Icons.Default.HorizontalRule
        giftName.contains("Compass", ignoreCase = true) -> Icons.Default.Explore
        giftName.contains("Trophy", ignoreCase = true) -> Icons.Default.EmojiEvents
        else -> Icons.Default.Construction
    }
}
