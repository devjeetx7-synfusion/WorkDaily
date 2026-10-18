package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorEarningsScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit
) {
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo Creator Earnings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Earnings Hero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveGold.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("TOTAL CREATOR DIAMONDS RECEIVED", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = LiveGold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveGold, modifier = Modifier.size(32.dp))
                            Text("${creatorEarning.totalDiamondsReceived}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }

                        Text("Total Rupee Value: ≈ ₹${creatorEarning.totalRupeesEarned}", fontSize = 13.sp, color = LivePrimaryMint, fontWeight = FontWeight.Bold)

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Available: ${creatorEarning.availableDiamonds} 💎", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Pending: ${creatorEarning.pendingDiamonds} 💎", fontSize = 11.sp, color = Color.Gray)
                            }

                            Button(
                                onClick = {
                                    val success = viewModel.cashoutEarnings(200)
                                    if (success) {
                                        Toast.makeText(context, "Converted 200 Diamonds into ₹100 Demo Cashout!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Insufficient available diamonds!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LiveGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cashout 200 💎", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Cashout History Section
            item {
                Text("Demo Cashout History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            if (creatorEarning.cashoutHistory.isEmpty()) {
                item {
                    Text("No cashouts requested yet.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                items(creatorEarning.cashoutHistory) { record ->
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
                            Column {
                                Text("Converted ${record.diamondsConverted} 💎", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Status: ${record.status}", fontSize = 11.sp, color = LivePrimaryMint)
                            }

                            Text("₹${record.amountRupees}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LiveGold)
                        }
                    }
                }
            }
        }
    }
}
