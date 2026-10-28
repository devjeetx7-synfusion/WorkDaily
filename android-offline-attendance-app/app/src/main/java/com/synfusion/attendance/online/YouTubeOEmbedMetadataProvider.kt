package com.synfusion.attendance.online

import com.synfusion.attendance.live.data.VideoMetadata
import com.synfusion.attendance.live.data.VideoMetadataProvider
import com.synfusion.attendance.live.watchparty.YouTubeLinkParser
import com.synfusion.attendance.live.watchparty.YouTubeOEmbed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves a playlist row's real title/thumbnail from YouTube's public oEmbed endpoint.
 *
 * - No API key, no scraping, no media access: one HTTPS GET to `youtube.com/oembed`.
 * - Short timeouts and full failure tolerance: a blocked/offline call returns null and the caller
 *   keeps the derived `YouTube Video (<id>)` title (never a fabricated title).
 */
class YouTubeOEmbedMetadataProvider(
    private val timeoutMillis: Int = 4_000,
    private val fetcher: (String) -> String? = ::httpGet
) : VideoMetadataProvider {

    override suspend fun fetch(videoId: String): VideoMetadata? {
        if (!YouTubeLinkParser.isValidVideoId(videoId)) return null
        return withContext(Dispatchers.IO) {
            val body = runCatching { fetcher(YouTubeOEmbed.endpointUrl(videoId)) }.getOrNull()
            YouTubeOEmbed.parse(body)
        }
    }

    private companion object {
        fun httpGet(url: String): String? {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4_000
                readTimeout = 4_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                val stream: InputStream = connection.inputStream
                return stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val builder = StringBuilder()
                    var read = reader.read()
                    var guard = 0
                    while (read != -1 && guard < MAX_RESPONSE_CHARS) {
                        builder.append(read.toChar())
                        read = reader.read()
                        guard++
                    }
                    builder.toString()
                }
            } finally {
                runCatching { connection.disconnect() }
            }
        }

        const val MAX_RESPONSE_CHARS = 8_192
    }
}
