package com.synfusion.attendance.ui.onboarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingBottomControls(
    showBack: Boolean,
    backText: String,
    nextText: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val backPressed by backInteraction.collectIsPressedAsState()
    val nextPressed by nextInteraction.collectIsPressedAsState()

    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.96f else 1f,
        animationSpec = tween(110),
        label = "onboardingBackScale"
    )
    val nextScale by animateFloatAsState(
        targetValue = if (nextPressed) 0.97f else 1f,
        animationSpec = tween(110),
        label = "onboardingNextScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = showBack,
            enter = fadeIn(tween(180)) + expandHorizontally(
                animationSpec = tween(240),
                expandFrom = Alignment.Start
            ),
            exit = fadeOut(tween(140)) + shrinkHorizontally(
                animationSpec = tween(220),
                shrinkTowards = Alignment.Start
            )
        ) {
            OutlinedButton(
                onClick = onBack,
                interactionSource = backInteraction,
                modifier = Modifier
                    .widthIn(min = 108.dp)
                    .height(58.dp)
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    },
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = backText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = onNext,
            interactionSource = nextInteraction,
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                },
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text = nextText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
