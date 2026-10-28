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
import com.synfusion.attendance.live.model.LiveRoomSessionState
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
    val availableRooms = remember(liveRooms, initialRoomId) {
        val active = liveRooms.filter { it.isLive }.ifEmpty { liveRooms }
        if (active.any { it.id == initialRoomId }) {
            active
        } else {
            // The requested room (e.g. opened from a share link) is not in the discovery cache yet.
            // Render it as its own page so LiveRoomScreen can resolve it from the backend session.
            listOf(
                LiveRoom(
                    id = initialRoomId,
                    title = "WorkDaily Live",
                    creatorId = "",
                    creatorName = "WorkDaily Host",
                    isLive = true
                )
            ) + active
        }
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

    val liveIdentity by viewModel.liveIdentity.collectAsState()
    val sessionState by viewModel.roomSessionState.collectAsState()
    val currentRoom = availableRooms.getOrNull(pagerState.currentPage)
    val isHost = currentRoom?.let { room ->
        val activeRoom = (sessionState as? LiveRoomSessionState.Active)?.room
        when {
            activeRoom != null && activeRoom.id == room.id ->
                (sessionState as LiveRoomSessionState.Active).isHost
            liveIdentity?.uid != null -> room.creatorId == liveIdentity?.uid
            room.isDemoRoom -> room.isHost
            else -> false
        }
    } ?: false

    // Disable vertical paging if the current stream is hosted by user or locked by child controls
    val isPagingEnabled = !isHost && !isChildPagingLocked

    LaunchedEffect(pagerState.currentPage, availableRooms) {
        val room = availableRooms.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        viewModel.setActiveRoom(room.id)
        // Join membership + start presence/chat/playback listeners for the visible room only.
        // Bundled sample rooms have no backend document, so they keep their local-only behavior.
        if (room.isDemoRoom) viewModel.closeLiveRoom() else viewModel.openLiveRoom(room.id)
    }

    DisposableEffect(Unit) {
        onDispose {
            // Leaves the presence document so the viewer count updates immediately.
            viewModel.closeLiveRoom()
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
