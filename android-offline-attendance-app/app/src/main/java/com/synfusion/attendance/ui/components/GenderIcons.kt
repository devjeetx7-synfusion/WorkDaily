package com.synfusion.attendance.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gender symbols for WorkDaily, drawn as vector paths.
 *
 * These are real geometry rather than the ♂ / ♀ / ⚧ text glyphs on purpose: emoji and symbol
 * rendering varies by platform, font and Android version, and several fonts ship no transgender
 * symbol at all, which previously produced a blank or tofu box. Vector paths render identically
 * everywhere and accept a theme tint.
 *
 * Built with [ImageVector.Builder] following the same pattern as `SocialIcons.kt`.
 */

/** The three gender identities the app persists. Values are the existing stored strings. */
enum class GenderIdentity(val storedValue: String) {
    MALE("Male"),
    FEMALE("Female"),
    TRANSGENDER("Transgender")
}

/**
 * Maps a persisted gender string to a [GenderIdentity], tolerating case and whitespace variants.
 *
 * Returns `null` for a blank or unrecognised value. That is deliberate: guessing would silently
 * assign someone a gender they never chose, and an unknown legacy value such as "Other" must not
 * be coerced into one of these three. Callers render no symbol in that case.
 *
 * Only unambiguous variants are mapped, so a user's stored value is never reinterpreted.
 */
fun genderIdentityFrom(value: String?): GenderIdentity? = when (value?.trim()?.lowercase()) {
    null, "" -> null
    "male", "m", "man", "boy" -> GenderIdentity.MALE
    "female", "f", "woman", "girl" -> GenderIdentity.FEMALE
    "transgender", "trans", "t" -> GenderIdentity.TRANSGENDER
    else -> null
}

/** Male symbol ♂ : a circle with an arrow pointing to the upper right. */
val GenderMaleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "GenderMale",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        ) {
            // Circle: centre (10, 14), radius 5.5 -- drawn as two semicircular arcs.
            moveTo(15.5f, 14f)
            arcTo(5.5f, 5.5f, 0f, false, true, 4.5f, 14f)
            arcTo(5.5f, 5.5f, 0f, false, true, 15.5f, 14f)
            // Arrow shaft toward the upper right.
            moveTo(13.9f, 10.1f)
            lineTo(20.5f, 3.5f)
            // Arrow head.
            moveTo(15.4f, 3.5f)
            lineTo(20.5f, 3.5f)
            lineTo(20.5f, 8.6f)
        }
    }.build()

/** Female symbol ♀ : a circle with a cross beneath it. */
val GenderFemaleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "GenderFemale",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        ) {
            // Circle: centre (12, 8), radius 5.5.
            moveTo(17.5f, 8f)
            arcTo(5.5f, 5.5f, 0f, false, true, 6.5f, 8f)
            arcTo(5.5f, 5.5f, 0f, false, true, 17.5f, 8f)
            // Vertical stem.
            moveTo(12f, 13.5f)
            lineTo(12f, 21f)
            // Cross bar.
            moveTo(8.5f, 17.5f)
            lineTo(15.5f, 17.5f)
        }
    }.build()

/**
 * Transgender symbol ⚧ : the male arrow, the female cross, and a third barred stroke to the
 * upper left, all around one shared circle.
 */
val GenderTransgenderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "GenderTransgender",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        ) {
            // Shared circle: centre (12, 12), radius 5.
            moveTo(17f, 12f)
            arcTo(5f, 5f, 0f, false, true, 7f, 12f)
            arcTo(5f, 5f, 0f, false, true, 17f, 12f)
            // Male arrow to the upper right.
            moveTo(15.6f, 8.4f)
            lineTo(21f, 3f)
            moveTo(16.6f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 7.4f)
            // Female cross below.
            moveTo(12f, 17f)
            lineTo(12f, 22f)
            moveTo(9.5f, 19.8f)
            lineTo(14.5f, 19.8f)
            // Third stroke to the upper left with a perpendicular bar.
            moveTo(8.4f, 8.4f)
            lineTo(3.5f, 3.5f)
            moveTo(3.5f, 5.7f)
            lineTo(5.7f, 3.5f)
        }
    }.build()

/** The vector for an identity, or `null` when the gender is unset or unrecognised. */
fun genderIconFor(identity: GenderIdentity?): ImageVector? = when (identity) {
    GenderIdentity.MALE -> GenderMaleIcon
    GenderIdentity.FEMALE -> GenderFemaleIcon
    GenderIdentity.TRANSGENDER -> GenderTransgenderIcon
    null -> null
}

/**
 * Theme-aware symbol colour: blue for male, pink for female, purple for transgender.
 *
 * The app has a manual dark-mode toggle, so this reads the luminance of the active surface rather
 * than the system setting, and picks a lighter variant on dark backgrounds. Every pairing clears
 * the 3:1 non-text contrast minimum: on white the dark variants score roughly 4.6:1, 5.5:1 and
 * 6.4:1, and on a #121212 surface the light variants score roughly 7.5:1, 8.5:1 and 7.4:1.
 */
@Composable
fun genderColor(identity: GenderIdentity): Color {
    val onDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (identity) {
        GenderIdentity.MALE -> if (onDarkSurface) Color(0xFF64B5F6) else Color(0xFF1976D2)
        GenderIdentity.FEMALE -> if (onDarkSurface) Color(0xFFF48FB1) else Color(0xFFD81B60)
        GenderIdentity.TRANSGENDER -> if (onDarkSurface) Color(0xFFCE93D8) else Color(0xFF8E24AA)
    }
}

/**
 * Renders the symbol for a persisted gender string.
 *
 * Draws nothing when the value is blank or unrecognised, so an unset gender is shown as unset
 * instead of being defaulted to one of the three. The content description names the gender for
 * screen readers, and is also omitted when there is nothing to announce.
 */
@Composable
fun GenderSymbol(
    gender: String?,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val identity = genderIdentityFrom(gender) ?: return
    val icon = genderIconFor(identity) ?: return
    Icon(
        imageVector = icon,
        contentDescription = "${identity.storedValue} gender",
        tint = genderColor(identity),
        modifier = modifier.size(size)
    )
}
