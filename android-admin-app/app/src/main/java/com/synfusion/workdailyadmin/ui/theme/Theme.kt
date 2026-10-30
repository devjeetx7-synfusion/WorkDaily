package com.synfusion.workdailyadmin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AdminBrandPrimary = Color(0xFF0F9F7A)
val AdminBrandPrimaryVariant = Color(0xFF08634C)
val AdminAccentBlue = Color(0xFF3B82F6)
val AdminDarkBackground = Color(0xFF111827)
val AdminDarkSurface = Color(0xFF1F2937)
val AdminDarkSurfaceVariant = Color(0xFF374151)
val AdminTextPrimary = Color(0xFFF9FAFB)
val AdminTextSecondary = Color(0xFF9CA3AF)
val AdminErrorRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = AdminBrandPrimary,
    onPrimary = Color.White,
    primaryContainer = AdminBrandPrimaryVariant,
    secondary = AdminAccentBlue,
    background = AdminDarkBackground,
    surface = AdminDarkSurface,
    surfaceVariant = AdminDarkSurfaceVariant,
    onBackground = AdminTextPrimary,
    onSurface = AdminTextPrimary,
    error = AdminErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = AdminBrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4F0),
    secondary = AdminAccentBlue,
    background = Color(0xFFF3F4F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E7EB),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    error = AdminErrorRed
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
