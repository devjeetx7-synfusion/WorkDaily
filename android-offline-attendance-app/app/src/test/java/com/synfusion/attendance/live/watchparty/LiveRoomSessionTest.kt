package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.data.InMemoryLiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
import com.synfusion.attendance.live.data.LiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomSession
import com.synfusion.attendance.live.data.VideoMetadata
import com.synfusion.attendance.live.data.VideoMetadataProvider
import com.synfusion.attendance.live.model.ChatMessage
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LiveRoomSessionState
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

/**
 * Session-level behavior: membership, presence, chat persistence and host-only playback.
 *
 * Uses the real coroutine dispatchers with short heartbeat intervals and bounded polling so the
 * test exercises the same listener/heartbeat code paths the app runs (no virtual-time shortcuts).
 * Runs against [InMemoryLiveRoomBackend], which enforces the identical authorization contract as
 * the Firestore backend.
 */
class LiveRoomSessionTest {

    private val host = LiveUserIdentity(
        uid = "uid_host",
        displayName = "Asha Kumar",
        username = "asha",
        profession = "Site Engineer"
    )
    private val viewer = LiveUserIdentity(
        uid = "uid_viewer",
        displayName = "Ravi Singh",
        username = "ravi",
        profession = "Site Worker"
    )

    private val clock = AtomicLong(1_000_000L)
    private val backend = InMemoryLiveRoomBackend(clock = { clock.get() })
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private suspend fun newSession(
        roomId: String,
        identity: LiveUserIdentity?,
        metadataProvider: VideoMetadataProvider? = null
    ): LiveRoomSession {
        val session = LiveRoomSession(
            roomId = roomId,
            backend = backend,
            identity = identity,
            scope = sessionScope,
            cleanupScope = cleanupScope,
            clock = { clock.get() },
            metadataProvider = metadataProvider ?: com.synfusion.attendance.live.data.NoOpVideoMetadataProvider,
            heartbeatIntervalMillis = 50L,
            presenceTickMillis = 40L
        )
        session.start()
        return session
    }

    private suspend fun awaitUntil(timeoutMillis: Long = 5_000L, condition: () -> Boolean) {
        withTimeout(timeoutMillis) {
            while (!condition()) delay(10)
        }
    }

    private suspend fun createWatchPartyRoom(): String {
        val result = backend.createRoom(
            host,
            LiveRoomCreateRequest(
                title = "Watch party",
                category = "Site Safety",
                type = LiveRoomType.YOUTUBE_WATCH_PARTY,
                selection = (YouTubeLinkParser.parse("https://youtu.be/dQw4w9WgXcQ") as YouTubeParseResult.Success).selection
            )
        ) as LiveOperationResult.Success
        return result.data.id
    }

    private suspend fun createTextRoom(): String {
        val result = backend.createRoom(
            host,
            LiveRoomCreateRequest(title = "Chat room", category = "Site Safety", type = LiveRoomType.TEXT)
        ) as LiveOperationResult.Success
        return result.data.id
    }

    private fun active(session: LiveRoomSession): LiveRoomSessionState.Active =
        session.state.value as LiveRoomSessionState.Active

    @Test
    fun `host session joins publishes presence and controls playback`() = runBlocking {
        val roomId = createWatchPartyRoom()
        val session = newSession(roomId, host)
        awaitUntil { session.state.value is LiveRoomSessionState.Active }

        assertTrue("session host flag", active(session).isHost)
        assertEquals(host.uid, active(session).room.hostUserId)
        awaitUntil { session.members.value.any { it.uid == host.uid && it.isHost } }
        assertTrue("host membership present", session.members.value.any { it.uid == host.uid && it.isHost })

        session.hostPlay(12f)
        awaitUntil { session.watchParty.value?.status == WatchPartyPlaybackStatus.PLAYING }
        assertEquals(host.uid, session.watchParty.value?.updatedByUserId)

        session.hostTurnOff()
        withTimeout(5_000) {
            while (session.watchParty.value?.isTurnedOff != true) delay(10)
            Unit
        }
        // Turning playback off must not end the room.
        assertTrue(
            "room must stay active after turning playback off, was ${session.state.value}",
            session.state.value is LiveRoomSessionState.Active
        )
        assertTrue("room must stay live", active(session).room.isLive)

        session.stop()
    }

    @Test
    fun `participant mirrors host playback and cannot publish`() = runBlocking {
        val roomId = createWatchPartyRoom()
        val hostSession = newSession(roomId, host)
        awaitUntil { hostSession.state.value is LiveRoomSessionState.Active }
        val viewerSession = newSession(roomId, viewer)
        awaitUntil { viewerSession.state.value is LiveRoomSessionState.Active }
        awaitUntil { active(viewerSession).viewerCount == 1 && viewerSession.members.value.size == 2 }

        hostSession.hostPlay(30f)
        awaitUntil { viewerSession.watchParty.value?.status == WatchPartyPlaybackStatus.PLAYING }

        val revisionBefore = viewerSession.watchParty.value?.revision
        viewerSession.hostPlay(99f)
        delay(200)
        assertEquals(revisionBefore, viewerSession.watchParty.value?.revision)
        assertNotNull(viewerSession.notice.value)

        hostSession.stop()
        viewerSession.stop()
    }

