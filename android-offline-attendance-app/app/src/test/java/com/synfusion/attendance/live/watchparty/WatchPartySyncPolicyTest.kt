package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPartySyncPolicyTest {

    private fun state(
        revision: Long = 1L,
        status: WatchPartyPlaybackStatus = WatchPartyPlaybackStatus.PAUSED,
        position: Float = 30f,
        updatedAtMillis: Long = 1_000_000L,
        commandId: String = "cmd-1",
        videoId: String = "dQw4w9WgXcQ"
    ) = WatchPartyState(
        roomId = "room_1",
        videoId = videoId,
        status = status,
        positionSeconds = position,
        updatedAtMillis = updatedAtMillis,
        hostUserId = "host_1",
        revision = revision,
        commandId = commandId
    )

    @Test
    fun `stale revisions are ignored`() {
        val decision = WatchPartySyncPolicy.decide(state(revision = 4L), appliedRevision = 5L, lastLocalCommandId = null)
        assertTrue(decision is PlaybackSyncDecision.Ignore)
    }

    @Test
    fun `newer revisions are applied`() {
        val decision = WatchPartySyncPolicy.decide(state(revision = 6L), appliedRevision = 5L, lastLocalCommandId = null)
        assertTrue(decision is PlaybackSyncDecision.Apply)
        assertEquals(WatchPartyPlaybackStatus.PAUSED, (decision as PlaybackSyncDecision.Apply).status)
    }

    @Test
    fun `host echo of its own command is flagged`() {
        val decision = WatchPartySyncPolicy.decide(
            state(revision = 6L, commandId = "cmd-42"),
            appliedRevision = 5L,
            lastLocalCommandId = "cmd-42"
        ) as PlaybackSyncDecision.Apply
        assertTrue(decision.isOwnEcho)
    }

    @Test
    fun `playing position advances with elapsed time`() {
        val playing = state(status = WatchPartyPlaybackStatus.PLAYING, position = 10f, updatedAtMillis = 1_000L)
        assertEquals(15f, WatchPartySyncPolicy.projectedPositionSeconds(playing, 6_000L), 0.001f)
    }

    @Test
    fun `paused position does not advance`() {
        val paused = state(status = WatchPartyPlaybackStatus.PAUSED, position = 10f, updatedAtMillis = 1_000L)
        assertEquals(10f, WatchPartySyncPolicy.projectedPositionSeconds(paused, 60_000L), 0.001f)
    }

    @Test
    fun `projection is clamped for very old timestamps`() {
        val playing = state(status = WatchPartyPlaybackStatus.PLAYING, position = 0f, updatedAtMillis = 0L)
        val projected = WatchPartySyncPolicy.projectedPositionSeconds(playing, Long.MAX_VALUE / 2)
        assertTrue(projected <= WatchPartySyncPolicy.MAX_PROJECTION_SECONDS)
    }

    @Test
    fun `drift tolerance only corrects real drift`() {
        assertFalse(WatchPartySyncPolicy.shouldCorrectPosition(100f, 101f))
        assertTrue(WatchPartySyncPolicy.shouldCorrectPosition(100f, 104f))
    }

    @Test
    fun `only the persisted host is authorized`() {
        assertTrue(WatchPartySyncPolicy.isAuthorizedHost("host_1", "host_1"))
        assertFalse(WatchPartySyncPolicy.isAuthorizedHost("viewer_1", "host_1"))
        assertFalse(WatchPartySyncPolicy.isAuthorizedHost(null, "host_1"))
        assertFalse(WatchPartySyncPolicy.isAuthorizedHost("host_1", null))
    }

    @Test
    fun `revision must match expected to advance`() {
        assertEquals(8L, WatchPartySyncPolicy.nextRevision(serverRevision = 7L, expectedRevision = 7L))
        assertNull(WatchPartySyncPolicy.nextRevision(serverRevision = 9L, expectedRevision = 7L))
    }

    @Test
    fun `ended rooms reject playback commands`() {
        assertTrue(WatchPartySyncPolicy.canAcceptPlaybackCommands(isLive = true, endedAtMillis = null))
        assertFalse(WatchPartySyncPolicy.canAcceptPlaybackCommands(isLive = false, endedAtMillis = null))
        assertFalse(WatchPartySyncPolicy.canAcceptPlaybackCommands(isLive = true, endedAtMillis = 123L))
    }

    @Test
    fun `play command bumps revision and sets playing`() {
        val next = WatchPartySyncPolicy.applyCommand(
            current = state(status = WatchPartyPlaybackStatus.PAUSED, position = 5f),
            command = LivePlaybackCommand.Play(5f),
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-2"
        )
        assertEquals(2L, next.revision)
        assertEquals(WatchPartyPlaybackStatus.PLAYING, next.status)
        assertEquals("cmd-2", next.commandId)
        assertEquals("host_1", next.updatedByUserId)
    }

    @Test
    fun `pause keeps the reported position`() {
        val next = WatchPartySyncPolicy.applyCommand(
            current = state(status = WatchPartyPlaybackStatus.PLAYING, position = 12f),
            command = LivePlaybackCommand.Pause(42.5f),
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-3"
        )
        assertEquals(WatchPartyPlaybackStatus.PAUSED, next.status)
        assertEquals(42.5f, next.positionSeconds, 0.001f)
    }

    @Test
    fun `seek while playing resumes playback`() {
        val next = WatchPartySyncPolicy.applyCommand(
            current = state(status = WatchPartyPlaybackStatus.PAUSED, position = 1f),
            command = LivePlaybackCommand.Seek(90f, resumePlaying = true),
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-4"
        )
        assertEquals(WatchPartyPlaybackStatus.PLAYING, next.status)
        assertEquals(90f, next.positionSeconds, 0.001f)
    }

    @Test
    fun `select item switches video without touching the playlist list`() {
        val next = WatchPartySyncPolicy.applyCommand(
            current = state(),
            command = LivePlaybackCommand.SelectItem("item_2", "abcdefghijk", "https://youtu.be/abcdefghijk"),
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-5"
        )
        assertEquals("abcdefghijk", next.videoId)
        assertEquals("item_2", next.activePlaylistItemId)
        assertEquals(WatchPartyPlaybackStatus.PLAYING, next.status)
        assertEquals(0f, next.positionSeconds, 0.001f)
        assertTrue(next.playlist.isEmpty())
    }

    @Test
    fun `turn off pauses but keeps the room usable`() {
        val next = WatchPartySyncPolicy.applyCommand(
            current = state(status = WatchPartyPlaybackStatus.PLAYING),
            command = LivePlaybackCommand.TurnOff,
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-6"
        )
        assertTrue(next.isTurnedOff)
        assertEquals(WatchPartyPlaybackStatus.PAUSED, next.status)
    }

    @Test
    fun `completed command advances to the next item or ends`() {
        val advanced = WatchPartySyncPolicy.applyCommand(
            current = state(),
            command = LivePlaybackCommand.Completed("item_3", "abcdefghijk", "https://youtu.be/abcdefghijk"),
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-7"
        )
        assertEquals("abcdefghijk", advanced.videoId)
        assertEquals("item_3", advanced.activePlaylistItemId)

        val ended = WatchPartySyncPolicy.applyCommand(
            current = state(),
            command = LivePlaybackCommand.Completed(),
            actorUid = "host_1",
            nowMillis = 2_000L,
            commandId = "cmd-8"
        )
        assertEquals(WatchPartyPlaybackStatus.ENDED, ended.status)
    }

    @Test
    fun `every command produces a distinct revision`() {
        var current = state()
        repeat(5) { index ->
            val previous = current.revision
            current = WatchPartySyncPolicy.applyCommand(
                current = current,
                command = LivePlaybackCommand.Play(index.toFloat()),
                actorUid = "host_1",
                nowMillis = 3_000L + index,
                commandId = "cmd-loop-$index"
            )
            assertNotEquals(previous, current.revision)
        }
        assertEquals(6L, current.revision)
    }

    @Test
    fun `position correction interval is bounded`() {
        assertTrue(WatchPartySyncPolicy.POSITION_CORRECTION_INTERVAL_MILLIS >= 10_000L)
        assertFalse(WatchPartySyncPolicy.isCorrectionDue(lastPublishedAtMillis = 1_000L, nowMillis = 2_000L))
        assertTrue(
            WatchPartySyncPolicy.isCorrectionDue(
                lastPublishedAtMillis = 1_000L,
                nowMillis = 1_000L + WatchPartySyncPolicy.POSITION_CORRECTION_INTERVAL_MILLIS
            )
        )
    }
}
