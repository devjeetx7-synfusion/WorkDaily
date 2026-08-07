package com.synfusion.attendance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Base Colors
val MintPrimary = Color(0xFF0F9F7A)      // Deep Premium Emerald Mint
val BlueSecondary = Color(0xFF2563EB)    // Royal Premium Blue
val OrangeTertiary = Color(0xFFEA580C)   // Deep Premium Orange
val PurpleLeave = Color(0xFF7C3AED)      // Rich Premium Violet

// Light Mode Theme Colors
val Primary = MintPrimary
val Secondary = BlueSecondary
val Tertiary = OrangeTertiary
val PrimaryForeground = Color(0xFFFFFFFF)
val Background = Color(0xFFF8FAFB)       // Premium clean off-white
val Surface = Color(0xFFFFFFFF)
val Card = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0F172A)      // slate-900 (High Contrast)
val TextSecondary = Color(0xFF475569)    // slate-600 (High Contrast)
val Border = Color(0xFFE2E8F0)          // slate-200 (Better contrast divider)
val Muted = Color(0xFFF1F5F9)           // slate-100
val Error = Color(0xFFDC2626)           // Premium Rose/Red

// Dark Mode Theme Colors
val PrimaryDark = Color(0xFF10B981)      // Bright Emerald Mint for AMOLED
val SecondaryDark = Color(0xFF60A5FA)    // Soft Blue
val TertiaryDark = Color(0xFFFDBA74)     // Soft Orange
val BackgroundDark = Color(0xFF020617)   // slate-950 (Premium Dark Slate/Black)
val SurfaceDark = Color(0xFF0F172A)      // slate-900
val CardDark = Color(0xFF1E293B)         // slate-800
val TextPrimaryDark = Color(0xFFF8FAFC)  // slate-50
val TextSecondaryDark = Color(0xFF94A3B8) // slate-400
val BorderDark = Color(0xFF334155)       // slate-700
val MutedDark = Color(0xFF1E293B)        // slate-800
val ErrorDark = Color(0xFFFB7185)        // Soft Rose/Red

// Premium Status Colors (Accessible & Vibrant)
// Light Theme Statuses
val StatusPresentLight = Color(0xFF0F9F7A)   // Emerald/Green
val StatusAbsentLight = Color(0xFFDC2626)    // Rose/Red
val StatusHalfDayLight = Color(0xFFEA580C)   // Amber/Orange
val StatusHolidayLight = Color(0xFF2563EB)   // Blue
val StatusLeaveLight = Color(0xFF7C3AED)     // Violet/Purple

// Dark Theme Statuses
val StatusPresentDark = Color(0xFF34D399)    // Vibrant Soft Green
val StatusAbsentDark = Color(0xFFFB7185)     // Vibrant Soft Rose
val StatusHalfDayDark = Color(0xFFFDBA74)    // Vibrant Soft Amber
val StatusHolidayDark = Color(0xFF60A5FA)    // Vibrant Soft Blue
val StatusLeaveDark = Color(0xFFC084FC)      // Vibrant Soft Violet

object AttendanceStatusColors {
    @Composable
    fun getPresent(isDark: Boolean = isSystemInDarkTheme()): Color =
        if (isDark) StatusPresentDark else StatusPresentLight

    @Composable
    fun getAbsent(isDark: Boolean = isSystemInDarkTheme()): Color =
        if (isDark) StatusAbsentDark else StatusAbsentLight

    @Composable
    fun getHalfDay(isDark: Boolean = isSystemInDarkTheme()): Color =
        if (isDark) StatusHalfDayDark else StatusHalfDayLight

    @Composable
    fun getHoliday(isDark: Boolean = isSystemInDarkTheme()): Color =
        if (isDark) StatusHolidayDark else StatusHolidayLight

    @Composable
    fun getLeave(isDark: Boolean = isSystemInDarkTheme()): Color =
        if (isDark) StatusLeaveDark else StatusLeaveLight
}
