package com.synfusion.attendance.live.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LiveKitTokenClientTest {
    private fun response(url: String = "wss://live.example.com", token: String = "test-token", room: String = "workdaily-abc") =
        mapOf("url" to url, "token" to token, "roomName" to room)

    @Test fun acceptsCallableResponseForRequestedRoom() {
        val credentials = parseLiveKitCredentials(response(), "abc")
        assertEquals("wss://live.example.com", credentials.url)
        assertEquals("test-token", credentials.token)
        assertEquals("workdaily-abc", credentials.roomName)
    }

    @Test fun rejectsWrongRoomAndInvalidResponses() {
        assertThrows(IllegalArgumentException::class.java) { parseLiveKitCredentials(response(room = "workdaily-other"), "abc") }
        assertThrows(IllegalArgumentException::class.java) { parseLiveKitCredentials(response(url = "ws://live.example.com"), "abc") }
        assertThrows(IllegalArgumentException::class.java) { parseLiveKitCredentials(response(url = "wss://user:secret@live.example.com"), "abc") }
        assertThrows(IllegalArgumentException::class.java) { parseLiveKitCredentials(response(token = ""), "abc") }
        assertThrows(IllegalStateException::class.java) { parseLiveKitCredentials(null, "abc") }
    }
}
