package com.synfusion.workdailyadmin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary Brand Palette (WorkDaily Signature Emerald)
val AdminBrandPrimary = Color(0xFF0F9F7A)
val AdminBrandPrimaryVariant = Color(0xFF0A7A5D)
val AdminBrandPrimaryLight = Color(0xFFE6F4F0)
val AdminBrandAccent = Color(0xFF10B981)

// Neutral SaaS Slate Scale
val AdminSlate900 = Color(0xFF0F172A)
val AdminSlate800 = Color(0xFF1E293B)
val AdminSlate700 = Color(0xFF334155)
val AdminSlate600 = Color(0xFF475569)
val AdminSlate500 = Color(0xFF64748B)
val AdminSlate400 = Color(0xFF94A3B8)
val AdminSlate300 = Color(0xFFCBD5E1)
val AdminSlate200 = Color(0xFFE2E8F0)
val AdminSlate100 = Color(0xFFF1F5F9)
val AdminSlate50 = Color(0xFFF8FAFC)

// Semantic Status Tokens
val AdminSuccessGreen = Color(0xFF10B981)
val AdminSuccessBg = Color(0xFFDCFCE7)
val AdminWarningAmber = Color(0xFFF59E0B)
val AdminWarningBg = Color(0xFFFEF3C7)
val AdminErrorRed = Color(0xFFEF4444)
val AdminErrorBg = Color(0xFFFEE2E2)
val AdminInfoBlue = Color(0xFF3B82F6)
val AdminInfoBg = Color(0xFFDBEAFE)
val AdminPurple = Color(0xFF8B5CF6)
val AdminPurpleBg = Color(0xFFEDE9FE)

// Surfaces and Backgrounds
val AdminDarkBackground = Color(0xFF0B0F17)
val AdminDarkSurface = Color(0xFF151C28)
val AdminDarkSurfaceVariant = Color(0xFF1E293B)
val AdminDarkBorder = Color(0xFF334155)

val AdminLightBackground = Color(0xFFF8FAFC)
val AdminLightSurface = Color(0xFFFFFFFF)
val AdminLightSurfaceVariant = Color(0xFFF1F5F9)
val AdminLightBorder = Color(0xFFE2E8F0)

val AdminTextPrimaryDark = Color(0xFFF8FAFC)
val AdminTextSecondaryDark = Color(0xFF94A3B8)
val AdminTextPrimaryLight = Color(0xFF0F172A)
val AdminTextSecondaryLight = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = AdminBrandPrimary,
    onPrimary = Color.White,
    primaryContainer = AdminBrandPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = AdminInfoBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = AdminBrandAccent,
    background = AdminDarkBackground,
    surface = AdminDarkSurface,
    surfaceVariant = AdminDarkSurfaceVariant,
    onBackground = AdminTextPrimaryDark,
    onSurface = AdminTextPrimaryDark,
    onSurfaceVariant = AdminTextSecondaryDark,
    outline = AdminDarkBorder,
    error = AdminErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AdminBrandPrimary,
    onPrimary = Color.White,
    primaryContainer = AdminBrandPrimaryLight,
    onPrimaryContainer = AdminBrandPrimaryVariant,
    secondary = AdminInfoBlue,
    onSecondary = Color.White,
    secondaryContainer = AdminInfoBg,
    onSecondaryContainer = Color(0xFF1E40AF),
    tertiary = AdminBrandAccent,
    background = AdminLightBackground,
    surface = AdminLightSurface,
    surfaceVariant = AdminLightSurfaceVariant,
    onBackground = AdminTextPrimaryLight,
    onSurface = AdminTextPrimaryLight,
    onSurfaceVariant = AdminTextSecondaryLight,
    outline = AdminLightBorder,
    error = AdminErrorRed,
    onError = Color.White
)

@Composable
fun WorkDailyAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
