package com.synfusion.attendance.online

import java.util.Locale

sealed class UsernameValidationResult {
    object Valid : UsernameValidationResult()
    data class Invalid(val reason: String) : UsernameValidationResult()
}

/**
 * Utility for validating WorkDaily unique usernames and generating valid initial username candidates.
 */
object UsernameValidator {

    private val USERNAME_REGEX = Regex("^[a-z0-9_.]{3,20}$")

    private val RESERVED_USERNAMES = setOf(
        "admin", "administrator", "workdaily", "synfusion", "official", "support",
        "root", "null", "system", "anonymous", "creator", "host", "user", "help",
        "api", "bot", "privacy", "terms", "settings", "profile", "dashboard",
        "verified", "service", "live", "feedback", "security"
    )

    fun normalize(username: String): String {
        return username.trim().lowercase(Locale.US)
    }

    fun validate(rawUsername: String): UsernameValidationResult {
        val normalized = normalize(rawUsername)

        if (normalized.length < 3) {
            return UsernameValidationResult.Invalid("Username must be at least 3 characters")
        }

        if (normalized.length > 20) {
            return UsernameValidationResult.Invalid("Username cannot exceed 20 characters")
        }

        if (!USERNAME_REGEX.matches(normalized)) {
            return UsernameValidationResult.Invalid("Only lowercase letters, numbers, underscores, and periods allowed")
        }

        if (normalized.startsWith(".") || normalized.endsWith(".") || normalized.contains("..")) {
            return UsernameValidationResult.Invalid("Username cannot start/end with a period or have consecutive periods")
        }

        if (RESERVED_USERNAMES.contains(normalized)) {
            return UsernameValidationResult.Invalid("This username is reserved by the system")
        }

        return UsernameValidationResult.Valid
    }

    /**
     * Generates a clean, valid initial username candidate from display name or Firebase UID.
     */
    fun generateCandidate(displayName: String?, uid: String): String {
        val base = displayName?.lowercase(Locale.US)?.replace(Regex("[^a-z0-9_]"), "")?.take(14)
            ?: "worker"

        val cleanBase = if (base.length < 3) "worker" else base
        val suffix = uid.takeLast(4).lowercase(Locale.US).replace(Regex("[^a-z0-9_]"), "7")
        val candidate = "${cleanBase}_$suffix".take(20)

        return if (validate(candidate) is UsernameValidationResult.Valid) {
            candidate
        } else {
            "user_${uid.takeLast(8).lowercase(Locale.US).replace(Regex("[^a-z0-9_]"), "0")}".take(20)
        }
    }
}
