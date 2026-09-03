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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val avatarLetters = remember(displayName) {
        val trimmed = displayName.trim()
        if (trimmed.length >= 2) trimmed.take(2).uppercase()
        else if (trimmed.isNotEmpty()) trimmed.take(1).uppercase()
        else "U"
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

    // Smooth Entrance Sequence (600-900ms)
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(event.id) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(700)) + expandVertically(animationSpec = tween(700)),
        exit = fadeOut(animationSpec = tween(400)) + shrinkVertically(animationSpec = tween(400))
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.5f),
                            accentColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                secondaryColor.copy(alpha = 0.18f),
                                accentColor.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        )
                    )
            ) {
                // Layer 1 — Festival Animation Canvas (Particles & Glows)
                FestivalAnimationCanvas(
                    event = event,
                    modifier = Modifier.matchParentSize()
                )

                // Layer 2 — High-Quality Festival Vector Artwork
                FestivalArtwork(
                    event = event,
                    modifier = Modifier.matchParentSize()
                )

                // Content Overlay (Layer 3 & 4 — Profile, Greeting, Real Date)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar + Greeting + Real Date
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onProfileClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, accentColor.copy(alpha = 0.8f), CircleShape)
                                    .background(primaryColor.copy(alpha = 0.25f)),
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
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    )
                                } else {
                                    Text(
                                        text = avatarLetters,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = event.greeting,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formattedDate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Circular Festival Emoji Badge
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.25f))
                                .border(1.5.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = event.emoji,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Festival Description / Wishes Sub-Text
                    Text(
                        text = event.description,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(start = 4.dp, end = 40.dp)
                    )
                }
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
