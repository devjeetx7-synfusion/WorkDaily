package com.synfusion.attendance.live.media

import com.synfusion.attendance.live.model.LiveRoomType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRoomCreationPolicyTest {
    @Test fun mediaRoomsAreNotAdvertisedWithoutPublishing() {
        assertFalse(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.VIDEO))
        assertFalse(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.AUDIO))
    }

    @Test fun existingNonMediaModesRemainAvailable() {
        assertTrue(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.TEXT))
        assertTrue(LiveRoomCreationPolicy.allowsCreation(LiveRoomType.YOUTUBE_WATCH_PARTY))
    }
}
