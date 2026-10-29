package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSetupScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit
) {
    val context = LocalContext.current

    var selectedType by remember { mutableStateOf(LiveRoomType.VIDEO) }
    var micEnabled by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var youtubeUrl by remember { mutableStateOf("") }
    var showYouTubeBrowser by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val liveIdentity by viewModel.liveIdentity.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAdvancedSettings by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }
    var isChatEnabled by remember { mutableStateOf(true) }
    var allowGuestRequests by remember { mutableStateOf(true) }
    var showGifts by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Immersive Full-Screen Stage Canvas Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1F2937),
                            Color(0xFF111827),
                            Color(0xFF000000)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            when (selectedType) {
                                LiveRoomType.VIDEO -> LivePrimaryEmerald.copy(alpha = 0.2f)
                                LiveRoomType.AUDIO -> LivePurple.copy(alpha = 0.2f)
                                LiveRoomType.TEXT -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedType) {
                            LiveRoomType.VIDEO -> Icons.Default.Videocam
                            LiveRoomType.AUDIO -> Icons.Default.Mic
                            LiveRoomType.TEXT -> Icons.Default.ChatBubble
                            LiveRoomType.YOUTUBE_WATCH_PARTY -> Icons.Default.OndemandVideo
                        },
                        contentDescription = null,
                        tint = when (selectedType) {
                            LiveRoomType.VIDEO -> LivePrimaryMint
                            LiveRoomType.AUDIO -> LivePurple
                            LiveRoomType.TEXT -> Color(0xFF60A5FA)
                            LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFF87171)
                        },
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = when (selectedType) {
                        LiveRoomType.VIDEO -> "Camera Preview Stage"
                        LiveRoomType.AUDIO -> "Voice Room Stage"
                        LiveRoomType.TEXT -> "Group Text Live Chat Stage"
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> "YouTube Watch Party Stage"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedType == LiveRoomType.YOUTUBE_WATCH_PARTY) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = youtubeUrl,
                            onValueChange = { youtubeUrl = it },
                            placeholder = { Text("Paste YouTube Video / Playlist URL or ID", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFEF4444),
                                unfocusedBorderColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                if (youtubeUrl.isNotBlank()) {
                                    IconButton(onClick = { youtubeUrl = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                                    }
                                }
                            }
                        )

                        // Browse YouTube Web Button
                        OutlinedButton(
                            onClick = { showYouTubeBrowser = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse YouTube Website", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        val videoId = extractYouTubeVideoId(youtubeUrl)
                        val playlistId = extractYouTubePlaylistId(youtubeUrl)

                        if (youtubeUrl.isNotBlank()) {
                            val isValid = videoId != null || playlistId != null
                            val statusText = when {
                                videoId != null -> "✓ Valid Video ID: $videoId"
                                playlistId != null -> "✓ Valid Playlist ID: $playlistId"
                                else -> "⚠️ Invalid YouTube URL, Video ID, or Playlist ID"
                            }
                            Text(
                                text = statusText,
                                color = if (isValid) LivePrimaryMint else Color(0xFFF87171),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                LiveBadge(text = "SETUP", bgColor = LivePrimaryEmerald)
            }
        }

        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Text(
                text = "Go Live",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { showAdvancedSettings = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Switcher Pill (Video, Audio, Text, YouTube)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                LiveRoomType.values().forEach { mode ->
                    val isSelected = selectedType == mode
                    val label = when (mode) {
                        LiveRoomType.VIDEO -> "Video"
                        LiveRoomType.AUDIO -> "Audio"
                        LiveRoomType.TEXT -> "Text"
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> "YouTube"
                    }
                    val bg = if (isSelected) {
                        when (mode) {
                            LiveRoomType.VIDEO -> LivePrimaryEmerald
                            LiveRoomType.AUDIO -> LivePurple
                            LiveRoomType.TEXT -> Color(0xFF2563EB)
                            LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFDC2626)
                        }
                    } else Color.Transparent

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable { selectedType = mode }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Actions based on Mode
            if (selectedType == LiveRoomType.VIDEO || selectedType == LiveRoomType.AUDIO) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { micEnabled = !micEnabled },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (micEnabled) Color.White.copy(alpha = 0.25f) else LiveRed)
                    ) {
                        Icon(
                            imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Microphone Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (selectedType == LiveRoomType.VIDEO) {
                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Flip Camera",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            submitError?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFF87171),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Start Live CTA Button
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    submitError = null

                    val selection = if (selectedType == LiveRoomType.YOUTUBE_WATCH_PARTY) {
                        when (val parsed = com.synfusion.attendance.live.watchparty.YouTubeLinkParser.parse(youtubeUrl)) {
                            is com.synfusion.attendance.live.watchparty.YouTubeParseResult.Success -> parsed.selection
                            is com.synfusion.attendance.live.watchparty.YouTubeParseResult.Failure -> {
                                submitError = parsed.message
                                return@Button
                            }
                        }
                    } else {
                        null
                    }

                    val autoTitle = when (selectedType) {
                        LiveRoomType.VIDEO -> "Live Site Workday Broadcast"
                        LiveRoomType.AUDIO -> "Live Site Voice Discussion"
                        LiveRoomType.TEXT -> "Live Site Group Text Chat"
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> "Site Safety YouTube Watch Party"
                    }

                    isSubmitting = true
                    coroutineScope.launch {
                        try {
                            if (viewModel.liveIdentity.value == null) {
                                viewModel.refreshLiveIdentity()
                                delay(250)
                            }
                            val identity = viewModel.liveIdentity.value
                            if (identity == null) {
                                submitError = "Sign in to WorkDaily to start a live room. Your account is the host."
                                return@launch
                            }
                            val result = viewModel.createLiveRoom(
                                com.synfusion.attendance.live.data.LiveRoomCreateRequest(
                                    title = autoTitle,
                                    category = "Site Safety",
                                    type = selectedType,
                                    tags = listOf("LiveWork", "SiteSafety"),
                                    description = "",
                                    isPublic = isPublic,
                                    allowGuestRequests = allowGuestRequests,
                                    showGifts = showGifts,
                                    isChatEnabled = isChatEnabled,
                                    selection = selection
                                )
                            )
                            when (result) {
                                is com.synfusion.attendance.live.model.LiveOperationResult.Success -> {
                                    Toast.makeText(context, "You are LIVE! 🔴", Toast.LENGTH_SHORT).show()
                                    onNavigateLiveRoom(result.data.id)
                                }

                                is com.synfusion.attendance.live.model.LiveOperationResult.Error -> {
                                    submitError = result.message
                                }
                            }
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedType) {
                        LiveRoomType.VIDEO -> LivePrimaryEmerald
                        LiveRoomType.AUDIO -> LivePurple
                        LiveRoomType.TEXT -> Color(0xFF2563EB)
                        LiveRoomType.YOUTUBE_WATCH_PARTY -> Color(0xFFDC2626)
                    },
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Live", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // YouTube Website Browser Bottom Sheet Dialog
        if (showYouTubeBrowser) {
            YouTubeBrowserBottomSheet(
                onDismiss = { showYouTubeBrowser = false },
                onSelectContent = { url, videoId, playlistId, title, thumbnailUrl ->
                    youtubeUrl = url
                    showYouTubeBrowser = false
                    Toast.makeText(context, "Selected: ${title ?: videoId ?: "YouTube video"} ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Room Settings Modal Bottom Sheet
        if (showAdvancedSettings) {
            ModalBottomSheet(
                onDismissRequest = { showAdvancedSettings = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Room Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Public Stream", fontSize = 14.sp)
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Live Chat", fontSize = 14.sp)
                        Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Guest Requests", fontSize = 14.sp)
                        Switch(checked = allowGuestRequests, onCheckedChange = { allowGuestRequests = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Gifts", fontSize = 14.sp)
                        Switch(checked = showGifts, onCheckedChange = { showGifts = it })
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeBrowserBottomSheet(
    onDismiss: () -> Unit,
    onSelectContent: (url: String, videoId: String?, playlistId: String?, title: String?, thumbnailUrl: String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("https://m.youtube.com") }
    var detectedVideoId by remember { mutableStateOf<String?>(null) }
    var detectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var videoMetadata by remember { mutableStateOf<com.synfusion.attendance.live.data.VideoMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }

    LaunchedEffect(detectedVideoId) {
        val vId = detectedVideoId
        if (vId != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val url = com.synfusion.attendance.live.watchparty.YouTubeOEmbed.endpointUrl(vId)
                val body = runCatching { java.net.URL(url).readText() }.getOrNull()
                videoMetadata = com.synfusion.attendance.live.watchparty.YouTubeOEmbed.parse(body)
            }
        } else {
            videoMetadata = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        windowInsets = WindowInsets(0.dp),
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .imePadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Browse & Search YouTube",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Interactive Search & Navigation Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search YouTube or paste link/ID...", fontSize = 12.sp, color = Color.Gray) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFEF4444)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            val parsedVideoId = extractYouTubeVideoId(searchQuery)
                            val parsedPlaylistId = extractYouTubePlaylistId(searchQuery)
                            if (parsedVideoId != null || parsedPlaylistId != null) {
                                val targetUrl = if (parsedVideoId != null) "https://m.youtube.com/watch?v=$parsedVideoId"
                                                else "https://m.youtube.com/playlist?list=$parsedPlaylistId"
                                webViewRef?.loadUrl(targetUrl)
                            } else {
                                val encoded = java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8")
                                webViewRef?.loadUrl("https://m.youtube.com/results?search_query=$encoded")
                            }
                        }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = LivePrimaryMint)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFEF4444),
                    unfocusedBorderColor = Color.Gray
                )
            )

            // WebView Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            webViewRef = this
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = false
                                displayZoomControls = false
                            }
                            setOnTouchListener { v, event ->
                                when (event.action) {
                                    android.view.MotionEvent.ACTION_DOWN,
                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    android.view.MotionEvent.ACTION_UP,
                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: android.webkit.WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    val host = uri.host?.lowercase() ?: ""
                                    val isAllowedHost = host.endsWith("youtube.com") || host.endsWith("youtu.be")
                                    return if (isAllowedHost) {
                                        false
                                    } else {
                                        Toast.makeText(context, "External link restricted", Toast.LENGTH_SHORT).show()
                                        true
                                    }
                                }

                                override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let {
                                        currentUrl = it
                                        detectedVideoId = extractYouTubeVideoId(it)
                                        detectedPlaylistId = extractYouTubePlaylistId(it)
                                    }
                                }

                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let {
                                        currentUrl = it
                                        detectedVideoId = extractYouTubeVideoId(it)
                                        detectedPlaylistId = extractYouTubePlaylistId(it)
                                    }
                                }

                                override fun doUpdateVisitedHistory(view: android.webkit.WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    url?.let {
                                        currentUrl = it
                                        detectedVideoId = extractYouTubeVideoId(it)
                                        detectedPlaylistId = extractYouTubePlaylistId(it)
                                    }
                                }
                            }
                            loadUrl("https://m.youtube.com")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color(0xFFEF4444)
                    )
                }
            }

            // Bottom Sticky Selector Bar
            Surface(
                color = Color(0xFF1F2937),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val vId = detectedVideoId
                    val pId = detectedPlaylistId
                    val meta = videoMetadata

                    if (vId != null || pId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (meta?.thumbnailUrl != null) {
                                coil.compose.AsyncImage(
                                    model = meta.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta?.title ?: if (vId != null) "✓ Video ID: $vId" else "✓ Playlist ID: $pId",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                if (meta?.authorName != null) {
                                    Text(
                                        text = meta.authorName,
                                        color = LivePrimaryMint,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onSelectContent(currentUrl, vId, pId, meta?.title, meta?.thumbnailUrl)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (vId != null) "Use Selected Video" else "Use Selected Playlist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Text(
                            text = "Navigate to any YouTube video or search above to select it.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
