package com.synfusion.attendance.live.watchparty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveShareLinkTest {

    @Test
    fun `share url round trips through the parser`() {
        val roomId = "room_ab12cd34ef56"
        val url = LiveShareLink.buildShareUrl(roomId)
        assertEquals("https://workdaily.app/live/$roomId", url)
        assertEquals(roomId, LiveShareLink.parseRoomId(url))
    }

    @Test
    fun `app scheme link resolves the same room`() {
        val appLink = LiveShareLink.buildAppLink("room_xyz999")
        assertEquals("workdaily://live/room_xyz999", appLink)
        assertEquals("room_xyz999", LiveShareLink.parseRoomId(appLink))
    }

    @Test
    fun `query strings and trailing slashes are tolerated`() {
        assertEquals(
            "room_abc123",
            LiveShareLink.parseRoomId("https://workdaily.app/live/room_abc123?ref=whatsapp")
        )
        assertEquals(
            "room_abc123",
            LiveShareLink.parseRoomId("https://www.workdaily.app/live/room_abc123/")
        )
    }

    @Test
    fun `unrelated urls are rejected`() {
        assertNull(LiveShareLink.parseRoomId("https://example.com/live/room_abc123"))
        assertNull(LiveShareLink.parseRoomId("https://workdaily.app/profile/room_abc123"))
        assertNull(LiveShareLink.parseRoomId("https://workdaily.app/live/"))
        assertNull(LiveShareLink.parseRoomId("not a link"))
        assertNull(LiveShareLink.parseRoomId(null))
    }

    @Test
    fun `invalid room ids are rejected`() {
        assertFalse(LiveShareLink.isValidRoomId("ab"))
        assertFalse(LiveShareLink.isValidRoomId("room with spaces"))
        assertTrue(LiveShareLink.isValidRoomId("room_1"))
    }

    @Test
    fun `share text contains the real link and host name`() {
        val text = LiveShareLink.buildShareText(
            roomId = "room_1",
            title = "Safety briefing",
            hostName = "Asha Kumar",
            isWatchParty = true
        )
        assertTrue(text.contains("Asha Kumar"))
        assertTrue(text.contains("Safety briefing"))
        assertTrue(text.contains("Watch Party"))
        assertTrue(text.contains("https://workdaily.app/live/room_1"))
    }

    @Test
    fun `share subject is human readable`() {
        assertEquals(
            "Join \"Safety briefing\" on WorkDaily",
            LiveShareLink.buildShareSubject("Safety briefing")
        )
    }

    @Test
    fun `share text does not leak private room fields`() {
        val text = LiveShareLink.buildShareText("room_1", "t", "h", false)
        assertFalse(text.contains("diamond", ignoreCase = true))
        assertFalse(text.contains("viewer", ignoreCase = true))
    }
}
