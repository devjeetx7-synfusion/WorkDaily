package com.synfusion.attendance.live.watchparty

import java.net.URI
import java.net.URLDecoder

/**
 * Where a parsed YouTube selection came from.
 */
enum class YouTubeSource {
    VIDEO,
    SHORT,
    LIVE,
    EMBED,
    PLAYLIST
}

/**
 * A validated YouTube reference that the official embedded IFrame player can actually play.
 *
 * Exactly one of [videoId] / [playlistId] is mandatory; when both are present the player opens the
 * video inside its playlist (the same behavior as the YouTube website).
 */
data class YouTubeSelection(
    val videoId: String?,
    val playlistId: String?,
    val source: YouTubeSource,
    val canonicalUrl: String
) {
    val isPlaylistOnly: Boolean get() = videoId.isNullOrBlank() && !playlistId.isNullOrBlank()
    val isVideoPlayable: Boolean get() = !videoId.isNullOrBlank()

    /** Thumbnail served by YouTube's public image CDN for the selected video. */
    val thumbnailUrl: String? get() = videoId?.let(YouTubeLinkParser::thumbnailUrl)

    /** Stable compare key used for duplicate detection inside a playlist. */
    val dedupeKey: String get() = videoId ?: playlistId.orEmpty()
}

/** Reasons a pasted YouTube reference cannot be used by the official player. */
enum class YouTubeParseError {
    EMPTY,
    UNSUPPORTED_HOST,
    UNSUPPORTED_URL,
    INVALID_VIDEO_ID,
    INVALID_PLAYLIST_ID
}

sealed class YouTubeParseResult {
    data class Success(val selection: YouTubeSelection) : YouTubeParseResult()
    data class Failure(val error: YouTubeParseError, val message: String) : YouTubeParseResult()
}

/**
 * Strict parser for YouTube video/playlist references.
 *
 * Only official YouTube hosts are accepted; a URL such as `https://example.com/watch?v=ID` is
 * rejected instead of silently extracting the ID, which prevents users from pasting an
 * arbitrary redirect page that happens to contain a YouTube-looking query parameter.
 *
 * YouTube Music (`music.youtube.com`) watch links are supported because they address the same
 * public YouTube videos through the same official IFrame embed API - there is no separate
 * "YouTube Music API" integration here and the app never claims one.
 */
object YouTubeLinkParser {

    const val PLAYLIST_PREFIXES = "PL|UU|LL|FL|RD|OL|EC|WL|PU|SR"

    private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
    private val PLAYLIST_ID_REGEX = Regex("^($PLAYLIST_PREFIXES)[A-Za-z0-9_-]{2,80}$")
    private val VIDEO_ID_STRICT = Regex("^[A-Za-z0-9_-]{11}$")

