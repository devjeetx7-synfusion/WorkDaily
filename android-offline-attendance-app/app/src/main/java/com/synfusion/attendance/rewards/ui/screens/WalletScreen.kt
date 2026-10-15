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
        containerColor = RewardsTheme.screenBackground
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
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewardsPrimaryGradient)
                            .padding(22.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.wallet_worker),
                                contentDescription = "Wallet Worker",
                                modifier = Modifier.size(115.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RewardCoinBalance(
                                amount = walletSummary.totalBalance,
                                fontSize = 34,
                                textColor = Color.White,
                                coinSize = 38.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total WorkDaily Coin Balance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighlightLavender
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Available, Pending, Lifetime Metrics Inset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Available", fontSize = 11.sp, color = RewardsTextSecondary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RewardCoinBalance(amount = walletSummary.availableCoins, fontSize = 14, textColor = RewardsSuccessGreen, coinSize = 15.dp)
                                }
                                Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color(0xFFCBD5E1)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pending", fontSize = 11.sp, color = RewardsTextSecondary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RewardCoinBalance(amount = walletSummary.pendingCoins, fontSize = 14, textColor = StatusOrange, coinSize = 15.dp)
                                }
                                Box(modifier = Modifier.height(20.dp).width(1.dp).background(Color(0xFFCBD5E1)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Lifetime", fontSize = 11.sp, color = RewardsTextSecondary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    RewardCoinBalance(amount = walletSummary.lifetimeEarned, fontSize = 14, textColor = RoyalPurple, coinSize = 15.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            RewardPrimaryButton(
                                text = "Redeem Rewards",
                                onClick = onNavigateRedeem,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                containerColor = RoyalPurple,
                                contentColor = Color.White
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
