package com.synfusion.attendance.live

import com.synfusion.attendance.live.data.InMemoryLiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
import com.synfusion.attendance.live.model.ChatMessage
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.watchparty.YouTubeSelection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkDailyLiveFixesTest {

    private val backend = InMemoryLiveRoomBackend()

    private val testHost = LiveUserIdentity(
        uid = "host_101",
        displayName = "Rajesh Sharma",
        username = "rajesh_sharma",
        photoUrl = "https://workdaily.app/photos/rajesh.jpg",
        profession = "Site Engineer",
        isVerified = true
    )

    private val testUser = LiveUserIdentity(
        uid = "user_202",
        displayName = "Priya Verma",
        username = "priya_verma",
        photoUrl = "https://workdaily.app/photos/priya.jpg",
        profession = "Civil Contractor",
        isVerified = false
    )

    @Test
    fun `test create room writes initial LIVE_STARTED system message`() = runTest {
        val request = LiveRoomCreateRequest(
            title = "Site Safety Briefing",
            category = "Safety",
            type = LiveRoomType.VIDEO
        )
        val result = backend.createRoom(testHost, request)
        assertTrue(result is LiveOperationResult.Success)
        val room = (result as LiveOperationResult.Success).data

        val messages = backend.observeMessages(room.id, 50).first()
        val startMsg = messages.firstOrNull { it.eventType == "LIVE_STARTED" }
        assertNotNull("LIVE_STARTED system event should exist", startMsg)
        assertEquals("host_101", startMsg?.senderId)
        assertEquals("Rajesh Sharma", startMsg?.senderName)
        assertTrue(startMsg?.isSystemMessage == true)
    }

    @Test
    fun `test join room writes idempotent USER_JOINED system message`() = runTest {
        val request = LiveRoomCreateRequest(
            title = "Site Safety Briefing",
            category = "Safety",
            type = LiveRoomType.VIDEO
        )
        val room = (backend.createRoom(testHost, request) as LiveOperationResult.Success).data

        // Join room once
        val joinResult1 = backend.joinRoom(room.id, testUser)
        assertTrue(joinResult1 is LiveOperationResult.Success)

        // Join room second time (reconnect)
        val joinResult2 = backend.joinRoom(room.id, testUser)
        assertTrue(joinResult2 is LiveOperationResult.Success)

        val messages = backend.observeMessages(room.id, 50).first()
        val joinMsgs = messages.filter { it.eventType == "USER_JOINED" && it.senderId == testUser.uid }
        assertEquals("User join event should be idempotent and present exactly once", 1, joinMsgs.size)
        assertEquals("Priya Verma joined the live", joinMsgs.first().message)
    }

    @Test
    fun `test end room persists statusText ENDED and endedAtMillis and rejects subsequent chat`() = runTest {
        val request = LiveRoomCreateRequest(
            title = "Watch Party",
            category = "Watch Party",
            type = LiveRoomType.YOUTUBE_WATCH_PARTY,
            selection = YouTubeSelection(
                videoId = "abc12345678",
                playlistId = null,
                source = com.synfusion.attendance.live.watchparty.YouTubeSource.VIDEO,
                canonicalUrl = "https://youtube.com/watch?v=abc12345678"
            )
        )
        val room = (backend.createRoom(testHost, request) as LiveOperationResult.Success).data

        val endResult = backend.endRoom(room.id, testHost.uid)
        assertTrue(endResult is LiveOperationResult.Success)

        val loadedRoom = (backend.loadRoom(room.id) as LiveOperationResult.Success).data
        assertNotNull(loadedRoom)
        assertFalse(loadedRoom!!.isLive)
        assertEquals("ENDED", loadedRoom.statusText)
        assertNotNull(loadedRoom.endedAtMillis)

        // Verify active rooms stream excludes ended room
        val activeRooms = backend.observeActiveRooms().first()
        assertFalse("Ended room must disappear from active rooms stream", activeRooms.any { it.id == room.id })

        // Attempting to send chat to ended room must fail
        val chatResult = backend.sendMessage(room.id, testUser, "Hello", "client_1")
        assertTrue(chatResult is LiveOperationResult.Error)
    }

    @Test
    fun `test non-host cannot end room`() = runTest {
        val request = LiveRoomCreateRequest(
            title = "Host Exclusive Room",
            category = "Engineering",
            type = LiveRoomType.VIDEO
        )
        val room = (backend.createRoom(testHost, request) as LiveOperationResult.Success).data

        val unauthorizedEnd = backend.endRoom(room.id, testUser.uid)
        assertTrue(unauthorizedEnd is LiveOperationResult.Error)
        val error = unauthorizedEnd as LiveOperationResult.Error
        assertTrue(error.isAuthError)

        val loadedRoom = (backend.loadRoom(room.id) as LiveOperationResult.Success).data
        assertTrue("Room remains active when non-host attempts to end it", loadedRoom!!.isLive)
    }

    @Test
    fun `test system follow event idempotency`() = runTest {
        val request = LiveRoomCreateRequest(
            title = "Follow Test Stream",
            category = "Safety",
            type = LiveRoomType.VIDEO
        )
        val room = (backend.createRoom(testHost, request) as LiveOperationResult.Success).data

        val clientMsgId = "sys_follow_${room.id}_${testUser.uid}_${testHost.uid}"
        val followMsg = ChatMessage(
            id = clientMsgId,
            roomId = room.id,
            senderId = testUser.uid,
            senderName = testUser.displayName,
            senderAvatar = testUser.photoUrl,
            senderUsername = testUser.username,
            message = "${testUser.displayName} started following ${testHost.displayName}",
            timestamp = System.currentTimeMillis(),
            eventType = "USER_FOLLOWED",
            clientMessageId = clientMsgId
        )

        val sendResult1 = backend.sendSystemEvent(room.id, followMsg)
        assertTrue(sendResult1 is LiveOperationResult.Success)

        // Duplicate send (e.g. retry / reconnect)
        val sendResult2 = backend.sendSystemEvent(room.id, followMsg)
        assertTrue(sendResult2 is LiveOperationResult.Success)

        val messages = backend.observeMessages(room.id, 50).first()
        val followMsgs = messages.filter { it.clientMessageId == clientMsgId }
        assertEquals("Follow event must be idempotent and appear once", 1, followMsgs.size)
    }
}
