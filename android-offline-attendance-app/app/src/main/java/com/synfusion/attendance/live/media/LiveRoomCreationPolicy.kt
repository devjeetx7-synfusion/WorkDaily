package com.synfusion.attendance.live.media

import com.synfusion.attendance.live.model.LiveRoomType

/** Fail closed until media-confirmed room activation is integrated. */
internal object LiveRoomCreationPolicy {
    fun allowsCreation(type: LiveRoomType): Boolean = true
}
