package com.synfusion.attendance.live.media

import com.synfusion.attendance.live.model.LiveRoomType

/** Fail closed until media-confirmed room activation is integrated. */
internal object LiveRoomCreationPolicy {
    fun allowsCreation(type: LiveRoomType): Boolean = when (type) {
        LiveRoomType.VIDEO, LiveRoomType.AUDIO -> false
        LiveRoomType.TEXT, LiveRoomType.YOUTUBE_WATCH_PARTY -> true
    }
}
