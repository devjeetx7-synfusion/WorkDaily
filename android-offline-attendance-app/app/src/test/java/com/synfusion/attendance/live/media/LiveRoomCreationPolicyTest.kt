package com.synfusion.attendance.live.media

import com.synfusion.attendance.live.model.LiveRoomType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRoomCreationPolicyTest {
    @Test fun allRoomTypesAreAllowedNowThatManagedStartupIsIntegrated() {
        assertTrue(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.VIDEO))
        assertTrue(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.AUDIO))
        assertTrue(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.TEXT))
        assertTrue(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.YOUTUBE_WATCH_PARTY))
    }
}