    @Test
    fun `chat messages persist and appear for other members`() = runBlocking {
        val roomId = createTextRoom()
        val hostSession = newSession(roomId, host)
        awaitUntil { hostSession.state.value is LiveRoomSessionState.Active }
        val viewerSession = newSession(roomId, viewer)
        awaitUntil { viewerSession.state.value is LiveRoomSessionState.Active }

        viewerSession.sendMessage("Namaste from the site")
        awaitUntil { hostSession.messages.value.any { it.message == "Namaste from the site" } }

        val received = hostSession.messages.value.first { it.message == "Namaste from the site" }
        assertEquals(viewer.uid, received.senderId)
        assertEquals("Ravi Singh", received.senderName)
        assertTrue(viewerSession.messages.value.none { it.isPending })

        hostSession.stop()
        viewerSession.stop()
    }

    @Test
    fun `unauthenticated session reads the room but cannot join or chat`() = runBlocking {
        val roomId = createTextRoom()
        val session = newSession(roomId, null)
        awaitUntil { session.state.value is LiveRoomSessionState.Active }

        assertFalse(session.isMember.value)
        session.sendMessage("hello")
        delay(200)
        assertTrue(session.messages.value.none { it.message == "hello" })
        assertEquals(1, backend.observeMembers(roomId).first().size)

        session.stop()
    }

    @Test
    fun `ending the room moves the session to an ended state`() = runBlocking {
        val roomId = createTextRoom()
        val session = newSession(roomId, host)
        awaitUntil { session.state.value is LiveRoomSessionState.Active }

        session.endRoom()
        awaitUntil { session.state.value is LiveRoomSessionState.Ended }

        session.stop()
    }

    @Test
    fun `stale live snapshot cannot revive an ended session`() = runBlocking {
        val roomId = createTextRoom()
        val live = (backend.loadRoom(roomId) as LiveOperationResult.Success).data!!
        val snapshots = MutableStateFlow<com.synfusion.attendance.live.model.LiveRoom?>(live)
        val snapshotBackend = object : LiveRoomBackend by backend {
            override fun observeRoom(roomId: String) = snapshots
        }
        val session = LiveRoomSession(roomId, snapshotBackend, host, sessionScope, cleanupScope)
        session.start()
        awaitUntil { session.state.value is LiveRoomSessionState.Active }
        snapshots.value = live.copy(isLive = false, endedAtMillis = clock.get())
        awaitUntil { session.state.value is LiveRoomSessionState.Ended }
        snapshots.value = live
        delay(150)
        assertTrue(session.state.value is LiveRoomSessionState.Ended)
        session.stop()
    }

    @Test
    fun `duplicate end requests do not call the backend twice`() = runBlocking {
        val roomId = createTextRoom()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val slowBackend = object : LiveRoomBackend by backend {
            override suspend fun endRoom(roomId: String, hostUid: String): LiveOperationResult<Unit> {
                calls++
                entered.complete(Unit)
                release.await()
                return backend.endRoom(roomId, hostUid)
            }
        }
        val session = LiveRoomSession(roomId, slowBackend, host, sessionScope, cleanupScope)
        session.start()
        awaitUntil { session.state.value is LiveRoomSessionState.Active }
        val first = async { session.endRoom() }
        entered.await()
        assertTrue(session.endRoom() is LiveOperationResult.Error)
        assertEquals(1, calls)
        release.complete(Unit)
        assertTrue(first.await() is LiveOperationResult.Success)
        session.stop()
    }

    @Test
    fun `stopping the session removes the presence document`() = runBlocking {
        val roomId = createTextRoom()
        val session = newSession(roomId, viewer)
        awaitUntil { session.state.value is LiveRoomSessionState.Active }
        assertEquals(2, backend.observeMembers(roomId).first().size)

        session.stop()
        withTimeout(5_000) {
            while (backend.observeMembers(roomId).first().any { it.uid == viewer.uid }) delay(10)
        }
        assertTrue(backend.observeMembers(roomId).first().none { it.uid == viewer.uid })
    }

    @Test
    fun `presence heartbeat keeps an idle member counted`() = runBlocking {
        val roomId = createTextRoom()
        val session = newSession(roomId, viewer)
        awaitUntil { session.state.value is LiveRoomSessionState.Active }
        awaitUntil { active(session).viewerCount == 1 }

        repeat(3) {
            clock.addAndGet(30_000L)
            delay(120)
            awaitUntil { active(session).viewerCount == 1 }
        }
        assertEquals(1, active(session).viewerCount)

        session.stop()
    }

