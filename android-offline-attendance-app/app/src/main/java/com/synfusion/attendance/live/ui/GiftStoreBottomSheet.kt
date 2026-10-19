package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
    onNavigateWallet: () -> Unit,
    onGiftSent: (LiveGift, Int) -> Unit = { _, _ -> }
) {
    val gifts by viewModel.gifts.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val liveRooms by viewModel.liveRooms.collectAsState()

    val currentRoom = liveRooms.find { it.id == roomId }
    val recipientName = currentRoom?.creatorName ?: "Host"

    val context = LocalContext.current

    var selectedGift by remember { mutableStateOf<LiveGift?>(gifts.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf("Construction") }
    var selectedQuantity by remember { mutableStateOf(1) }
    var showCongratulationsDialog by remember { mutableStateOf<Pair<LiveGift, Int>?>(null) }

    val categories = listOf("Construction", "Site Tools", "Love", "Celebration", "Premium")
    val quantityMultipliers = listOf(1, 5, 10, 99)

    val filteredGifts = remember(gifts, selectedCategory) {
        when (selectedCategory) {
            "Construction" -> gifts.filter { it.category == "Site Tools" || it.category == "Materials" || it.category == "Masonry" }
            "Site Tools" -> gifts.filter { it.category == "Site Tools" || it.category == "Tools" || it.category == "Power Tools" }
            "Love" -> gifts.filter { it.category == "Green Energy" || it.category == "Design" || it.diamondCost <= 50 }
            "Celebration" -> gifts.filter { it.category == "VIP Gifts" || it.category == "Trophies" || it.diamondCost in 50..1000 }
            "Premium" -> gifts.filter { it.diamondCost >= 500 }
            else -> gifts
        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        contentDescription = "Gifts Catalog",
                        tint = LiveGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Text("Construction Gifts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                DiamondBalanceChip(
                    balance = wallet.diamondBalance,
                    onClick = {
                        onDismiss()
                        onNavigateWallet()
                    }
                )
            }

            // Category Filter Scrollable Tabs
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
                                color = if (selectedCategory == cat) LivePrimaryMint else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Gifts Grid (Adaptive multi-column, unclipped up-to-2-line gift names)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 82.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                items(filteredGifts, key = { it.id }) { gift ->
                    val isSelected = selectedGift?.id == gift.id
                    val totalCost = gift.diamondCost * selectedQuantity
                    val canAfford = wallet.diamondBalance >= totalCost

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clickable { selectedGift = gift },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) LivePrimaryEmerald.copy(alpha = 0.25f) else LiveSurfaceDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) LivePrimaryMint else if (!canAfford) Color.Gray.copy(alpha = 0.2f) else LivePrimaryEmerald.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LiveGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getGiftIcon(gift.name),
                                    contentDescription = gift.name,
                                    tint = if (canAfford) LiveGold else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = gift.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canAfford) Color.White else Color.Gray,
                                maxLines = 2,
                                lineHeight = 12.sp,
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

            // Quantity Multiplier Pills Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quantity:", fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quantityMultipliers.forEach { qty ->
                        val isSelected = selectedQuantity == qty
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) LiveGold else LiveSurfaceDark)
                                .clickable { selectedQuantity = qty }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "x$qty",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar: Gift Total & Send CTA
            val activeGift = selectedGift
            if (activeGift != null) {
                val totalCost = activeGift.diamondCost * selectedQuantity
                val canAfford = wallet.diamondBalance >= totalCost

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${activeGift.name} (x$selectedQuantity)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Total: $totalCost", fontSize = 12.sp, color = LiveDiamondColor, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(12.dp))
                        }
                    }

                    if (canAfford) {
                        Button(
                            onClick = {
                                val success = viewModel.sendGift(roomId, activeGift, selectedQuantity)
                                if (success) {
                                    onGiftSent(activeGift, selectedQuantity)
                                    showCongratulationsDialog = Pair(activeGift, selectedQuantity)
                                } else {
                                    Toast.makeText(context, "Insufficient diamonds balance!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
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
                            shape = RoundedCornerShape(14.dp)
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

        showCongratulationsDialog?.let { (gift, qty) ->
            GiftCongratulationsDialog(
                gift = gift,
                quantity = qty,
                recipientName = recipientName,
                totalDiamondsSpent = gift.diamondCost * qty,
                onDismiss = {
                    showCongratulationsDialog = null
                    onDismiss()
                }
            )
        }
    }
}
