package com.synfusion.attendance.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R

@Composable
fun ExpandableNoteText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 2,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (text.isBlank()) return

    var isExpanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    var lineCount by remember { mutableStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .animateContentSize()
            .then(
                if (hasOverflow || lineCount > collapsedMaxLines) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null // keep it clean without ripple
                    ) {
                        isExpanded = !isExpanded
                    }
                } else Modifier
            )
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult: TextLayoutResult ->
                lineCount = textLayoutResult.lineCount
                if (!isExpanded) {
                    hasOverflow = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > collapsedMaxLines
                }
            }
        )
        if (hasOverflow || lineCount > collapsedMaxLines) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    if (isExpanded) R.string.show_less else R.string.show_more
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
