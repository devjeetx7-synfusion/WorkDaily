package com.synfusion.attendance.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
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
import com.synfusion.attendance.data.FestivalCategory
import com.synfusion.attendance.data.FestivalEvent
import com.synfusion.attendance.data.FestivalPreviewManager
import com.synfusion.attendance.data.FestivalRepository
import com.synfusion.attendance.ui.responsive.LocalWindowSizeInfo
import com.synfusion.attendance.ui.responsive.ResponsiveContainer
import com.synfusion.attendance.utils.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestivalPreviewScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activePreviewEvent by FestivalPreviewManager.activePreviewEvent.collectAsStateWithLifecycle(initialValue = null)
    val specialFestivals = remember { FestivalRepository.getSpecialHomeEventsForPreview() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Festival Preview",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    if (activePreviewEvent != null) {
                        TextButton(
                            onClick = {
                                FestivalPreviewManager.clearPreview()
                                Toast.makeText(context, "Restored real date festival detection", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Use Real Date",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        ResponsiveContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro Explanation Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Test how WorkDaily celebrates each Special Festival. Preview mode does not change your real date or data.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Active Preview Status Indicator Banner
                if (activePreviewEvent != null) {
                    item {
                        val activeEvent = activePreviewEvent!!
                        val activePrimaryColor = remember(activeEvent.primaryColorHex) { Color(activeEvent.primaryColorHex) }
                        val activeAccentColor = remember(activeEvent.accentColorHex) { Color(activeEvent.accentColorHex) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    1.5.dp,
                                    Brush.horizontalGradient(listOf(activePrimaryColor, activeAccentColor)),
                                    RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = activePrimaryColor.copy(alpha = 0.12f)
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(activePrimaryColor.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = activeEvent.emoji, fontSize = 22.sp)
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = activePrimaryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Festival Preview Active",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activePrimaryColor
                                            )
                                        }
                                        Text(
                                            text = activeEvent.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        FestivalPreviewManager.clearPreview()
                                        Toast.makeText(context, "Exit Festival Preview", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(text = "Exit Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Section Header
                item {
                    Text(
                        text = "Available Special Festivals (${specialFestivals.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                // Special Festival Cards
                items(
                    items = specialFestivals,
                    key = { it.id }
                ) { festival ->
                    val isCurrentActive = activePreviewEvent?.name == festival.name
                    FestivalPreviewCard(
                        event = festival,
                        isActive = isCurrentActive,
                        onPreviewClick = {
                            FestivalPreviewManager.setPreviewFestival(festival)
                            Toast.makeText(context, "Previewing ${festival.name} on Home", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FestivalPreviewCard(
    event: FestivalEvent,
    isActive: Boolean,
    onPreviewClick: () -> Unit
) {
    val primaryColor = remember(event.primaryColorHex) { Color(event.primaryColorHex) }
    val secondaryColor = remember(event.secondaryColorHex) { Color(event.secondaryColorHex) }
    val accentColor = remember(event.accentColorHex) { Color(event.accentColorHex) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onPreviewClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            secondaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Circular Festival Emoji/Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.18f))
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = event.emoji,
                        fontSize = 30.sp
                    )
                }

                // Festival Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val typeText = if (event.isPublicHoliday) "Special Festival • Public Holiday" else "Special Festival"
                    Text(
                        text = typeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = primaryColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = event.greeting,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = event.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }

                // Action Button / Badge
                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onPreviewClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Preview",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
