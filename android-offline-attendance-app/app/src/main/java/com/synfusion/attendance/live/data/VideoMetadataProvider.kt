package com.synfusion.attendance.live.data

/**
 * Public metadata of a YouTube video, resolved from an official, key-less YouTube endpoint.
 * Used only for playlist row titles/thumbnails; playback always stays inside the official player.
 */
data class VideoMetadata(
    val title: String,
    val thumbnailUrl: String? = null,
    val authorName: String? = null
)

/**
 * Resolves [VideoMetadata] for a video id. Implementations must never throw and must return null
 * when metadata is unavailable, so callers can fall back to a derived (non-invented) title.
 */
interface VideoMetadataProvider {
    suspend fun fetch(videoId: String): VideoMetadata?
}

/** Default provider: no network call, callers keep the derived title. */
object NoOpVideoMetadataProvider : VideoMetadataProvider {
    override suspend fun fetch(videoId: String): VideoMetadata? = null
}