    private val SUPPORTED_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com",
        "youtu.be",
        "www.youtu.be"
    )

    fun thumbnailUrl(videoId: String): String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

    fun watchUrl(videoId: String, playlistId: String? = null): String = buildString {
        append("https://www.youtube.com/watch?v=").append(videoId)
        if (!playlistId.isNullOrBlank()) append("&list=").append(playlistId)
    }

    fun playlistUrl(playlistId: String): String = "https://www.youtube.com/playlist?list=$playlistId"

    fun isValidVideoId(raw: String?): Boolean = raw != null && VIDEO_ID_STRICT.matches(raw.trim())

    fun isValidPlaylistId(raw: String?): Boolean =
        raw != null && PLAYLIST_ID_REGEX.matches(raw.trim())

    /**
     * Parses user input. A bare 11 character video ID or a playlist ID is accepted for
     * convenience; everything else must be a URL on a supported YouTube host.
     */
    fun parse(rawInput: String?): YouTubeParseResult {
        val input = rawInput?.trim().orEmpty()
        if (input.isEmpty()) {
            return YouTubeParseResult.Failure(YouTubeParseError.EMPTY, "Paste a YouTube video or playlist link.")
        }

        // Bare IDs (typed or copied from a share dialog).
        if (VIDEO_ID_REGEX.matches(input)) {
            return YouTubeParseResult.Success(
                YouTubeSelection(
                    videoId = input,
                    playlistId = null,
                    source = YouTubeSource.VIDEO,
                    canonicalUrl = watchUrl(input)
                )
            )
        }
        if (PLAYLIST_ID_REGEX.matches(input)) {
            return YouTubeParseResult.Success(
                YouTubeSelection(
                    videoId = null,
                    playlistId = input,
                    source = YouTubeSource.PLAYLIST,
                    canonicalUrl = playlistUrl(input)
                )
            )
        }

        val uri = try {
            val normalized = when {
                input.startsWith("//") -> "https:$input"
                input.contains("://") -> input
                input.startsWith("www.") || input.startsWith("m.") ||
                    input.startsWith("music.") || input.startsWith("youtu.be") -> "https://$input"
                else -> return YouTubeParseResult.Failure(
                    YouTubeParseError.UNSUPPORTED_URL,
                    "Enter a YouTube video or playlist link."
                )
            }
            URI(normalized)
        } catch (_: Exception) {
            return YouTubeParseResult.Failure(YouTubeParseError.UNSUPPORTED_URL, "That link is not a valid URL.")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return YouTubeParseResult.Failure(
                YouTubeParseError.UNSUPPORTED_URL,
                "Only http(s) YouTube links are supported."
            )
        }

        val host = uri.host?.lowercase()?.removePrefix("www.") ?: ""
        val isYoutuBe = host == "youtu.be"
        if (host !in SUPPORTED_HOSTS) {
            return YouTubeParseResult.Failure(
                YouTubeParseError.UNSUPPORTED_HOST,
                "Only youtube.com, youtu.be and music.youtube.com links are supported."
            )
        }

        val query = parseQuery(uri.rawQuery)
        val listId = query["list"]?.takeIf { it.isNotBlank() }

        if (isYoutuBe) {
            val pathId = uri.path.orEmpty().trim('/').substringBefore('/')
            if (!VIDEO_ID_REGEX.matches(pathId)) {
                return YouTubeParseResult.Failure(
                    YouTubeParseError.INVALID_VIDEO_ID,
                    "That youtu.be link does not contain a valid video ID."
                )
            }
            return YouTubeParseResult.Success(
                YouTubeSelection(
                    videoId = pathId,
                    playlistId = listId?.takeIf(::isValidPlaylistId),
                    source = YouTubeSource.SHORT,
                    canonicalUrl = watchUrl(pathId, listId?.takeIf(::isValidPlaylistId))
                )
            )
        }

        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        val first = segments.firstOrNull()?.lowercase()

        // /watch?v=ID
        if (first == "watch") {
            val videoId = query["v"]
            if (videoId != null && VIDEO_ID_REGEX.matches(videoId)) {
                val safeList = listId?.takeIf(::isValidPlaylistId)
                return YouTubeParseResult.Success(
                    YouTubeSelection(
                        videoId = videoId,
                        playlistId = safeList,
                        source = YouTubeSource.VIDEO,
                        canonicalUrl = watchUrl(videoId, safeList)
                    )
                )
            }
            if (listId != null && isValidPlaylistId(listId)) {
                return YouTubeParseResult.Success(
                    YouTubeSelection(
                        videoId = null,
                        playlistId = listId,
                        source = YouTubeSource.PLAYLIST,
                        canonicalUrl = playlistUrl(listId)
                    )
                )
            }
            return YouTubeParseResult.Failure(
                YouTubeParseError.INVALID_VIDEO_ID,
                "That YouTube link does not contain a valid video or playlist ID."
            )
        }

        // /playlist?list=ID
        if (first == "playlist") {
            if (listId != null && isValidPlaylistId(listId)) {
                return YouTubeParseResult.Success(
                    YouTubeSelection(
                        videoId = null,
                        playlistId = listId,
                        source = YouTubeSource.PLAYLIST,
                        canonicalUrl = playlistUrl(listId)
                    )
                )
            }
            return YouTubeParseResult.Failure(
                YouTubeParseError.INVALID_PLAYLIST_ID,
                "That playlist link does not contain a valid playlist ID."
            )
        }

        // /shorts/ID, /live/ID, /embed/ID, /v/ID
        val pathKind = when (first) {
            "shorts" -> YouTubeSource.SHORT
            "live" -> YouTubeSource.LIVE
            "embed", "v" -> YouTubeSource.EMBED
            else -> null
        }
        if (pathKind != null) {
            val videoId = segments.getOrNull(1)
            if (videoId == null || !VIDEO_ID_REGEX.matches(videoId)) {
                return YouTubeParseResult.Failure(
                    YouTubeParseError.INVALID_VIDEO_ID,
                    "That YouTube link does not contain a valid 11 character video ID."
                )
            }
            val safeList = listId?.takeIf(::isValidPlaylistId)
            return YouTubeParseResult.Success(
                YouTubeSelection(
                    videoId = videoId,
                    playlistId = safeList,
                    source = pathKind,
                    canonicalUrl = watchUrl(videoId, safeList)
                )
            )
        }

        return YouTubeParseResult.Failure(
            YouTubeParseError.UNSUPPORTED_URL,
            "Open a YouTube video or playlist, then paste its link here."
        )
    }

    /** Convenience helper used by the setup screen and playlist sheet. */
    fun parseVideoId(rawInput: String?): String? = (parse(rawInput) as? YouTubeParseResult.Success)?.selection?.videoId

    fun parsePlaylistId(rawInput: String?): String? =
        (parse(rawInput) as? YouTubeParseResult.Success)?.selection?.playlistId

    /** Extracts the watch/share URL that should be persisted for the selection. */
    fun canonicalUrlOrNull(rawInput: String?): String? =
        (parse(rawInput) as? YouTubeParseResult.Success)?.selection?.canonicalUrl

    /**
     * Builds the IFrame player variable set for a selection.
     *
     * The official player is always used with `playsinline` so playback stays inside the floating
     * rounded container instead of being pushed into fullscreen, `enablejsapi` for player-state
     * events and `controls=1` so YouTube's own controls and branding remain available.
     */
    fun playerVariables(selection: YouTubeSelection, origin: String): Map<String, String> {
        val vars = linkedMapOf(
            "playsinline" to "1",
            "controls" to "1",
            "enablejsapi" to "1",
            "rel" to "0",
            "modestbranding" to "0",
            "origin" to origin,
            "autohide" to "2"
        )
        // The video itself is passed to the player constructor / loadVideoById, not as a playerVar.
        selection.playlistId?.let { vars["list"] = it }
        if (selection.isPlaylistOnly) {
            vars["listType"] = "playlist"
        }
        return vars
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        rawQuery.split('&').forEach { pair ->
            if (pair.isBlank()) return@forEach
            val idx = pair.indexOf('=')
            val key = if (idx >= 0) pair.substring(0, idx) else pair
            val rawValue = if (idx >= 0) pair.substring(idx + 1) else ""
            val decoded = try {
                URLDecoder.decode(rawValue, "UTF-8")
            } catch (_: Exception) {
                rawValue
            }
            result[decodedKey(key)] = decoded
        }
        return result
    }

    private fun decodedKey(key: String): String = try {
        URLDecoder.decode(key, "UTF-8")
    } catch (_: Exception) {
        key
    }
}
