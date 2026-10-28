package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.data.VideoMetadata

/**
 * Parsing/validation for YouTube's public oEmbed endpoint
 * (`https://www.youtube.com/oembed?url=...&format=json`).
 *
 * The endpoint is public, key-less and read-only: it only returns the public title, author and
 * thumbnail of an *embeddable* video, which is exactly what the playlist rows need. Nothing about
 * the video stream is touched, and when a video is not embeddable the call simply fails — the caller
 * then keeps the derived placeholder title instead of inventing metadata.
 *
 * The parser is intentionally dependency free and defensive: a malformed body never throws.
 */
object YouTubeOEmbed {

    /** oEmbed requires the canonical watch URL of the video. */
    fun endpointUrl(videoId: String): String {
        val watchUrl = YouTubeLinkParser.watchUrl(videoId, null)
        return "https://www.youtube.com/oembed?url=" +
            java.net.URLEncoder.encode(watchUrl, "UTF-8") +
            "&format=json"
    }

    /**
     * Extracts `title` / `thumbnail_url` / `author_name` from an oEmbed response body.
     * Returns null when the body is not a JSON object or has no usable title.
     */
    fun parse(body: String?): VideoMetadata? {
        val json = body?.trim().orEmpty()
        if (!json.startsWith("{") || !json.endsWith("}")) return null
        val title = readString(json, "title")?.trim().orEmpty()
        if (title.isBlank()) return null
        return VideoMetadata(
            title = sanitizeTitle(title),
            thumbnailUrl = readString(json, "thumbnail_url")?.takeIf { it.startsWith("https://") },
            authorName = readString(json, "author_name")?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    /** Titles are rendered in a single line row; collapse whitespace and bound the length. */
    fun sanitizeTitle(raw: String): String {
        val collapsed = raw.replace(Regex("\\s+"), " ").trim()
        return if (collapsed.length <= MAX_TITLE_LENGTH) collapsed else collapsed.take(MAX_TITLE_LENGTH - 1) + "…"
    }

    const val MAX_TITLE_LENGTH = 120

    private fun readString(json: String, key: String): String? {
        val marker = "\"$key\""
        var index = json.indexOf(marker)
        while (index >= 0) {
            var cursor = index + marker.length
            while (cursor < json.length && json[cursor].isWhitespace()) cursor++
            if (cursor < json.length && json[cursor] == ':') {
                cursor++
                while (cursor < json.length && json[cursor].isWhitespace()) cursor++
                if (cursor < json.length && json[cursor] == '"') {
                    return readJsonString(json, cursor + 1)
                }
            }
            index = json.indexOf(marker, index + marker.length)
        }
        return null
    }

    private fun readJsonString(json: String, start: Int): String? {
        val builder = StringBuilder()
        var cursor = start
        while (cursor < json.length) {
            when (val char = json[cursor]) {
                '"' -> return builder.toString()
                '\\' -> {
                    if (cursor + 1 >= json.length) return null
                    when (val escaped = json[cursor + 1]) {
                        'n' -> builder.append('\n')
                        't' -> builder.append('\t')
                        'r' -> builder.append('\r')
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000C')
                        'u' -> {
                            if (cursor + 5 >= json.length) return null
                            val hex = json.substring(cursor + 2, cursor + 6)
                            val code = hex.toIntOrNull(16) ?: return null
                            builder.append(code.toChar())
                            cursor += 4
                        }

                        else -> builder.append(escaped)
                    }
                    cursor += 2
                }

                else -> {
                    builder.append(char)
                    cursor++
                }
            }
        }
        return null
    }
}