    @Test
    fun `playlist add is host only and persisted to the queue`() = runBlocking {
        val roomId = createWatchPartyRoom()
        val viewerSession = newSession(roomId, viewer)
        awaitUntil { viewerSession.state.value is LiveRoomSessionState.Active }

        viewerSession.addPlaylistVideo("https://youtu.be/abcdefghijk")
        delay(200)
        assertTrue(viewerSession.playlist.value.none { it.videoId == "abcdefghijk" })

        val hostSession = newSession(roomId, host)
        awaitUntil { hostSession.state.value is LiveRoomSessionState.Active }
        hostSession.addPlaylistVideo("https://youtu.be/abcdefghijk")
        awaitUntil { hostSession.playlist.value.any { it.videoId == "abcdefghijk" } }

        // Duplicate add is rejected without creating a second row.
        hostSession.addPlaylistVideo("https://youtu.be/abcdefghijk")
        delay(200)
        assertEquals(1, hostSession.playlist.value.count { it.videoId == "abcdefghijk" })

        hostSession.stop()
        viewerSession.stop()
    }

    @Test
    fun `playlist rows use real metadata titles when available`() = runBlocking {
        val roomId = createWatchPartyRoom()
        val metadataProvider = object : VideoMetadataProvider {
            override suspend fun fetch(videoId: String): VideoMetadata =
                VideoMetadata(
                    title = "Working at Height — Site Safety",
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                )
        }
        val hostSession = newSession(roomId, host, metadataProvider)
        awaitUntil { hostSession.state.value is LiveRoomSessionState.Active }

        hostSession.addPlaylistVideo("https://youtu.be/abcdefghijk")
        awaitUntil { hostSession.playlist.value.any { it.videoId == "abcdefghijk" } }
        val item = hostSession.playlist.value.first { it.videoId == "abcdefghijk" }
        assertEquals("Working at Height — Site Safety", item.title)
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg", item.thumbnailUrl)

        hostSession.stop()
    }

    @Test
    fun `playlist falls back to the derived title when metadata is unavailable`() = runBlocking {
        val roomId = createWatchPartyRoom()
        val failingProvider = object : VideoMetadataProvider {
            override suspend fun fetch(videoId: String): VideoMetadata? = null
        }
        val hostSession = newSession(roomId, host, failingProvider)
        awaitUntil { hostSession.state.value is LiveRoomSessionState.Active }

        hostSession.addPlaylistVideo("https://youtu.be/abcdefghijk")
        awaitUntil { hostSession.playlist.value.any { it.videoId == "abcdefghijk" } }
        val item = hostSession.playlist.value.first { it.videoId == "abcdefghijk" }
        assertEquals("YouTube Video (abcdefghijk)", item.title)
        assertTrue(item.thumbnailUrl.contains("i.ytimg.com"))

        hostSession.stop()
    }

    @Test
    fun `chat merge removes optimistic duplicates`() {
        val pending = ChatMessage(
            id = "c_1",
            roomId = "r",
            senderId = "u",
            senderName = "A",
            message = "hi",
            timestamp = 10L,
            clientMessageId = "c_1",
            isPending = true
        )
        val persisted = pending.copy(isPending = false)
        val merged = LiveChatPolicy.mergeMessages(listOf(persisted), listOf(pending))
        assertEquals(1, merged.size)
        assertFalse(merged.first().isPending)
    }

    @Test
    fun `user whose join operation has not completed cannot send message`() = runBlocking {
        val roomId = createTextRoom()
        val session = LiveRoomSession(
            roomId = roomId,
            backend = backend,
            identity = viewer,
            scope = sessionScope,
            cleanupScope = cleanupScope,
            clock = { clock.get() }
        )
        // Session created but start() NOT called yet -> membership not completed
        assertFalse(session.isMember.value)

        session.sendMessage("Message before join completed")
        delay(100)
        assertTrue("Message must not be persisted before join completion", session.messages.value.none { it.message == "Message before join completed" })
    }

    @Test
    fun `unauthorized user cannot end another hosts room`() = runBlocking {
        val roomId = createTextRoom()
        val viewerSession = newSession(roomId, viewer)
        awaitUntil { viewerSession.state.value is LiveRoomSessionState.Active }

        val endResult = viewerSession.endRoom()
        assertTrue(endResult is LiveOperationResult.Error)
        assertTrue((endResult as LiveOperationResult.Error).isAuthError)
        assertEquals("Only the host can end this live room.", endResult.message)

        val roomData = (backend.loadRoom(roomId) as LiveOperationResult.Success).data
        assertNotNull(roomData)
        assertTrue("Room must remain live when unauthorized user tries to end it", roomData!!.isLive)

        viewerSession.stop()
    }

    @Test
    fun `system events are created and deduplicated`() = runBlocking {
        val roomId = createTextRoom()
        val hostSession = newSession(roomId, host)
        awaitUntil { hostSession.state.value is LiveRoomSessionState.Active }

        val sysMsg = ChatMessage(
            id = "sys_test_1",
            roomId = roomId,
            senderId = host.uid,
            senderName = "System",
            message = "System Announcement",
            timestamp = clock.get(),
            clientMessageId = "sys_test_1",
            isSystemMessage = true,
            eventType = "ANNOUNCEMENT"
        )

        hostSession.sendSystemEvent(sysMsg)
        hostSession.sendSystemEvent(sysMsg) // duplicate send
        delay(150)

        val messages = backend.observeMessages(roomId, 50).first()
        assertEquals(1, messages.count { it.clientMessageId == "sys_test_1" })

        hostSession.stop()
    }
}
