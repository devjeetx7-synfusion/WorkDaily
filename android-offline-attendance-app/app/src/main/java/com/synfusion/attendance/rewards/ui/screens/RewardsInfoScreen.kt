package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.ui.components.RewardsTheme
import com.synfusion.attendance.rewards.ui.components.RewardsTopAppBar
import com.synfusion.attendance.rewards.ui.components.RoyalPurple

data class FAQItem(val question: String, val answer: String)

@Composable
fun RewardsInfoScreen(
    onNavigateBack: () -> Unit
) {
    val faqList = remember {
        listOf(
            FAQItem(
                "What are WorkDaily Coins?",
                "WorkDaily Coins are reward points you earn by using the WorkDaily application, logging attendance consistently, completing tasks, and inviting peers."
            ),
            FAQItem(
                "How to earn WorkDaily Coins?",
                "You can earn coins by completing daily check-ins, maintaining attendance streaks, taking part in weekly challenges, unlocking achievement milestones, and referring friends using your unique referral code."
            ),
            FAQItem(
                "What is the difference between Available vs Pending coins?",
                "Available coins are confirmed rewards ready to be redeemed. Pending coins are currently undergoing verification (e.g., waiting for referred users to qualify) and will move to Available once confirmed."
            ),
            FAQItem(
                "How do Referral Rewards work?",
                "When you share your unique referral code and a friend signs up and logs initial attendance entries, both you and your friend earn bonus coins."
            ),
            FAQItem(
                "How do Tasks & Challenges work?",
                "Tasks are categorized into Daily, Weekly, and Special milestones. Completing the requirements unlocks the 'Claim' button to collect coin rewards."
            ),
            FAQItem(
                "How do Achievements & Badges work?",
                "Badges track long-term milestones like 7-day streaks, 30-day attendance mastery, and report exports. Unlocking a badge rewards bonus XP and coins."
            ),
            FAQItem(
                "What is the Leaderboard?",
                "The Leaderboard ranks WorkDaily users based on earned XP points over Weekly and Monthly periods. Top rankers earn prestigious profile badges."
            ),
            FAQItem(
                "What are Reversed Rewards?",
                "If a referral fails qualification criteria or a redemption is canceled, the associated coins are marked as 'Reversed' or returned to your balance."
            ),
            FAQItem(
                "How do I redeem my Rewards?",
                "Visit the 'Redeem' section in your Wallet to exchange available coins for digital gift vouchers and shopping coupons. (Note: Currently running in Demo Mode)."
            )
        )
    }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Rewards Program Info",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(faqList) { faq ->
                FAQCard(faq = faq)
            }
        }
    }
}

@Composable
private fun FAQCard(faq: FAQItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTheme.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = RoyalPurple
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = faq.answer,
                        fontSize = 13.sp,
                        color = RewardsTheme.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
