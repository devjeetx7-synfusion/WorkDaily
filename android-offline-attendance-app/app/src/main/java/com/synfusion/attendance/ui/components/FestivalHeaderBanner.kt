package com.synfusion.attendance.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.data.FestivalEvent
import java.io.File

/**
 * Premium Festival Hero Environment.
 * Creates an integrated hero header (20–35% upper screen area)
 * with background gradient fade, vector artwork, particles, greeting, user avatar, and date.
 */
@Composable
fun FestivalHeroEnvironment(
    event: FestivalEvent,
    displayName: String,
    formattedDate: String,
    photoUrl: String = "",
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = remember(event.primaryColorHex) { Color(event.primaryColorHex) }
    val secondaryColor = remember(event.secondaryColorHex) { Color(event.secondaryColorHex) }
    val accentColor = remember(event.accentColorHex) { Color(event.accentColorHex) }

    val bgGradientTop = remember(event) {
        event.heroBgGradientTopHex?.let { Color(it) } ?: Color(0xFF0B1026)
    }
    val bgGradientBottom = remember(event) {
        event.heroBgGradientBottomHex?.let { Color(it) } ?: Color(0xFF1E2B58)
    }

    val firstName = remember(displayName) {
        val trimmed = displayName.trim()
        if (trimmed.isNotEmpty()) trimmed.split(" ")[0] else "Rajesh"
    }

    val avatarLetters = remember(displayName) {
        val trimmed = displayName.trim()
        if (trimmed.length >= 2) trimmed.take(2).uppercase()
        else if (trimmed.isNotEmpty()) trimmed.take(1).uppercase()
        else "R"
    }

    val imageFile = remember(photoUrl) {
        if (photoUrl.isNotEmpty()) File(photoUrl) else null
    }
    val showLocalImage = imageFile != null && imageFile.exists()
    val imageRequest = remember(photoUrl, showLocalImage) {
        if (imageFile != null && showLocalImage) {
            coil.request.ImageRequest.Builder(context)
                .data(imageFile)
                .crossfade(true)
                .memoryCacheKey("${photoUrl}_${imageFile.lastModified()}")
                .diskCacheKey("${photoUrl}_${imageFile.lastModified()}")
                .networkCachePolicy(coil.request.CachePolicy.DISABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        } else null
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !event.useLightStatusBarIcons
            }
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(event.id) {
        isVisible = true
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)),
        exit = fadeOut(animationSpec = tween(400))
    ) {
        val heroAssetUrl = remember(event.heroAssetPath) { "file:///android_asset/${event.heroAssetPath}" }
        var isHeroAssetLoaded by remember(event.id) { mutableStateOf(false) }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            bgGradientTop,
                            bgGradientBottom,
                            bgGradientBottom.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            // Layer 1 — Bundled PNG Hero Asset Image (if available) with responsive cover crop
            coil.compose.SubcomposeAsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(heroAssetUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${event.name} Hero Artwork",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { isHeroAssetLoaded = true },
                onError = { isHeroAssetLoaded = false }
            )

            // Layer 2 — Vector Artwork & Animation Canvas Fallback (if hero.png PNG asset is missing)
            if (!isHeroAssetLoaded) {
                FestivalAnimationCanvas(
                    event = event,
                    modifier = Modifier.matchParentSize()
                )
                FestivalArtwork(
                    event = event,
                    modifier = Modifier.matchParentSize()
                )
            }

            // Layer 3 — Smooth Bottom Gradient Transition Fade into WorkDaily background
            val targetBgColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.78f to bgGradientBottom.copy(alpha = 0.65f),
                            1.0f to targetBgColor
                        )
                    )
            )

            // Content Overlay with Top Safe Insets for Transparent Status Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding.calculateTopPadding())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Header Row: User Avatar / Greeting (Left) & Date / Subtitle (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Profile Avatar + Personal Greeting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onProfileClick() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFFACC15).copy(alpha = 0.9f), CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showLocalImage && imageRequest != null) {
                                coil.compose.SubcomposeAsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    error = {
                                        Text(
                                            text = avatarLetters,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                )
                            } else {
                                Text(
                                    text = avatarLetters,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            val subGreeting = when {
                                event.id.contains("janmashtami") -> "Radhe Radhe,"
                                event.id.contains("ram_navami") -> "Jai Shri Ram,"
                                event.id.contains("eid") -> "Khair Mubarak,"
                                event.id.contains("diwali") -> "Shubh Deepavali,"
                                event.id.contains("chhath") -> "Jai Chhathi Maiya,"
                                event.id.contains("teachers") -> "Respects,"
                                else -> "Greetings,"
                            }
                            Text(
                                text = subGreeting,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = firstName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Formatted Real Date Tag
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Prominent Festival Title (High Contrast Poster Style)
                Text(
                    text = event.greeting,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                    lineHeight = 38.sp,
                    modifier = Modifier.padding(end = 60.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / Description Text
                Text(
                    text = event.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(end = 80.dp)
                )

                // Bottom padding space reserved for overlapping earnings cards
                Spacer(modifier = Modifier.height(44.dp))
            }
        }
    }
}

/**
 * Backward compatible composable banner wrapping FestivalHeroEnvironment.
 */
@Composable
fun FestivalHeaderBanner(
    event: FestivalEvent,
    modifier: Modifier = Modifier
) {
    val todayCal = remember { java.util.Calendar.getInstance() }
    val formattedDate = remember {
        java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault()).format(todayCal.time)
    }

    FestivalHeroEnvironment(
        event = event,
        displayName = "User",
        formattedDate = formattedDate,
        modifier = modifier
    )
}
