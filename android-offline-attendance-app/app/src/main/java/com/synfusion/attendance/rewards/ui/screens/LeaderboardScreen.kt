package com.synfusion.attendance.rewards.ui.screens

import kotlinx.coroutines.launch
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val leaderboardUsers by viewModel.getLeaderboard().collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Leaderboard",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = RewardsTheme.screenBackground
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
                        coroutineScope.launch {
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
                                    .padding(vertical = 22.dp, horizontal = 12.dp)
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

                                    // 3D Podium Container (#2 Left, #1 Center, #3 Right)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Rank #2 (Silver)
                                        LeaderboardPodiumSlot3D(
                                            user = top2,
                                            drawableRes = R.drawable.leaderboard_second,
                                            rankNum = 2,
                                            pillarHeight = 110.dp,
                                            pillarBrush = RewardsSilverGradient,
                                            artworkSize = 90.dp
                                        )

                                        // Rank #1 (Gold)
                                        LeaderboardPodiumSlot3D(
                                            user = top1,
                                            drawableRes = R.drawable.leaderboard_first,
                                            rankNum = 1,
                                            pillarHeight = 150.dp,
                                            pillarBrush = RewardsGoldGradient,
                                            artworkSize = 110.dp
                                        )

                                        // Rank #3 (Bronze)
                                        LeaderboardPodiumSlot3D(
                                            user = top3,
                                            drawableRes = R.drawable.leaderboard_third,
                                            rankNum = 3,
                                            pillarHeight = 90.dp,
                                            pillarBrush = RewardsBronzeGradient,
                                            artworkSize = 80.dp
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
private fun LeaderboardPodiumSlot3D(
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
        modifier = Modifier.width(100.dp)
    ) {
        // Medal / Avatar Artwork resting directly on top of the podium rim
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.offset(y = 12.dp)
        ) {
            if (rankNum == 1) {
                Box(
                    modifier = Modifier
                        .size(artworkSize + 12.dp)
                        .clip(CircleShape)
                        .background(GoldBright.copy(alpha = 0.3f))
                )
            }
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "Rank #$rankNum",
                modifier = Modifier.size(artworkSize),
                contentScale = ContentScale.Fit
            )
        }

        // Layered 3D Metallic Podium Cylinder with Name & XP embedded on front face
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth()
        ) {
            LeaderboardCanvasPodium(
                rank = rankNum,
                colorGradient = pillarBrush,
                height = pillarHeight
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 18.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = user.name,
                    fontSize = if (rankNum == 1) 14.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (rankNum == 1) Color(0xFF4A2800) else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${user.xp} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentUserRankCard(user: LeaderboardUser) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsTheme.elevatedSurface),
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
                            color = RewardsTheme.textPrimary
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
                        color = RewardsTheme.textSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RewardsTheme.cardBackground)
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
        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
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
                        .background(RewardsTheme.elevatedSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${user.rank}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsTheme.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RoyalPurple.copy(alpha = 0.12f)),
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
                    color = RewardsTheme.textPrimary,
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

@Composable
private fun LeaderboardCanvasPodium(
    rank: Int,
    colorGradient: Brush,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.BottomCenter
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val ellipseHeight = 22.dp.toPx()

            // Shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.22f),
                topLeft = androidx.compose.ui.geometry.Offset(0f, canvasHeight - ellipseHeight),
                size = androidx.compose.ui.geometry.Size(canvasWidth, ellipseHeight)
            )

            // Main Cylinder Body
            drawRect(
                brush = colorGradient,
                topLeft = androidx.compose.ui.geometry.Offset(0f, ellipseHeight / 2),
                size = androidx.compose.ui.geometry.Size(canvasWidth, canvasHeight - ellipseHeight)
            )

            // Front face highlights
            drawRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.08f, ellipseHeight / 2),
                size = androidx.compose.ui.geometry.Size(canvasWidth * 0.22f, canvasHeight - ellipseHeight)
            )

            drawRect(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.72f, ellipseHeight / 2),
                size = androidx.compose.ui.geometry.Size(canvasWidth * 0.28f, canvasHeight - ellipseHeight)
            )

            // Bottom Curve
            drawArc(
                brush = colorGradient,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(0f, canvasHeight - ellipseHeight),
                size = androidx.compose.ui.geometry.Size(canvasWidth, ellipseHeight)
            )

            // Top Metallic Rim Surface
            drawOval(
                color = when(rank) {
                    1 -> Color(0xFFFDE68A)
                    2 -> Color(0xFFE2E8F0)
                    3 -> Color(0xFFFED7AA)
                    else -> Color.White
                },
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(canvasWidth, ellipseHeight)
            )

            // Top inner highlight
            drawOval(
                color = Color.White.copy(alpha = 0.65f),
                topLeft = androidx.compose.ui.geometry.Offset(canvasWidth * 0.1f, ellipseHeight * 0.1f),
                size = androidx.compose.ui.geometry.Size(canvasWidth * 0.8f, ellipseHeight * 0.8f)
            )
        }
    }
}
