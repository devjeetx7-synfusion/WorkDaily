package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

data class TopUpPackage(
    val diamonds: Int,
    val priceRupees: Int,
    val isPopular: Boolean = false,
    val bonusDiamonds: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiamondWalletScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit
) {
    val wallet by viewModel.wallet.collectAsState()
    val giftTransactions by viewModel.giftTransactions.collectAsState()
    val context = LocalContext.current

    val packages = listOf(
        TopUpPackage(100, 10),
        TopUpPackage(500, 45, isPopular = true, bonusDiamonds = 50),
        TopUpPackage(1000, 80, bonusDiamonds = 150),
        TopUpPackage(2500, 180, bonusDiamonds = 400),
        TopUpPackage(5000, 350, bonusDiamonds = 1000)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo Diamond Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiveDarkBg, titleContentColor = Color.White)
            )
        },
        containerColor = LiveDarkBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Balance Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveDiamondColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("DIAMOND BALANCE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = LiveDiamondColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(32.dp))
                                    Text("${wallet.diamondBalance}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.addFreeDiamonds(1000)
                                    Toast.makeText(context, "+1000 Demo Diamonds Added! 💎", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Free +1000", fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Purchased: ${wallet.totalPurchased} 💎", fontSize = 12.sp, color = Color.LightGray)
                            Text("Total Spent: ${wallet.totalSpent} 💎", fontSize = 12.sp, color = Color.LightGray)
                        }
                    }
                }
            }

            // Top-Up Packages Section
            item {
                Text("Simulated Diamond Packages", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    packages.forEach { pkg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val totalToAdd = pkg.diamonds + pkg.bonusDiamonds
                                    viewModel.topUpDiamonds(totalToAdd)
                                    Toast.makeText(context, "Added $totalToAdd Diamonds (₹${pkg.priceRupees} Simulated)!", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (pkg.isPopular) LiveGold else LivePrimaryEmerald.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(LiveDiamondColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(24.dp))
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${pkg.diamonds} Diamonds", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            if (pkg.bonusDiamonds > 0) {
                                                LiveBadge(text = "+${pkg.bonusDiamonds} BONUS", bgColor = LiveGold)
                                            }
                                        }
                                        Text("Instant demo credit", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val totalToAdd = pkg.diamonds + pkg.bonusDiamonds
                                        viewModel.topUpDiamonds(totalToAdd)
                                        Toast.makeText(context, "Added $totalToAdd Diamonds!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pkg.isPopular) LiveGold else LivePrimaryEmerald,
                                        contentColor = if (pkg.isPopular) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("₹${pkg.priceRupees}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions History
            item {
                Text("Recent Diamond Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (giftTransactions.isEmpty()) {
                item {
                    Text("No gift transactions yet. Send gifts in live streams!", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                items(giftTransactions.take(5)) { tx ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = LiveGold)
                                Column {
                                    Text("Sent ${tx.giftName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Room: ${tx.roomId}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Text("-${tx.diamondCost} 💎", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LiveRed)
                        }
                    }
                }
            }
        }
    }
}
