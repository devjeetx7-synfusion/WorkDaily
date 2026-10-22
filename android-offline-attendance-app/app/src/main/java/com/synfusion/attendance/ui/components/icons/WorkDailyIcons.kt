package com.synfusion.attendance.ui.components.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor

/**
 * WorkDaily Centralized Icon Registry (Lucide Icon System V2)
 *
 * Provides a clean, lightweight, outline-styled, theme-aware icon system across WorkDaily.
 * Standardizes sizes, strokes, and accessibility descriptions.
 */
object WorkDailyIcons {
    // Navigation & General
    val Home: ImageVector get() = Icons.Outlined.Home
    val HomeFilled: ImageVector get() = Icons.Filled.Home
    val Calendar: ImageVector get() = Icons.Outlined.CalendarToday
    val CalendarFilled: ImageVector get() = Icons.Filled.CalendarToday
    val Stats: ImageVector get() = Icons.Outlined.BarChart
    val StatsFilled: ImageVector get() = Icons.Filled.BarChart
    val LiveTv: ImageVector get() = Icons.Outlined.LiveTv
    val LiveTvFilled: ImageVector get() = Icons.Filled.LiveTv
    val Settings: ImageVector get() = Icons.Outlined.Settings
    val User: ImageVector get() = Icons.Outlined.Person
    val UserCheck: ImageVector get() = Icons.Outlined.HowToReg
    val UserAdd: ImageVector get() = Icons.Outlined.PersonAdd
    val Group: ImageVector get() = Icons.Outlined.Group
    val ArrowLeft: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
    val MoreVertical: ImageVector get() = Icons.Outlined.MoreVert
    val Search: ImageVector get() = Icons.Outlined.Search
    val Bell: ImageVector get() = Icons.Outlined.Notifications
    val BellFilled: ImageVector get() = Icons.Filled.Notifications
    val Close: ImageVector get() = Icons.Outlined.Close
    val Check: ImageVector get() = Icons.Outlined.Check
    val ChevronLeft: ImageVector get() = Icons.Outlined.ChevronLeft
    val ChevronRight: ImageVector get() = Icons.Outlined.ChevronRight
    val ChevronDown: ImageVector get() = Icons.Outlined.KeyboardArrowDown

    // Attendance & Work
    val Briefcase: ImageVector get() = Icons.Outlined.WorkOutline
    val CheckCircle: ImageVector get() = Icons.Outlined.CheckCircle
    val CancelCircle: ImageVector get() = Icons.Outlined.Cancel
    val Clock: ImageVector get() = Icons.Outlined.Schedule
    val History: ImageVector get() = Icons.Outlined.History
    val Edit: ImageVector get() = Icons.Outlined.Edit
    val Delete: ImageVector get() = Icons.Outlined.DeleteOutline

    // Financial & Wallet
    val Wallet: ImageVector get() = Icons.Outlined.AccountBalanceWallet
    val Calculator: ImageVector get() = Icons.Outlined.Calculate
    val TrendingUp: ImageVector get() = Icons.Outlined.TrendingUp
    val Receipt: ImageVector get() = Icons.Outlined.ReceiptLong
    val FileText: ImageVector get() = Icons.Outlined.Description

    // PDF & Export
    val Download: ImageVector get() = Icons.Outlined.FileDownload
    val Share: ImageVector get() = Icons.Outlined.Share
    val Print: ImageVector get() = Icons.Outlined.Print
    val ZoomIn: ImageVector get() = Icons.Outlined.ZoomIn
    val ZoomOut: ImageVector get() = Icons.Outlined.ZoomOut

    // Live & Streaming
    val Video: ImageVector get() = Icons.Outlined.Videocam
    val Camera: ImageVector get() = Icons.Outlined.PhotoCamera
    val Chat: ImageVector get() = Icons.AutoMirrored.Filled.Chat
    val Heart: ImageVector get() = Icons.Outlined.FavoriteBorder
    val HeartFilled: ImageVector get() = Icons.Filled.Favorite
    val Gift: ImageVector get() = Icons.Outlined.CardGiftcard
    val Diamond: ImageVector get() = Icons.Outlined.Diamond
    val Shield: ImageVector get() = Icons.Outlined.Shield
    val Lock: ImageVector get() = Icons.Outlined.Lock
    val Info: ImageVector get() = Icons.Outlined.Info
    val HelpCircle: ImageVector get() = Icons.AutoMirrored.Filled.Help

    // Standard Icon Size Metrics
    val SizeSmall = 16.dp
    val SizeMedium = 20.dp
    val SizeLarge = 24.dp
}

/**
 * Standard Reusable Icon Composable using `WorkDailyIcons`
 */
@Composable
fun WorkDailyIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = WorkDailyIcons.SizeLarge,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
