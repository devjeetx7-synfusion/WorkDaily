package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.LeaderboardTimeframe
import com.synfusion.attendance.rewards.data.LeaderboardUser
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeaderboardScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val tabs = remember { listOf("Weekly", "Monthly") }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val leaderboardUsers by viewModel.getLeaderboard().collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Leaderboard",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = RewardsScreenBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Switcher Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                RewardSegmentedControl(
                    options = tabs,
                    selectedIndex = pagerState.currentPage,
                    onOptionSelected = { index ->
                        kotlinx.coroutines.runBlocking {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                LaunchedEffect(pageIndex) {
                    viewModel.setLeaderboardTimeframe(
                        if (pageIndex == 0) LeaderboardTimeframe.WEEKLY else LeaderboardTimeframe.MONTHLY
                    )
                }

                val top1 = leaderboardUsers.find { it.rank == 1 }
                val top2 = leaderboardUsers.find { it.rank == 2 }
                val top3 = leaderboardUsers.find { it.rank == 3 }
                val restUsers = leaderboardUsers.filter { it.rank > 3 }
                val currentUser = leaderboardUsers.find { it.isCurrentUser }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // XP Rankings Hero & 3D Podium Card
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
                                    .background(RewardsSecondaryGradient)
                                    .padding(vertical = 22.dp, horizontal = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = GoldBright,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "XP RANKINGS",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            letterSpacing = 1.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // 3D Podium Container
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Rank #2 (Silver)
                                        PodiumSlot3D(
                                            user = top2,
                                            drawableRes = R.drawable.leaderboard_second,
                                            rankNum = 2,
                                            pillarHeight = 90.dp,
                                            pillarBrush = RewardsSilverGradient,
                                            artworkSize = 72.dp
                                        )

                                        // Rank #1 (Gold)
                                        PodiumSlot3D(
                                            user = top1,
                                            drawableRes = R.drawable.leaderboard_first,
                                            rankNum = 1,
                                            pillarHeight = 125.dp,
                                            pillarBrush = RewardsGoldGradient,
                                            artworkSize = 92.dp
                                        )

                                        // Rank #3 (Bronze)
                                        PodiumSlot3D(
                                            user = top3,
                                            drawableRes = R.drawable.leaderboard_third,
                                            rankNum = 3,
                                            pillarHeight = 75.dp,
                                            pillarBrush = RewardsBronzeGradient,
                                            artworkSize = 68.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Highlighted Current User Rank Card
                    if (currentUser != null) {
                        item {
                            CurrentUserRankCard(user = currentUser)
                        }
                    }

                    // Remaining Ranking Rows
                    items(restUsers, key = { it.id }) { user ->
                        if (!user.isCurrentUser) {
                            LeaderboardRowItem(user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSlot3D(
    user: LeaderboardUser?,
    drawableRes: Int,
    rankNum: Int,
    pillarHeight: androidx.compose.ui.unit.Dp,
    pillarBrush: Brush,
    artworkSize: androidx.compose.ui.unit.Dp
) {
    if (user == null) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        // Medal / Avatar Artwork with optional Gold Halo for #1
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            if (rankNum == 1) {
                Box(
                    modifier = Modifier
                        .size(artworkSize + 16.dp)
                        .clip(CircleShape)
                        .background(GoldBright.copy(alpha = 0.25f))
                )
            }
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "Rank #$rankNum",
                modifier = Modifier.size(artworkSize),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = user.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${user.xp} XP",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3D Styled Pillar Container with Beveled Top Edge
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(pillarHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(pillarBrush)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.4f),
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            // Beveled Top Surface Highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color.White.copy(alpha = 0.35f))
            )

            // Rank Number
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rankNum",
                    fontSize = if (rankNum == 1) 28.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CurrentUserRankCard(user: LeaderboardUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsLavenderSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, RoyalPurple)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(RoyalPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${user.rank}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RewardsTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RoyalPurple)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Current Leaderboard Rank",
                        fontSize = 12.sp,
                        color = RewardsTextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, HighlightLavender, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${user.xp} XP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalPurple
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRowItem(user: LeaderboardUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${user.rank}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsTextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RewardsLavenderSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = RoyalPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = user.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${user.xp} XP",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = RoyalPurple
            )
        }
    }
}
