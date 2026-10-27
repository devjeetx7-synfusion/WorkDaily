package com.synfusion.attendance.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for the Date of Birth value used across WorkDaily.
 *
 * The canonical format is the ISO-8601 "yyyy-MM-dd" string stored in
 * `PublicProfile.dateOfBirth` (Firestore) and parsed by the `calculateAge(...)` helpers.
 * The local Room column `UserEntity.dateOfBirth` stores epoch milliseconds, so the
 * lossless conversions between the two representations live here as well.
 */
object DateOfBirthUtils {

    /** Canonical formatted length, e.g. `1994-05-21`. */
    const val FORMATTED_LENGTH = 10

    /** Number of digits a complete date of birth contains (yyyyMMdd). */
    private const val MAX_DIGITS = 8

    /** Guards against implausible dates without hard-coding a minimum birth year. */
    private const val MAX_AGE_YEARS = 120L

    /**
     * Normalises raw keystrokes into the canonical "yyyy-MM-dd" format.
     *
     * Only digits are accepted - the Settings field uses a numeric keypad because Compose
     * exposes no date keyboard type - and the two '-' separators are inserted automatically.
     * The result always matches the format the rest of the app expects, and re-formatting an
     * already formatted value is idempotent, so editing and deleting behave predictably.
     */
    fun formatInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(MAX_DIGITS)
        val builder = StringBuilder(FORMATTED_LENGTH)
        digits.forEachIndexed { index, digit ->
            if (index == 4 || index == 6) builder.append('-')
            builder.append(digit)
        }
        return builder.toString()
    }

    /**
     * Validates a date of birth and returns a user-facing message, or `null` when acceptable.
     *
     * A blank or partially typed value is accepted (returns `null`) so input is never blocked
     * mid-entry. Once all [MAX_DIGITS] digits are present the value must be a real calendar
     * date, must not be in the future, and must not be older than [MAX_AGE_YEARS].
     */
    fun validate(dob: String?): String? {
        if (dob.isNullOrBlank()) return null
        if (dob.filter { it.isDigit() }.length < MAX_DIGITS) return null
        val birth = parse(dob) ?: return "Enter a valid date of birth"
        val today = LocalDate.now()
        return when {
            birth.isAfter(today) -> "Date of birth cannot be in the future"
            birth.isBefore(today.minusYears(MAX_AGE_YEARS)) -> "Enter a valid date of birth"
            else -> null
        }
    }

    /**
     * True only when [dob] is a complete canonical date, is a real calendar date,
     * is not in the future, and is not implausibly old.
     */
    fun isValid(dob: String?): Boolean {
        if (dob == null || dob.length != FORMATTED_LENGTH) return false
        if (dob.filter { it.isDigit() }.length < MAX_DIGITS) return false
        return validate(dob) == null
    }

    /** Parses a canonical "yyyy-MM-dd" string, returning `null` for any invalid calendar date. */
    fun parse(dob: String?): LocalDate? {
        if (dob.isNullOrBlank()) return null
        val parts = dob.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        return try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts a canonical date of birth to epoch millis at start-of-day for local storage.
     * Returns `null` for an incomplete, invalid or future date so bad input is never persisted.
     */
    fun toEpochMillis(dob: String?): Long? {
        if (!isValid(dob)) return null
        val parsed = parse(dob) ?: return null
        return parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Converts the local epoch-millis column back to the canonical "yyyy-MM-dd" string. */
    fun fromEpochMillis(millis: Long?): String? {
        if (millis == null) return null
        return try {
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        } catch (_: Exception) {
            null
        }
    }
}
