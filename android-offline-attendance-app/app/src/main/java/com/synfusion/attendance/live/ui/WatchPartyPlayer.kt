package com.synfusion.attendance.live.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.LivePrimaryMint
import com.synfusion.attendance.live.ui.components.LiveSurfaceDark
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.live.watchparty.WatchPartySyncPolicy
import com.synfusion.attendance.live.watchparty.YouTubeLinkParser
import com.synfusion.attendance.live.watchparty.YouTubeParseResult
import com.synfusion.attendance.live.watchparty.YouTubeSelection
import com.synfusion.attendance.live.watchparty.YouTubeSource
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/** Player events reported by the official YouTube IFrame bridge. */
private sealed class WatchPartyPlayerEvent {
    object Ready : WatchPartyPlayerEvent()
    data class StateChanged(val status: WatchPartyPlaybackStatus, val positionSeconds: Float) : WatchPartyPlayerEvent()
    data class Position(val positionSeconds: Float, val playing: Boolean) : WatchPartyPlayerEvent()
    data class Failed(val code: Int) : WatchPartyPlayerEvent()
}

/**
 * Watch Party stage: the **official YouTube embedded player** (IFrame Player API inside a WebView)
 * hosted in a rounded, draggable container.
 *
 * Design notes / policy compliance:
 * - No custom video player, no stream extraction, no ad bypassing. YouTube's own controls,
 *   branding and ads are untouched (`controls=1`, `modestbranding=0`).
 * - `playsinline=1` keeps playback inside the floating container; the app never requests fullscreen.
 * - The WebView only navigates to YouTube/Google hosts; external links are ignored.
 * - A missing/invalid video never falls back to a sample video; the user is asked to pick one.
 */
