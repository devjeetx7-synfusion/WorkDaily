package com.synfusion.attendance.ui.responsive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT, // < 600dp
    MEDIUM,  // 600dp - 839dp
    EXPANDED // >= 840dp
}

enum class WindowHeightClass {
    COMPACT, // < 480dp
    MEDIUM,  // 480dp - 899dp
    EXPANDED // >= 900dp
}

data class WindowSizeInfo(
    val widthClass: WindowWidthClass = WindowWidthClass.COMPACT,
    val heightClass: WindowHeightClass = WindowHeightClass.MEDIUM,
    val screenWidthDp: Dp = 360.dp,
    val screenHeightDp: Dp = 640.dp
) {
    val isCompact: Boolean get() = widthClass == WindowWidthClass.COMPACT
    val isMedium: Boolean get() = widthClass == WindowWidthClass.MEDIUM
    val isExpanded: Boolean get() = widthClass == WindowWidthClass.EXPANDED
    val isLandscape: Boolean get() = screenWidthDp > screenHeightDp

    val maxContentWidth: Dp
        get() = Dp.Unspecified

    val maxFormWidth: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> Dp.Unspecified
            WindowWidthClass.MEDIUM -> 560.dp
            WindowWidthClass.EXPANDED -> 680.dp
        }

    val smallSpacing: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> 8.dp
            WindowWidthClass.MEDIUM -> 12.dp
            WindowWidthClass.EXPANDED -> 16.dp
        }

    val mediumSpacing: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> 16.dp
            WindowWidthClass.MEDIUM -> 20.dp
            WindowWidthClass.EXPANDED -> 24.dp
        }

    val largeSpacing: Dp
        get() = when (widthClass) {
            WindowWidthClass.COMPACT -> 24.dp
            WindowWidthClass.MEDIUM -> 32.dp
            WindowWidthClass.EXPANDED -> 40.dp
        }
}

val LocalWindowSizeInfo = staticCompositionLocalOf { WindowSizeInfo() }

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp

    val widthClass = when {
        configuration.screenWidthDp < 600 -> WindowWidthClass.COMPACT
        configuration.screenWidthDp < 840 -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    val heightClass = when {
        configuration.screenHeightDp < 480 -> WindowHeightClass.COMPACT
        configuration.screenHeightDp < 900 -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }

    return WindowSizeInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp
    )
}

@Composable
fun ProvideWindowSizeInfo(
    windowSizeInfo: WindowSizeInfo = rememberWindowSizeInfo(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalWindowSizeInfo provides windowSizeInfo) {
        content()
    }
}

@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = LocalWindowSizeInfo.current.maxContentWidth,
    alignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = alignment
    ) {
        if (maxContentWidth != Dp.Unspecified) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxSize()
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
