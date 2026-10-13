package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.RewardTransaction
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@Composable
fun WalletScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateRedeem: () -> Unit,
    onNavigateCoinHistory: () -> Unit,
    onSelectTransaction: (RewardTransaction) -> Unit
) {
    val walletSummary by viewModel.walletSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "My Wallet",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsPurpleContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(RewardsPurpleContainer, Color.White)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.wallet_worker),
                                contentDescription = "Wallet Worker",
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RewardCoinBalance(
                                amount = walletSummary.totalBalance,
                                fontSize = 34,
                                textColor = Color(0xFF1F1047),
                                coinSize = 36.dp
                            )
                            Text(
                                text = "WorkDaily Coins",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RewardsPurple
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = RewardsPurple.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Available", fontSize = 12.sp, color = NeutralSlate)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RewardCoinBalance(amount = walletSummary.availableCoins, fontSize = 15, textColor = StatusGreen, coinSize = 16.dp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pending", fontSize = 12.sp, color = NeutralSlate)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RewardCoinBalance(amount = walletSummary.pendingCoins, fontSize = 15, textColor = StatusOrange, coinSize = 16.dp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Lifetime", fontSize = 12.sp, color = NeutralSlate)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RewardCoinBalance(amount = walletSummary.lifetimeEarned, fontSize = 15, textColor = RewardsPurple, coinSize = 16.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            RewardPrimaryButton(
                                text = "Redeem Rewards",
                                onClick = onNavigateRedeem,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            )
                        }
                    }
                }
            }

            item {
                RewardsSectionHeader(
                    title = "Recent Transactions",
                    actionText = "View All",
                    onActionClick = onNavigateCoinHistory
                )
            }

            items(transactions) { tx ->
                RewardTransactionRow(
                    transaction = tx,
                    onClick = {
                        viewModel.selectTransaction(tx)
                        onSelectTransaction(tx)
                    }
                )
            }
        }
    }
}
