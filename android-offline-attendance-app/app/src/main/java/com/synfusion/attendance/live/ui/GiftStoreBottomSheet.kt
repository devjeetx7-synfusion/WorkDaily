package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveGift
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftStoreBottomSheet(
    roomId: String,
    viewModel: LiveViewModel,
    onDismiss: () -> Unit,
    onNavigateWallet: () -> Unit
) {
    val gifts by viewModel.gifts.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val context = LocalContext.current

    var selectedGift by remember { mutableStateOf<LiveGift?>(gifts.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Site Tools", "Machinery", "Heavy Equipment", "Trophies")

    val filteredGifts = remember(gifts, selectedCategory) {
        if (selectedCategory == "All") gifts
        else gifts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LiveDarkBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Title & Diamond Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Construction Gifts",
                        tint = LiveGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Construction Gifts (20+)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                DiamondBalanceChip(
                    balance = wallet.diamondBalance,
                    onClick = {
                        onDismiss()
                        onNavigateWallet()
                    }
                )
            }

            // Category Filter Scrollable Chips
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = LivePrimaryMint,
                edgePadding = 0.dp,
                divider = {}
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = {
                            Text(
                                text = cat,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == cat) LivePrimaryMint else Color.Gray
                            )
                        }
                    )
                }
            }

            // Gifts Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                items(filteredGifts) { gift ->
                    val isSelected = selectedGift?.id == gift.id
                    val canAfford = wallet.diamondBalance >= gift.diamondCost

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .clickable { selectedGift = gift },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) LivePrimaryEmerald.copy(alpha = 0.25f) else LiveSurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) LivePrimaryMint else if (!canAfford) Color.Gray.copy(alpha = 0.3f) else LivePrimaryEmerald.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LiveGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getGiftIcon(gift.name),
                                    contentDescription = gift.name,
                                    tint = if (canAfford) LiveGold else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = gift.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) Color.White else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = null,
                                    tint = LiveDiamondColor,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "${gift.diamondCost}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LiveDiamondColor
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Row: Selected Gift Details & Send CTA Button
            val activeGift = selectedGift
            if (activeGift != null) {
                val canAfford = wallet.diamondBalance >= activeGift.diamondCost

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activeGift.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cost: ${activeGift.diamondCost}", fontSize = 12.sp, color = LiveDiamondColor, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(12.dp))
                        }
                    }

                    if (canAfford) {
                        Button(
                            onClick = {
                                val success = viewModel.sendGift(roomId, activeGift)
                                if (success) {
                                    Toast.makeText(context, "Sent ${activeGift.name}! 🎉", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Insufficient diamonds balance!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Gift", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onNavigateWallet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Diamonds", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
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
