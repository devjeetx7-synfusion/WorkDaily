package com.synfusion.attendance.live.ui

/**
 * Formats the elapsed live-stream duration for the room header.
 *
 * The value always comes from the room's persisted `startTimeMillis`, so an ended room shows its
 * real broadcast length and nothing is fabricated. Pure function so it can be unit tested on the JVM.
 */
fun formatLiveDuration(elapsedSeconds: Long): String {
    val total = elapsedSeconds.coerceAtLeast(0L)
    val hours = total / 3_600L
    val minutes = (total % 3_600L) / 60L
    val seconds = total % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** Elapsed seconds between the room start and [nowMillis], never negative. */
fun liveElapsedSeconds(startTimeMillis: Long, nowMillis: Long): Long {
    if (startTimeMillis <= 0L) return 0L
    return ((nowMillis - startTimeMillis) / 1_000L).coerceAtLeast(0L)
}
