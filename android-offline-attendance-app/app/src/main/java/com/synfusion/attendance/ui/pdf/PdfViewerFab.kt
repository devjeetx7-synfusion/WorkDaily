package com.synfusion.attendance.ui.pdf

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import kotlin.math.cos
import kotlin.math.sin

data class FabArcAction(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun PdfViewerFab(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "fab_rotation"
    )

    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "fab_expansion_progress"
    )

    val actions = listOf(
        FabArcAction("more", stringResource(R.string.pdf_more), Icons.Default.MoreHoriz, onMore),
        FabArcAction("save", stringResource(R.string.pdf_save), Icons.Default.Save, onSave),
        FabArcAction("share", stringResource(R.string.pdf_share), Icons.Default.Share, onShare)
    )

    // Base radius along which secondary buttons pop out in an arc (angles: 90, 135, 180 degrees -> top-left arc)
    val arcRadiusDp: Dp = 100.dp
    val angles = listOf(180.0, 135.0, 90.0) // 180 = Left, 135 = Top-Left, 90 = Top

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (progress > 0.01f) {
            actions.forEachIndexed { index, action ->
                val angleRad = Math.toRadians(angles[index])
                val targetOffsetX = (arcRadiusDp.value * cos(angleRad)).dp
                val targetOffsetY = -(arcRadiusDp.value * sin(angleRad)).dp

                val currentOffsetX = targetOffsetX * progress
                val currentOffsetY = targetOffsetY * progress

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset(x = currentOffsetX, y = currentOffsetY)
                        .scale(progress)
                        .alpha(progress)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onToggleExpanded()
                                action.onClick()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.title,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = action.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = onToggleExpanded,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.pdf_more),
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}
