package com.synfusion.attendance.live.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.synfusion.attendance.live.model.LiveGift
import com.synfusion.attendance.live.ui.components.*

@Composable
fun GiftCongratulationsDialog(
    gift: LiveGift,
    quantity: Int,
    recipientName: String,
    totalDiamondsSpent: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "giftGlow")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = LiveDarkBg,
            border = androidx.compose.foundation.BorderStroke(2.dp, LiveGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Sparkle Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scalePulse)
                        .clip(CircleShape)
                        .background(LiveGold.copy(alpha = 0.2f))
                        .border(2.dp, LiveGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGiftIcon(gift.name),
                        contentDescription = gift.name,
                        tint = LiveGold,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Gift Sent! 🎉",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "You sent ${if (quantity > 1) "$quantity x " else ""}${gift.name} to $recipientName",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }

                // Summary Chips Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = LiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL DIAMONDS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveDiamondColor)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(16.dp))
                                Text("$totalDiamondsSpent 💎", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("QUANTITY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveGold)
                            Text("x$quantity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LiveGold)
                        }
                    }
                }

                // CTA Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue Watching", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
