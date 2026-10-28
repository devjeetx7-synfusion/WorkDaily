package com.synfusion.attendance.live.ui

import com.synfusion.attendance.live.watchparty.YouTubeLinkParser

/**
 * Backwards compatible helpers for the rest of the Live UI. Parsing itself lives in
 * [YouTubeLinkParser], which validates the host and
 * rejects non-YouTube URLs instead of extracting IDs from arbitrary pages.
 */
fun extractYouTubeVideoId(input: String): String? =
    YouTubeLinkParser.parseVideoId(input)

fun extractYouTubePlaylistId(input: String): String? =
    YouTubeLinkParser.parsePlaylistId(input)

