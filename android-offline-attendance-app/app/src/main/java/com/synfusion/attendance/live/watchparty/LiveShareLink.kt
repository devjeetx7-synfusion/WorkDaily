package com.synfusion.attendance.live.watchparty

import java.net.URI
import java.net.URLEncoder

/**
 * Stable WorkDaily Live share links.
 *
 * A room id is the Firestore document id of `live_rooms/{roomId}`, which is already a stable,
 * non-guessable identifier. Both an https link (shareable outside the app) and an app scheme link
 * (used as a fallback deep link) resolve to the same room id.
 */
object LiveShareLink {

    const val WEB_HOST = "workdaily.app"
    const val HTTPS_PREFIX = "https://$WEB_HOST/live/"
    const val APP_SCHEME_PREFIX = "workdaily://live/"

    private val ROOM_ID_REGEX = Regex("^[A-Za-z0-9_-]{4,128}$")

    fun isValidRoomId(roomId: String?): Boolean = roomId != null && ROOM_ID_REGEX.matches(roomId.trim())

    fun buildShareUrl(roomId: String): String = HTTPS_PREFIX + roomId.trim()

    fun buildAppLink(roomId: String): String = APP_SCHEME_PREFIX + roomId.trim()

    /**
     * Human readable share text. Only room metadata the host already shows publicly is included;
     * private room notes, diamonds or participation details are never part of the link payload.
     */
    fun buildShareText(
        roomId: String,
        title: String,
        hostName: String,
        isWatchParty: Boolean
    ): String {
        val kind = if (isWatchParty) "Watch Party" else "Live"
        val safeTitle = title.trim().ifBlank { "WorkDaily Live" }
        val safeHost = hostName.trim().ifBlank { "A WorkDaily host" }
        return "$safeHost is hosting \"$safeTitle\" ($kind) on WorkDaily. Join here: ${buildShareUrl(roomId)}"
    }

    fun buildShareSubject(title: String): String = "Join \"${title.trim().ifBlank { "WorkDaily Live" }}\" on WorkDaily"

    /** Android ACTION_SEND payload (WhatsApp, etc.) keeps the link on its own line. */
    fun buildShareBody(roomId: String, title: String, hostName: String, isWatchParty: Boolean): String =
        buildShareText(roomId, title, hostName, isWatchParty)

    fun buildEncodedShareUrl(roomId: String): String =
        URLEncoder.encode(buildShareUrl(roomId), "UTF-8")

    /**
     * Resolves a room id from any supported link/URL. Returns `null` for unrelated links so the
     * app never navigates to a room based on an arbitrary URL.
     */
    fun parseRoomId(rawLink: String?): String? {
        val link = rawLink?.trim().orEmpty()
        if (link.isEmpty()) return null

        if (link.startsWith(APP_SCHEME_PREFIX, ignoreCase = true)) {
            val id = link.substring(APP_SCHEME_PREFIX.length).substringBefore('/').substringBefore('?')
            return id.takeIf(::isValidRoomId)
        }

        val uri = try {
            URI(if (link.contains("://")) link else "https://$link")
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host != WEB_HOST) return null

        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        if (!segments[0].equals("live", ignoreCase = true)) return null
        return segments[1].takeIf(::isValidRoomId)
    }
}
