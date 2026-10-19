package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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

enum class GiftPanelState {
    COMPACT,
    EXPANDED,
    FULL
}

/**
 * Modern Frosted-Glass Expandable Gift Panel supporting 3 interactive states:
 * State 1 (COMPACT): Minimal horizontal carousel over video.
 * State 2 (EXPANDED): Medium sheet with category tabs, 2/3 column gift grid, and Send CTA.
 * State 3 (FULL): Full vertical browsing interface respecting status and navigation bar insets.
 */
@Composable
fun ExpandableGiftPanel(
    roomId: String,
    viewModel: LiveViewModel,
    onClose: () -> Unit,
    onNavigateWallet: () -> Unit,
    onGiftSent: (LiveGift, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gifts by viewModel.gifts.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val context = LocalContext.current

    var panelState by remember { mutableStateOf(GiftPanelState.COMPACT) }
    var selectedGift by remember { mutableStateOf<LiveGift?>(gifts.firstOrNull()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedQuantity by remember { mutableStateOf(1) }

    val categories = listOf("All", "Popular", "Construction", "Premium")
    val quantityMultipliers = listOf(1, 5, 10, 99)

    val filteredGifts = remember(gifts, selectedCategory) {
        when (selectedCategory) {
            "Popular" -> gifts.filter { it.diamondCost in 10..200 }
            "Construction" -> gifts.filter { it.category == "Site Tools" || it.category == "Materials" || it.category == "Masonry" }
            "Premium" -> gifts.filter { it.diamondCost >= 200 }
            else -> gifts
        }
    }

    // Back press handler collapses state or closes panel
    BackHandler {
        when (panelState) {
            GiftPanelState.FULL -> panelState = GiftPanelState.EXPANDED
            GiftPanelState.EXPANDED -> panelState = GiftPanelState.COMPACT
            GiftPanelState.COMPACT -> onClose()
        }
    }

    // Draggable gesture state for vertical drag region
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        dragAccumulator += delta
        if (dragAccumulator < -40f) { // Swipe UP
            if (panelState == GiftPanelState.COMPACT) {
                panelState = GiftPanelState.EXPANDED
                dragAccumulator = 0f
            } else if (panelState == GiftPanelState.EXPANDED) {
                panelState = GiftPanelState.FULL
                dragAccumulator = 0f
            }
        } else if (dragAccumulator > 40f) { // Swipe DOWN
            if (panelState == GiftPanelState.FULL) {
                panelState = GiftPanelState.EXPANDED
                dragAccumulator = 0f
            } else if (panelState == GiftPanelState.EXPANDED) {
                panelState = GiftPanelState.COMPACT
                dragAccumulator = 0f
            }
        }
    }

    val panelHeightModifier = when (panelState) {
        GiftPanelState.COMPACT -> Modifier.height(130.dp)
        GiftPanelState.EXPANDED -> Modifier.height(380.dp)
        GiftPanelState.FULL -> Modifier.fillMaxHeight(0.75f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(panelHeightModifier)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        color = Color(0xDC0B101A), // Translucent Frosted Glass surface
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Drag Region Header (Drag Handle + Title + Diamond Balance + Expand/Close Buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { dragAccumulator = 0f }
                    )
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(LiveGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = LiveGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = if (panelState == GiftPanelState.COMPACT) "Gift Store" else "Gift Catalog",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Drag Indicator Handle
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DiamondBalanceChip(
                        balance = wallet.diamondBalance,
                        onClick = onNavigateWallet,
                        fontSize = 10,
                        iconSize = 12.dp
                    )

                    // State Expand Toggle Button
                    IconButton(
                        onClick = {
                            panelState = when (panelState) {
                                GiftPanelState.COMPACT -> GiftPanelState.EXPANDED
                                GiftPanelState.EXPANDED -> GiftPanelState.FULL
                                GiftPanelState.FULL -> GiftPanelState.COMPACT
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = when (panelState) {
                                GiftPanelState.COMPACT -> Icons.Default.KeyboardArrowUp
                                GiftPanelState.EXPANDED -> Icons.Default.UnfoldMore
                                GiftPanelState.FULL -> Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = "Toggle Expansion",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Close Button
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Gift Store",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // STATE 1: COMPACT MODE (Floating horizontal carousel)
            if (panelState == GiftPanelState.COMPACT) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    items(gifts, key = { it.id }) { gift ->
                        val canAfford = wallet.diamondBalance >= gift.diamondCost

                        Surface(
                            modifier = Modifier
                                .width(88.dp)
                                .height(76.dp)
                                .clickable {
                                    if (canAfford) {
                                        val success = viewModel.sendGift(roomId, gift, 1)
                                        if (success) {
                                            onGiftSent(gift, 1)
                                        } else {
                                            Toast.makeText(context, "Insufficient diamonds", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Tap diamond balance to top up", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (canAfford) LiveGold.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Icon(
                                    imageVector = getGiftIcon(gift.name),
                                    contentDescription = gift.name,
                                    tint = if (canAfford) LiveGold else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = gift.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(10.dp))
                                    Text("${gift.diamondCost}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LiveDiamondColor)
                                }
                            }
                        }
                    }
                }
            } else {
                // STATE 2 & 3: EXPANDED / FULL MODES (Tabs + Grid + Quantity Multiplier + Send Action)
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
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Adaptive Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 85.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredGifts, key = { it.id }) { gift ->
                        val isSelected = selectedGift?.id == gift.id
                        val totalCost = gift.diamondCost * selectedQuantity
                        val canAfford = wallet.diamondBalance >= totalCost

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clickable { selectedGift = gift },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) LivePrimaryEmerald.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) LivePrimaryMint else Color.White.copy(alpha = 0.15f)
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(10.dp))
                                    Text("${gift.diamondCost}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LiveDiamondColor)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quantity Multiplier Bar & Send CTA
                val activeGift = selectedGift
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity Multiplier Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quantityMultipliers.forEach { qty ->
                            val isSelected = selectedQuantity == qty
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) LiveGold else Color.White.copy(alpha = 0.12f))
                                    .clickable { selectedQuantity = qty }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "x$qty",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    // Send Gift Button
                    if (activeGift != null) {
                        val totalCost = activeGift.diamondCost * selectedQuantity
                        val canAfford = wallet.diamondBalance >= totalCost

                        Button(
                            onClick = {
                                if (canAfford) {
                                    val success = viewModel.sendGift(roomId, activeGift, selectedQuantity)
                                    if (success) {
                                        onGiftSent(activeGift, selectedQuantity)
                                    } else {
                                        Toast.makeText(context, "Insufficient diamonds balance!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    onNavigateWallet()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) LivePrimaryEmerald else LiveGold,
                                contentColor = if (canAfford) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (canAfford) Icons.AutoMirrored.Filled.Send else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (canAfford) "Send ($totalCost 💎)" else "Get Diamonds",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
