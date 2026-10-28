package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.data.InMemoryLiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end authorization/persistence behavior of the Live room backend contract.
 *
 * The production implementation is Firestore-backed; this suite runs the identical contract against
 * [InMemoryLiveRoomBackend] so room creation, presence, playlist revisions and host-only writes are
 * verified on the JVM without an emulator.
 */
class LiveRoomBackendTest {

    private val host = LiveUserIdentity(
        uid = "firebase_host",
        displayName = "Asha Kumar",
        username = "asha",
        photoUrl = "https://cdn.example/asha.jpg",
        profession = "Site Engineer"
    )
    private val viewer = LiveUserIdentity(
        uid = "firebase_viewer",
        displayName = "Ravi Singh",
        username = "ravi",
        profession = "Site Worker"
    )

    private var now = 1_000_000L
    private fun backend() = InMemoryLiveRoomBackend(clock = { now })

    private suspend fun createWatchParty(
        backend: InMemoryLiveRoomBackend,
        videoId: String = "dQw4w9WgXcQ"
    ) = backend.createRoom(
        host,
        LiveRoomCreateRequest(
            title = "Safety briefing watch party",
            category = "Site Safety",
            type = LiveRoomType.YOUTUBE_WATCH_PARTY,
            selection = (YouTubeLinkParser.parse("https://youtu.be/$videoId") as YouTubeParseResult.Success).selection
        )
    )

    @Test
    fun `room is created with the authenticated host id and profile`() = runBlocking {
        val backend = backend()
        val result = createWatchParty(backend) as LiveOperationResult.Success
        val room = result.data

        assertEquals(host.uid, room.hostUserId)
        assertEquals(host.uid, room.creatorId)
        assertEquals(host.displayName, room.creatorName)
        assertEquals(host.photoUrl, room.creatorAvatar)
        assertEquals("asha", room.hostUsername)
        assertEquals("dQw4w9WgXcQ", room.youtubeVideoId)
        assertEquals(WatchPartyPlaybackStatus.UNSTARTED, room.watchPartyState?.status)
        assertEquals(host.uid, room.watchPartyState?.hostUserId)
        assertNotEquals("creator_me", room.hostUserId)

        // Persisted: reloading returns the same document.
        val reloaded = backend.loadRoom(room.id) as LiveOperationResult.Success
        assertEquals(room.id, reloaded.data?.id)
        assertEquals(host.uid, reloaded.data?.hostUserId)
    }

    @Test
    fun `creation is refused without an authenticated identity`() = runBlocking {
        val backend = backend()
        val result = backend.createRoom(
            host.copy(uid = ""),
            LiveRoomCreateRequest(title = "x", category = "y", type = LiveRoomType.VIDEO)
        )
        assertTrue((result as LiveOperationResult.Error).isAuthError)
    }

    @Test
    fun `duplicate active room creation for the same host is rejected`() = runBlocking {
        val backend = backend()
        createWatchParty(backend)
        val second = createWatchParty(backend)
        val error = second as LiveOperationResult.Error
        assertTrue(error.isConflict)
    }

    @Test
    fun `host membership is recorded and viewer count excludes the host`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data

        val members = backend.observeMembers(room.id).first()
        assertEquals(1, members.size)
        assertTrue(members.first().isHost)
        assertEquals(0, PresencePolicy.countActiveViewers(members, now))

