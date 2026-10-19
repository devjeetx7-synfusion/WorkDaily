package com.synfusion.attendance.live.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.ui.components.LiveDarkBg
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveFeedScreen(
    initialRoomId: String,
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateSummary: (String) -> Unit = {},
    onNavigatePrivateChat: (String) -> Unit = {},
    onNavigateFullProfile: (String) -> Unit = {}
) {
    val liveRooms by viewModel.liveRooms.collectAsState()
    val availableRooms = remember(liveRooms) {
        val active = liveRooms.filter { it.isLive }
        if (active.isNotEmpty()) active else liveRooms
    }

    if (availableRooms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LiveDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No Live Streams Available", color = Color.White, fontSize = 16.sp)
        }
        return
    }

    val initialIndex = remember(initialRoomId, availableRooms) {
        val index = availableRooms.indexOfFirst { it.id == initialRoomId }
        if (index >= 0) index else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { availableRooms.size }
    )

    var isChildPagingLocked by remember { mutableStateOf(false) }

    val currentRoom = availableRooms.getOrNull(pagerState.currentPage)
    val isHost = currentRoom?.let { room ->
        room.isHost || room.creatorId == "creator_me" || room.creatorId == "user_me"
    } ?: false

    // Disable vertical paging if the current stream is hosted by user or locked by child controls
    val isPagingEnabled = !isHost && !isChildPagingLocked

    LaunchedEffect(pagerState.currentPage, availableRooms) {
        val room = availableRooms.getOrNull(pagerState.currentPage)
        if (room != null) {
            viewModel.setActiveRoom(room.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveDarkBg)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = isPagingEnabled,
            key = { index -> availableRooms.getOrNull(index)?.id ?: index }
        ) { page ->
            val room = availableRooms.getOrNull(page)
            if (room != null) {
                LiveRoomScreen(
                    roomId = room.id,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onNavigateWallet = onNavigateWallet,
                    onNavigateSummary = onNavigateSummary,
                    onNavigatePrivateChat = onNavigatePrivateChat,
                    onNavigateFullProfile = onNavigateFullProfile,
                    onPagingLockChange = { isLocked ->
                        if (page == pagerState.currentPage) {
                            isChildPagingLocked = isLocked
                        }
                    }
                )
            }
        }
    }
}
