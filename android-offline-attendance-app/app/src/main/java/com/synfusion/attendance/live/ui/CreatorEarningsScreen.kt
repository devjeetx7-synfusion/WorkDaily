package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorEarningsScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit
) {
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val context = LocalContext.current

    var showCashoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Stream Earnings", fontWeight = FontWeight.Bold) },
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
            // Earnings Summary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryMint.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ESTIMATED EARNINGS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = LivePrimaryMint)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${String.format(Locale.getDefault(), "%.2f", creatorEarning.totalRupeesEarned)}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(LivePrimaryEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = LivePrimaryMint, modifier = Modifier.size(24.dp))
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(16.dp))
                                Text("${creatorEarning.totalDiamondsReceived} Received Diamonds", fontSize = 13.sp, color = Color.LightGray)
                            }

                            Button(
                                onClick = { showCashoutDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                                enabled = creatorEarning.totalDiamondsReceived > 0,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cashout Demo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Cashout History Section
            item {
                Text("Cashout History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (creatorEarning.cashoutHistory.isEmpty()) {
                item {
                    Text("No cashout records yet. Host live streams and receive gifts to earn!", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                items(creatorEarning.cashoutHistory) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LivePrimaryMint)
                                Column {
                                    Text("Cashout ${record.diamondsConverted} 💎", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
                                    Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Text("₹${String.format(Locale.getDefault(), "%.2f", record.amountRupees)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LivePrimaryMint)
                        }
                    }
                }
            }
        }

        if (showCashoutDialog) {
            AlertDialog(
                onDismissRequest = { showCashoutDialog = false },
                title = { Text("Simulate Creator Cashout", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Convert ${creatorEarning.totalDiamondsReceived} 💎 to ₹${String.format(Locale.getDefault(), "%.2f", creatorEarning.totalRupeesEarned)} in simulated bank account?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = viewModel.cashoutEarnings(creatorEarning.totalDiamondsReceived)
                            if (success) {
                                Toast.makeText(context, "Cashout successful! 💵", Toast.LENGTH_SHORT).show()
                            }
                            showCashoutDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                    ) {
                        Text("Confirm Cashout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCashoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