@Composable
fun WatchPartyStage(
    roomId: String,
    watchPartyState: WatchPartyState?,
    playlist: List<WatchPartyPlaylistItem>,
    roomYoutubeVideoId: String?,
    roomYoutubeVideoUrl: String?,
    roomYoutubePlaylistId: String?,
    isHost: Boolean,
    isSessionBacked: Boolean,
    isLive: Boolean,
    playlistLoading: Boolean,
    locallyMuted: Boolean,
    onToggleLocalMute: () -> Unit,
    viewModel: LiveViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val selection: YouTubeSelection? = remember(watchPartyState?.videoId, watchPartyState?.playlistId, roomYoutubeVideoId, roomYoutubePlaylistId, roomYoutubeVideoUrl) {
        resolveSelection(
            stateVideoId = watchPartyState?.videoId,
            statePlaylistId = watchPartyState?.playlistId,
            roomVideoId = roomYoutubeVideoId,
            roomPlaylistId = roomYoutubePlaylistId,
            roomVideoUrl = roomYoutubeVideoUrl
        )
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var playerReady by remember { mutableStateOf(false) }
    var playerStatus by remember { mutableStateOf(WatchPartyPlaybackStatus.UNSTARTED) }
    var lastKnownPosition by remember { mutableFloatStateOf(0f) }
    var appliedRevision by remember(roomId) { mutableLongStateOf(-1L) }
    var loadedVideoId by remember(roomId) { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isHidden by rememberSaveable(roomId) { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var lastPublishedPositionAt by remember { mutableLongStateOf(0L) }

    var offsetX by remember(roomId) { mutableFloatStateOf(0f) }
    var offsetY by remember(roomId) { mutableFloatStateOf(0f) }

    val isTurnedOff = watchPartyState?.isTurnedOff == true
    val sharedMuted = watchPartyState?.isMuted == true

    // The WebView JS bridge is created once, so every value it reads must come from an updated
    // state holder instead of the (possibly stale) first composition.
    val currentIsHost = rememberUpdatedState(isHost)
    val currentIsSessionBacked = rememberUpdatedState(isSessionBacked)
    val currentIsLive = rememberUpdatedState(isLive)
    val currentTurnedOff = rememberUpdatedState(isTurnedOff)
    val currentPlayerStatus = rememberUpdatedState(playerStatus)
    val latestState = rememberUpdatedState(watchPartyState)
    // Set while a remote revision is being pushed into the WebView: the resulting player callbacks
    // are consequences of the applied state, not a new host intent, so they must not be published
    // back (that would create the revision feedback loop).
    var remoteApplyUntilMillis by remember { mutableLongStateOf(0L) }
    val currentEventSink = rememberUpdatedState<(WatchPartyPlayerEvent) -> Unit> { event ->
        when (event) {
            is WatchPartyPlayerEvent.Ready -> {
                playerReady = true
                errorMessage = null
                loadedVideoId = selection?.videoId ?: loadedVideoId
            }

            is WatchPartyPlayerEvent.StateChanged -> {
                val previousStatus = playerStatus
                playerStatus = event.status
                lastKnownPosition = event.positionSeconds
                val causedByRemoteState = System.currentTimeMillis() < remoteApplyUntilMillis
                // Only publish real transitions: YouTube re-emits PLAYING after buffering/seeks and
                // every write is a revision bump, so duplicates must not reach Firestore.
                val isRealTransition = previousStatus != event.status
                if (!causedByRemoteState && isRealTransition && currentIsHost.value &&
                    currentIsSessionBacked.value && currentIsLive.value && !currentTurnedOff.value
                ) {
                    when (event.status) {
                        WatchPartyPlaybackStatus.PLAYING -> {
                            lastPublishedPositionAt = System.currentTimeMillis()
                            viewModel.hostPlay(event.positionSeconds)
                        }

                        WatchPartyPlaybackStatus.PAUSED -> viewModel.hostPause(event.positionSeconds)
                        WatchPartyPlaybackStatus.ENDED -> viewModel.hostNotifyPlayerCompleted()
                        else -> Unit
                    }
                }
            }

            is WatchPartyPlayerEvent.Position -> {
                lastKnownPosition = event.positionSeconds
                if (event.playing) playerStatus = WatchPartyPlaybackStatus.PLAYING
            }

            is WatchPartyPlayerEvent.Failed -> {
                errorMessage = playerErrorMessage(event.code)
                playerReady = false
            }
        }
    }

    fun evaluate(script: String) {
        runCatching { webViewRef?.evaluateJavascript(script, null) }
    }

    // Load/seek/play based on a (remote or host) state revision.
    fun applyStateToPlayer(state: WatchPartyState, forceVideoLoad: Boolean) {
        remoteApplyUntilMillis = System.currentTimeMillis() + 1_500L
        val targetVideoId = state.videoId
        if (targetVideoId.isBlank()) return
        val position = WatchPartySyncPolicy.projectedPositionSeconds(state, System.currentTimeMillis())
        val videoChanged = forceVideoLoad || targetVideoId != loadedVideoId
        if (videoChanged) {
            loadedVideoId = targetVideoId
            val autoPlay = state.status == WatchPartyPlaybackStatus.PLAYING
            evaluate("wpLoad('${targetVideoId}', ${position}, $autoPlay);")
            playerStatus = WatchPartyPlaybackStatus.BUFFERING
            return
        }
        when (state.status) {
            WatchPartyPlaybackStatus.PLAYING -> {
                if (playerStatus != WatchPartyPlaybackStatus.PLAYING ||
                    WatchPartySyncPolicy.shouldCorrectPosition(lastKnownPosition, position)
                ) {
                    evaluate("wpSeek(${position}); wpPlay();")
                }
            }

            WatchPartyPlaybackStatus.PAUSED, WatchPartyPlaybackStatus.UNSTARTED -> {
                if (playerStatus == WatchPartyPlaybackStatus.PLAYING) evaluate("wpPause();")
                if (WatchPartySyncPolicy.shouldCorrectPosition(lastKnownPosition, position)) {
                    evaluate("wpSeek(${position});")
                }
            }

            WatchPartyPlaybackStatus.ENDED -> {
                if (playerStatus == WatchPartyPlaybackStatus.PLAYING) evaluate("wpPause();")
            }

            WatchPartyPlaybackStatus.BUFFERING -> Unit
        }
    }

    // Shared mute is authoritative when the host toggles it; participants can mute locally without
    // publishing anything (their local flag is combined here).
    LaunchedEffect(sharedMuted, locallyMuted, playerReady) {
        if (!playerReady) return@LaunchedEffect
        evaluate(if (sharedMuted || locallyMuted) "wpMute();" else "wpUnmute();")
    }

    // Apply remote/host revisions exactly once per revision, in order.
    LaunchedEffect(watchPartyState, playerReady, isTurnedOff, selection) {
        val state = watchPartyState ?: return@LaunchedEffect
        if (!playerReady) return@LaunchedEffect
        if (isTurnedOff) {
            evaluate("wpPause();")
            return@LaunchedEffect
        }
        if (state.revision <= appliedRevision) return@LaunchedEffect
        appliedRevision = state.revision
        remoteApplyUntilMillis = System.currentTimeMillis() + 1_500L
        applyStateToPlayer(state, forceVideoLoad = selection?.videoId != null && loadedVideoId != selection.videoId)
    }

    // Hiding the video tears the WebView down; reset the player bookkeeping so reopening reloads
    // the video at the shared position instead of assuming the old WebView is still alive.
    LaunchedEffect(isHidden) {
        if (isHidden) {
            playerReady = false
            loadedVideoId = null
            appliedRevision = -1L
        }
    }

    // Periodic drift correction / host position publication (bounded frequency).
    LaunchedEffect(playerReady, isHost, isSessionBacked, isTurnedOff) {
        if (!playerReady) return@LaunchedEffect
        while (true) {
            delay(5_000)
            val now = System.currentTimeMillis()
            val state = watchPartyState ?: continue
            if (isTurnedOff) continue
            val expected = WatchPartySyncPolicy.projectedPositionSeconds(state, now)
            if (isHost && isSessionBacked) {
                val drifted = abs(lastKnownPosition - expected) > 3f
                val due = WatchPartySyncPolicy.isCorrectionDue(lastPublishedPositionAt, now)
                if (playerStatus == WatchPartyPlaybackStatus.PLAYING && (drifted || due)) {
                    lastPublishedPositionAt = now
                    if (drifted) {
                        viewModel.hostSeek(lastKnownPosition, resumePlaying = true)
                    } else {
                        viewModel.hostPublishPositionCorrection(lastKnownPosition)
                    }
                }
            } else if (playerStatus == WatchPartyPlaybackStatus.PLAYING &&
                WatchPartySyncPolicy.shouldCorrectPosition(lastKnownPosition, expected)
            ) {
                evaluate("wpSeek(${expected});")
            }
        }
    }

    // Pause local playback when the app goes to the background. The host publishes the pause so
    // every participant stays aligned instead of drifting through a suspended WebView.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    evaluate("wpPause();")
                    runCatching { webViewRef?.onPause() }
                    if (currentIsHost.value && currentIsSessionBacked.value &&
                        currentPlayerStatus.value == WatchPartyPlaybackStatus.PLAYING
                    ) {
                        viewModel.hostPause(lastKnownPosition)
                    }
                }

                Lifecycle.Event.ON_START -> {
                    runCatching { webViewRef?.onResume() }
                    val state = latestState.value
                    if (state != null && state.status == WatchPartyPlaybackStatus.PLAYING &&
                        !currentTurnedOff.value
                    ) {
                        evaluate(
                            "wpSeek(${WatchPartySyncPolicy.projectedPositionSeconds(state, System.currentTimeMillis())}); wpPlay();"
                        )
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Release the WebView and its JS bridge on dispose (room exit, video change, process death).
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                runCatching {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.removeJavascriptInterface("AndroidBridge")
                    webView.destroy()
                }
            }
            webViewRef = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            isTurnedOff -> TurnedOffStage(
                isHost = isHost,
                onOpenPlaylist = { showPlaylistSheet = true },
                onTurnOn = { viewModel.hostTurnWatchPartyOn() }
            )

            selection == null -> NoVideoSelectedStage(
                isHost = isHost,
                onOpenPlaylist = { showPlaylistSheet = true }
            )

            isHidden -> HiddenPlayerPill(
                onReopen = { isHidden = false },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
            )

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 56.dp, start = 8.dp, end = 8.dp)
                ) {
                    val maxWidthPx = with(density) { maxWidth.toPx() }
                    val maxHeightPx = with(density) { maxHeight.toPx() }
                    val playerWidthDp = (maxWidth * 0.92f).coerceAtLeast(220.dp)
                    val playerHeightDp = (playerWidthDp * 9f / 16f).coerceAtLeast(124.dp)
                    val playerWidthPx = with(density) { playerWidthDp.toPx() }
                    val playerHeightPx = with(density) { playerHeightDp.toPx() }
                    val headerHeightPx = with(density) { 34.dp.toPx() }
                    val totalHeightPx = playerHeightPx + headerHeightPx
                    // Keep the floating player inside the usable area and above the chat composer.
                    val chatReservePx = with(density) { 190.dp.toPx() }
                    val maxOffsetX = (maxWidthPx - playerWidthPx).coerceAtLeast(0f)
                    val maxOffsetY = (maxHeightPx - totalHeightPx - chatReservePx).coerceAtLeast(0f)

                    LaunchedEffect(maxOffsetX, maxOffsetY) {
                        offsetX = offsetX.coerceIn(0f, maxOffsetX)
                        offsetY = offsetY.coerceIn(0f, maxOffsetY)
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = LiveSurfaceDark,
                        border = BorderStroke(1.dp, LivePrimaryMint.copy(alpha = 0.35f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .width(playerWidthDp)
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    ) {
                        Column {
                            // Drag handle header. Dragging is limited to this bar so YouTube's own
                            // controls keep receiving taps and slider gestures inside the player.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .background(Color(0xFF131A24))
                                    .pointerInput(maxOffsetX, maxOffsetY) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxOffsetX)
                                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxOffsetY)
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.DragIndicator,
                                        contentDescription = "Drag video",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isHost) "Watch Party · Host" else "Watch Party",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            // The host publishes the shared mute state; participants
                                            // only change their own audio preference.
                                            if (isHost && isSessionBacked && isLive) {
                                                viewModel.hostSetMuted(!sharedMuted)
                                            } else {
                                                onToggleLocalMute()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (sharedMuted || locallyMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = if (sharedMuted || locallyMuted) "Unmute" else "Mute",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showPlaylistSheet = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.QueueMusic,
                                            contentDescription = "Playlist",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { isHidden = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = "Hide video",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(playerHeightDp)
                                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                                    .background(Color.Black)
                            ) {
                                if (selection != null) {
                                    YouTubeIframePlayer(
                                        selection = selection,
                                        onCreated = { webView -> webViewRef = webView },
                                        onEvent = { event -> currentEventSink.value(event) }
                                    )
                                }

                                errorMessage?.let { message ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.9f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Text(
                                                "Video unavailable",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                message,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { showPlaylistSheet = true }) {
                                                    Text("Playlist", color = LivePrimaryMint, fontSize = 12.sp)
                                                }
                                                TextButton(onClick = {
                                                    errorMessage = null
                                                    playerReady = false
                                                    webViewRef?.reload()
                                                }) {
                                                    Text("Retry", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Room-level lifecycle hint: participants cannot control playback.
        if (!isHost && isSessionBacked && !isTurnedOff && selection != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp)
            ) {
                Text(
                    "Host controls playback",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        if (showPlaylistSheet) {
            WatchPartyPlaylistSheet(
                watchPartyState = watchPartyState,
                playlist = playlist,
                isHost = isHost,
                isSessionBacked = isSessionBacked,
                isLoading = playlistLoading,
                locallyMuted = locallyMuted,
                onToggleLocalMute = onToggleLocalMute,
                onAddVideo = { url -> viewModel.addPlaylistVideo(url) },
                onSelectItem = { item -> viewModel.selectPlaylistVideo(item.id) },
                onRemoveItem = { item -> viewModel.removePlaylistVideo(item.id) },
                onTurnOff = { viewModel.hostTurnWatchPartyOff() },
                onTurnOn = { viewModel.hostTurnWatchPartyOn() },
                onDismiss = { showPlaylistSheet = false }
            )
        }
    }
}

private fun resolveSelection(
    stateVideoId: String?,
    statePlaylistId: String?,
    roomVideoId: String?,
    roomPlaylistId: String?,
    roomVideoUrl: String?
): YouTubeSelection? {
    val videoId = stateVideoId?.takeIf { it.isNotBlank() }
        ?: roomVideoId?.takeIf { it.isNotBlank() }
        ?: roomVideoUrl?.let { raw -> (YouTubeLinkParser.parse(raw) as? YouTubeParseResult.Success)?.selection?.videoId }
    val playlistId = statePlaylistId?.takeIf { it.isNotBlank() }
        ?: roomPlaylistId?.takeIf { it.isNotBlank() }
        ?: roomVideoUrl?.let { raw -> (YouTubeLinkParser.parse(raw) as? YouTubeParseResult.Success)?.selection?.playlistId }

    if (videoId == null && playlistId == null) return null
    if (videoId != null && !YouTubeLinkParser.isValidVideoId(videoId)) return null
    val canonical = if (videoId != null) {
        YouTubeLinkParser.watchUrl(videoId, playlistId)
    } else {
        YouTubeLinkParser.playlistUrl(playlistId!!)
    }
    return YouTubeSelection(
        videoId = videoId,
        playlistId = playlistId,
        source = if (videoId == null) YouTubeSource.PLAYLIST else YouTubeSource.VIDEO,
        canonicalUrl = canonical
    )
}

private fun playerErrorMessage(code: Int): String = when (code) {
    2 -> "The video ID is invalid."
    5 -> "The YouTube player could not play this video in a WebView."
    100 -> "This video was removed or is private."
    101, 150 -> "The video owner does not allow embedded playback."
    else -> "YouTube reported a playback error (code $code)."
}

/**
 * Origin used both as the WebView base URL and as the `origin` player variable. They must match,
 * otherwise the IFrame API rejects `postMessage` control of the player.
 */
private const val PLAYER_ORIGIN = "https://workdaily.app"

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeIframePlayer(
    selection: YouTubeSelection,
    onCreated: (WebView) -> Unit,
    onEvent: (WatchPartyPlayerEvent) -> Unit
) {
    val html = remember(selection.videoId, selection.playlistId) {
        buildYouTubePlayerHtml(selection, origin = PLAYER_ORIGIN)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            WebView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                // Clip the WebView surface to the rounded container.
                background = GradientDrawable().apply {
                    cornerRadius = 18f * viewContext.resources.displayMetrics.density
                    setColor(android.graphics.Color.BLACK)
                }
                clipToOutline = true
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // Remote state must be able to start playback without a per-viewer tap.
                    mediaPlaybackRequiresUserGesture = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    allowFileAccess = false
                    allowContentAccess = false
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url ?: return true
                        if (request.isForMainFrame.not()) return false
                        val host = url.host?.lowercase().orEmpty()
                        val path = url.path.orEmpty()
                        val isPlayerSurface = host.endsWith("youtube.com") &&
                            (path.startsWith("/embed/") || path.startsWith("/iframe_api"))
                        if (isPlayerSurface) return false
                        val isYouTubeDestination = host.endsWith("youtube.com") ||
                            host.endsWith("youtu.be") ||
                            host.endsWith("youtube-nocookie.com")
                        if (isYouTubeDestination) {
                            // "Watch on YouTube" / channel taps leave the app instead of replacing the
                            // embedded player, so playback and branding rules are respected.
                            runCatching {
                                viewContext.startActivity(
                                    Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        return true
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            post { onEvent(WatchPartyPlayerEvent.Failed(-1)) }
                        }
                    }
                }
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPlayerReady() {
                            post { onEvent(WatchPartyPlayerEvent.Ready) }
                        }

                        @JavascriptInterface
                        fun onPlayerStateChange(stateCode: Int, positionSeconds: Float) {
                            val status = when (stateCode) {
                                0 -> WatchPartyPlaybackStatus.ENDED
                                1 -> WatchPartyPlaybackStatus.PLAYING
                                2 -> WatchPartyPlaybackStatus.PAUSED
                                3 -> WatchPartyPlaybackStatus.BUFFERING
                                -1 -> WatchPartyPlaybackStatus.UNSTARTED
                                else -> WatchPartyPlaybackStatus.UNSTARTED
                            }
                            post { onEvent(WatchPartyPlayerEvent.StateChanged(status, positionSeconds)) }
                        }

                        @JavascriptInterface
                        fun onPlayerPosition(positionSeconds: Float, playing: Boolean) {
                            post { onEvent(WatchPartyPlayerEvent.Position(positionSeconds, playing)) }
                        }

                        @JavascriptInterface
                        fun onPlayerError(code: Int) {
                            post { onEvent(WatchPartyPlayerEvent.Failed(code)) }
                        }
                    },
                    "AndroidBridge"
                )
                loadDataWithBaseURL(
                    PLAYER_ORIGIN,
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
                onCreated(this)
            }
        },
        update = { webView ->
            // Language/config changes only; video changes are applied through the JS bridge.
            if (webView.url == null) {
                runCatching { webView.loadDataWithBaseURL(PLAYER_ORIGIN, html, "text/html", "UTF-8", null) }
            }
        }
    )
}

/**
 * The IFrame Player API page. Only official YouTube API endpoints are used; the embed keeps its
 * native controls, branding and advertising behavior intact.
 */
private fun buildYouTubePlayerHtml(selection: YouTubeSelection, origin: String): String {
    val variables = YouTubeLinkParser.playerVariables(selection, origin)
    val playerVars = variables.entries.joinToString(",\n") { (key, value) ->
        "                    '$key': '${value.replace("'", "")}'"
    }
    val videoLine = selection.videoId?.let { "                        videoId: '$it',\n" }.orEmpty()
    return """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
  <style>
    html, body { margin: 0; padding: 0; height: 100%; background: #000; overflow: hidden; }
    #player { width: 100%; height: 100%; }
  </style>
</head>
<body>
  <div id="player"></div>
  <script>
    var tag = document.createElement('script');
    tag.src = "https://www.youtube.com/iframe_api";
    var firstScriptTag = document.getElementsByTagName('script')[0];
    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

    var player;
    var positionTimer;

    function bridgeReady() {
      return window.AndroidBridge && window.AndroidBridge.onPlayerReady;
    }

    function onYouTubeIframeAPIReady() {
      player = new YT.Player('player', {
        width: '100%',
        height: '100%',
$videoLine        playerVars: {
$playerVars
        },
        events: {
          'onReady': function (event) {
            if (bridgeReady()) { window.AndroidBridge.onPlayerReady(); }
            startPositionTicker();
          },
          'onStateChange': function (event) {
            var position = 0;
            try { position = player.getCurrentTime() || 0; } catch (e) { position = 0; }
            if (window.AndroidBridge) { window.AndroidBridge.onPlayerStateChange(event.data, position); }
          },
          'onError': function (event) {
            if (window.AndroidBridge) { window.AndroidBridge.onPlayerError(event.data); }
          }
        }
      });
    }

    function startPositionTicker() {
      if (positionTimer) { clearInterval(positionTimer); }
      positionTimer = setInterval(function () {
        try {
          var playing = player && player.getPlayerState && player.getPlayerState() === 1;
          var position = player && player.getCurrentTime ? (player.getCurrentTime() || 0) : 0;
          if (window.AndroidBridge && window.AndroidBridge.onPlayerPosition) {
            window.AndroidBridge.onPlayerPosition(position, playing);
          }
        } catch (e) { }
      }, 1000);
    }

    function wpPlay() { try { if (player && player.playVideo) player.playVideo(); } catch (e) { } }
    function wpPause() { try { if (player && player.pauseVideo) player.pauseVideo(); } catch (e) { } }
    function wpSeek(seconds) { try { if (player && player.seekTo) player.seekTo(seconds, true); } catch (e) { } }
    function wpMute() { try { if (player && player.mute) player.mute(); } catch (e) { } }
    function wpUnmute() { try { if (player && player.unMute) player.unMute(); } catch (e) { } }
    function wpLoad(videoId, startSeconds, autoPlay) {
      try {
        if (!player) { return; }
        var args = { videoId: videoId, startSeconds: startSeconds || 0 };
        if (autoPlay === false && player.cueVideoById) {
          player.cueVideoById(args);
        } else if (player.loadVideoById) {
          player.loadVideoById(args);
        }
      } catch (e) { }
    }
  </script>
</body>
</html>
    """.trimIndent()
}

@Composable
private fun TurnedOffStage(
    isHost: Boolean,
    onOpenPlaylist: () -> Unit,
    onTurnOn: () -> Unit
) {
    StageMessage(
        icon = Icons.Default.TvOff,
        title = "Watch Party turned off",
        message = "The host stopped synchronized video playback. The Live room is still running.",
        actions = {
            TextButton(onClick = onOpenPlaylist) {
                Text("Playlist", color = LivePrimaryMint, fontSize = 13.sp)
            }
            if (isHost) {
                Button(
                    onClick = onTurnOn,
                    colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                ) {
                    Text("Turn On", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun NoVideoSelectedStage(
    isHost: Boolean,
    onOpenPlaylist: () -> Unit
) {
    StageMessage(
        icon = Icons.Default.OndemandVideo,
        title = "No video selected",
        message = if (isHost) {
            "Add a YouTube video to start the Watch Party. Nothing is auto-played."
        } else {
            "Waiting for the host to add a video to the Watch Party playlist."
        },
        actions = {
            if (isHost) {
                Button(
                    onClick = onOpenPlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Playlist", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun StageMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 96.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(46.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            message,
            color = Color.LightGray,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
private fun HiddenPlayerPill(onReopen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onReopen,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDC2626),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            Text("Reopen video", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