        backend.joinRoom(room.id, viewer)
        val joined = backend.observeMembers(room.id).first()
        assertEquals(2, joined.size)
        assertEquals(1, PresencePolicy.countActiveViewers(joined, now))
        assertEquals(2, PresencePolicy.activeParticipantCount(joined, now))
    }

    @Test
    fun `rejoining does not duplicate membership rows`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data

        repeat(5) { backend.joinRoom(room.id, viewer) }
        val members = backend.observeMembers(room.id).first()
        assertEquals(2, members.size)
        assertEquals(1, members.count { it.uid == viewer.uid })
    }

    @Test
    fun `stale heartbeats drop out of the viewer count`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        now += PresencePolicy.ACTIVE_WINDOW_MILLIS + 1_000L
        val members = backend.observeMembers(room.id).first()
        assertEquals(0, PresencePolicy.countActiveViewers(members, now))
        assertEquals(listOf(viewer.uid), PresencePolicy.staleMemberUids(members, now))
    }

    @Test
    fun `leaving removes the membership`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)
        backend.leaveRoom(room.id, viewer.uid)

        val members = backend.observeMembers(room.id).first()
        assertEquals(1, members.size)
        assertTrue(members.none { it.uid == viewer.uid })
    }

    @Test
    fun `only the host can change playback`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)
        val revision = room.watchPartyState!!.revision

        val viewerAttempt = backend.applyPlaybackCommand(
            roomId = room.id,
            hostUid = viewer.uid, // participant tries to act as host
            expectedRevision = revision,
            command = LivePlaybackCommand.Play(0f)
        )
        assertTrue((viewerAttempt as LiveOperationResult.Error).isAuthError)

        val hostAttempt = backend.applyPlaybackCommand(
            roomId = room.id,
            hostUid = host.uid,
            expectedRevision = revision,
            command = LivePlaybackCommand.Play(0f)
        )
        assertTrue(hostAttempt is LiveOperationResult.Success)
        assertEquals(WatchPartyPlaybackStatus.PLAYING, (hostAttempt as LiveOperationResult.Success).data.status)
    }

    @Test
    fun `stale expected revision is rejected as a conflict`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        val revision = room.watchPartyState!!.revision

        val first = backend.applyPlaybackCommand(room.id, host.uid, revision, LivePlaybackCommand.Play(0f))
        assertTrue(first is LiveOperationResult.Success)

        val stale = backend.applyPlaybackCommand(room.id, host.uid, revision, LivePlaybackCommand.Pause(10f))
        assertTrue((stale as LiveOperationResult.Error).isConflict)
    }

    @Test
    fun `host playback changes propagate to observers in revision order`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        val rev1 = (backend.loadRoom(room.id) as LiveOperationResult.Success).data!!.watchPartyState!!.revision
        val play = backend.applyPlaybackCommand(room.id, host.uid, rev1, LivePlaybackCommand.Play(5f)) as LiveOperationResult.Success
        val rev2 = play.data.revision

        val seek = backend.applyPlaybackCommand(
            room.id, host.uid, rev2, LivePlaybackCommand.Seek(120f, resumePlaying = true)
        ) as LiveOperationResult.Success

        // A late joiner reads the latest persisted state.
        val latest = backend.observeWatchParty(room.id).first()!!
        assertEquals(seek.data.revision, latest.revision)
        assertEquals(120f, latest.positionSeconds, 0.001f)
        assertEquals(WatchPartyPlaybackStatus.PLAYING, latest.status)
        assertEquals(host.uid, latest.updatedByUserId)
        assertTrue(latest.commandId.isNotBlank())
    }

    @Test
    fun `playlist add select remove is host only and persistent`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        val secondItem = WatchPartyPlaylistItem(
            id = "abcdefghijk",
            videoId = "abcdefghijk",
            videoUrl = "https://youtu.be/abcdefghijk",
            title = "Second video",
            thumbnailUrl = YouTubeLinkParser.thumbnailUrl("abcdefghijk"),
            addedByUserId = host.uid,
            addedByUserName = host.displayName,
            addedAtMillis = now
        )

        val unauthorized = backend.addPlaylistItem(room.id, viewer.uid, secondItem)
        assertTrue((unauthorized as LiveOperationResult.Error).isAuthError)

        backend.addPlaylistItem(room.id, host.uid, secondItem)
        assertEquals(2, backend.observePlaylist(room.id).first().size)

        val duplicate = backend.addPlaylistItem(room.id, host.uid, secondItem)
        assertTrue((duplicate as LiveOperationResult.Error).isConflict)

        val stateBefore = backend.observeWatchParty(room.id).first()!!
        val selectResult = backend.applyPlaybackCommand(
            room.id,
            host.uid,
            stateBefore.revision,
            LivePlaybackCommand.SelectItem(secondItem.id, secondItem.videoId, secondItem.videoUrl)
        )
        val selected = (selectResult as LiveOperationResult.Success).data
        assertEquals("abcdefghijk", selected.videoId)
        assertEquals(secondItem.id, selected.activePlaylistItemId)

        backend.removePlaylistItem(room.id, host.uid, secondItem.id)
        val remaining = backend.observePlaylist(room.id).first()
        assertEquals(1, remaining.size)
        val stateAfter = backend.observeWatchParty(room.id).first()!!
        assertEquals("dQw4w9WgXcQ", stateAfter.videoId)
        assertEquals(WatchPartyPlaybackStatus.PAUSED, stateAfter.status)
    }

    @Test
    fun `ended room rejects playback and new members`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data

        val unauthorizedEnd = backend.endRoom(room.id, viewer.uid)
        assertTrue((unauthorizedEnd as LiveOperationResult.Error).isAuthError)

        backend.endRoom(room.id, host.uid)
        val endedRoom = (backend.loadRoom(room.id) as LiveOperationResult.Success).data!!
        assertFalse(endedRoom.isLive)
        assertNotNull(endedRoom.endedAtMillis)

        val revision = backend.observeWatchParty(room.id).first()!!.revision
        val playback = backend.applyPlaybackCommand(room.id, host.uid, revision, LivePlaybackCommand.Play(0f))
        assertTrue(playback is LiveOperationResult.Error)

        val join = backend.joinRoom(room.id, viewer)
        assertTrue(join is LiveOperationResult.Error)
    }

    @Test
    fun `chat requires membership and stamps the real sender`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data

        val notMember = backend.sendMessage(room.id, viewer, "hello", "c_1")
        assertTrue((notMember as LiveOperationResult.Error).isAuthError)

        backend.joinRoom(room.id, viewer)
        val sent = backend.sendMessage(room.id, viewer, "  Namaste  ", "c_2")
        val message = (sent as LiveOperationResult.Success).data
        assertEquals(viewer.uid, message.senderId)
        assertEquals("Ravi Singh", message.senderName)
        assertEquals("Namaste", message.message)
        assertEquals("c_2", message.clientMessageId)

        val persisted = backend.observeMessages(room.id, 50).first()
        assertEquals(1, persisted.size)
        assertEquals(viewer.uid, persisted.first().senderId)
    }

    @Test
    fun `duplicate chat sends with the same client id are idempotent`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        backend.sendMessage(room.id, viewer, "first", "c_dup")
        backend.sendMessage(room.id, viewer, "first", "c_dup")
        assertEquals(1, backend.observeMessages(room.id, 50).first().size)
    }

    @Test
    fun `chat cannot spoof another sender name`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        // Identity in the payload is ignored: the membership record wins.
        backend.sendMessage(room.id, viewer.copy(displayName = "Asha Kumar"), "hi", "c_spoof")
        val message = backend.observeMessages(room.id, 50).first().first()
        assertEquals(viewer.uid, message.senderId)
        assertEquals("Ravi Singh", message.senderName)
    }

    @Test
    fun `empty and oversized messages are rejected`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        assertTrue(backend.sendMessage(room.id, viewer, "   ", "c_empty") is LiveOperationResult.Error)
        val tooLong = "x".repeat(LiveChatPolicy.MAX_MESSAGE_LENGTH + 1)
        assertTrue(backend.sendMessage(room.id, viewer, tooLong, "c_long") is LiveOperationResult.Error)
    }

    @Test
    fun `chat history is bounded and paginates`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        repeat(12) { index ->
            now += 1_000L
            backend.sendMessage(room.id, viewer, "msg $index", "c_$index")
        }
        val limited = backend.observeMessages(room.id, 5).first()
        assertEquals(5, limited.size)
        assertEquals("msg 11", limited.last().message)

        val cursor = LiveChatPolicy.olderHistoryCursor(limited)!!
        val older = (backend.loadOlderMessages(room.id, cursor, 5) as LiveOperationResult.Success).data
        assertTrue(older.isNotEmpty())
        assertTrue(older.all { it.timestamp < cursor })
    }

    @Test
    fun `non host cannot remove participants`() = runBlocking {
        val backend = backend()
        val room = (createWatchParty(backend) as LiveOperationResult.Success).data
        backend.joinRoom(room.id, viewer)

        val attempt = backend.removeMember(room.id, viewer.uid, host.uid)
        assertTrue((attempt as LiveOperationResult.Error).isAuthError)
        assertEquals(2, backend.observeMembers(room.id).first().size)

        val hostRemoves = backend.removeMember(room.id, host.uid, viewer.uid)
        assertTrue(hostRemoves is LiveOperationResult.Success)
        assertEquals(1, backend.observeMembers(room.id).first().size)
    }

    @Test
    fun `missing room is reported as unavailable`() = runBlocking {
        val backend = backend()
        assertNull((backend.loadRoom("room_missing") as LiveOperationResult.Success).data)
        val resolution = com.synfusion.attendance.live.data.LiveRoomSession.resolveLink(backend, "room_missing")
        assertFalse(resolution.isActive)
        assertNotNull(resolution.message)
    }

    @Test
    fun `chat validation mirrors the security rules`() {
        assertTrue(LiveChatPolicy.validateSend("hi", true, true, true, false) is ChatValidation.Valid)
        assertTrue(LiveChatPolicy.validateSend("hi", false, true, true, false) is ChatValidation.Invalid)
        assertTrue(LiveChatPolicy.validateSend("hi", true, false, true, false) is ChatValidation.Invalid)
        assertTrue(LiveChatPolicy.validateSend("hi", true, true, false, false) is ChatValidation.Invalid)
        assertTrue(LiveChatPolicy.validateSend("hi", true, true, true, true) is ChatValidation.Invalid)
    }
}
