package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPartyPlaylistLogicTest {

    private val identity = LiveUserIdentity(
        uid = "host_1",
        displayName = "Asha Kumar",
        username = "asha",
        profession = "Site Engineer"
    )

    private fun item(id: String, videoId: String, addedAt: Long) = WatchPartyPlaylistItem(
        id = id,
        videoId = videoId,
        videoUrl = "https://youtu.be/$videoId",
        title = "Video $videoId",
        thumbnailUrl = YouTubeLinkParser.thumbnailUrl(videoId),
        addedByUserId = "host_1",
        addedByUserName = "Asha Kumar",
        addedAtMillis = addedAt
    )

    private fun emptyState() = WatchPartyState(
        roomId = "room_1",
        hostUserId = "host_1",
        revision = 3L,
        updatedAtMillis = 1_000L
    )

    private fun selection(videoId: String) =
        (YouTubeLinkParser.parse("https://youtu.be/$videoId") as YouTubeParseResult.Success).selection

    @Test
    fun `first added video becomes the active item`() {
        val mutation = WatchPartyPlaylistLogic.addItem(
            current = emptyState(),
            playlist = emptyList(),
            selection = selection("dQw4w9WgXcQ"),
            identity = identity,
            nowMillis = 2_000L,
            itemId = "dQw4w9WgXcQ"
        ) as PlaylistMutation.Success

        assertEquals("dQw4w9WgXcQ", mutation.state.videoId)
        assertEquals("dQw4w9WgXcQ", mutation.state.activePlaylistItemId)
        assertEquals(4L, mutation.state.revision)
        assertEquals("Asha Kumar", mutation.item?.addedByUserName)
        assertEquals("host_1", mutation.item?.addedByUserId)
        assertTrue(mutation.item!!.thumbnailUrl.contains("i.ytimg.com"))
    }

    @Test
    fun `duplicate videos are rejected instead of being added twice`() {
        val playlist = listOf(item("dQw4w9WgXcQ", "dQw4w9WgXcQ", 1_000L))
        val mutation = WatchPartyPlaylistLogic.addItem(
            current = emptyState(),
            playlist = playlist,
            selection = selection("dQw4w9WgXcQ"),
            identity = identity,
            nowMillis = 2_000L,
            itemId = "dQw4w9WgXcQ"
        )
        assertTrue(mutation is PlaylistMutation.Duplicate)
    }

    @Test
    fun `playlist size is bounded`() {
        val bigPlaylist = (0 until WatchPartyPlaylistLogic.MAX_PLAYLIST_SIZE).map { index ->
            item("item_$index", "videoId${index.toString().padStart(4, '0')}", index.toLong())
        }
        val mutation = WatchPartyPlaylistLogic.addItem(
            current = emptyState(),
            playlist = bigPlaylist,
            selection = selection("dQw4w9WgXcQ"),
            identity = identity,
            nowMillis = 2_000L,
            itemId = "dQw4w9WgXcQ"
        )
        assertTrue(mutation is PlaylistMutation.Rejected)
    }

    @Test
    fun `removing the active item advances to the next one and pauses`() {
        val playlist = listOf(
            item("a", "aaaaaaaaaaa", 1L),
            item("b", "bbbbbbbbbbb", 2L),
            item("c", "ccccccccccc", 3L)
        )
        val state = emptyState().copy(activePlaylistItemId = "b", videoId = "bbbbbbbbbbb", status = WatchPartyPlaybackStatus.PLAYING)
        val mutation = WatchPartyPlaylistLogic.removeItem(state, playlist, "b", "host_1", 5_000L) as PlaylistMutation.Success

        assertEquals("c", mutation.state.activePlaylistItemId)
        assertEquals("ccccccccccc", mutation.state.videoId)
        assertEquals(WatchPartyPlaybackStatus.PAUSED, mutation.state.status)
        assertEquals(0f, mutation.state.positionSeconds, 0.001f)
        assertEquals(4L, mutation.state.revision)
    }

    @Test
    fun `removing the last active item falls back to the previous one`() {
        val playlist = listOf(item("a", "aaaaaaaaaaa", 1L), item("b", "bbbbbbbbbbb", 2L))
        val state = emptyState().copy(activePlaylistItemId = "b", videoId = "bbbbbbbbbbb")
        val mutation = WatchPartyPlaylistLogic.removeItem(state, playlist, "b", "host_1", 5_000L) as PlaylistMutation.Success
        assertEquals("a", mutation.state.activePlaylistItemId)
    }

    @Test
    fun `removing the only item ends playback but keeps the room`() {
        val playlist = listOf(item("a", "aaaaaaaaaaa", 1L))
        val state = emptyState().copy(activePlaylistItemId = "a", videoId = "aaaaaaaaaaa")
        val mutation = WatchPartyPlaylistLogic.removeItem(state, playlist, "a", "host_1", 5_000L) as PlaylistMutation.Success
        assertNull(mutation.state.activePlaylistItemId)
        assertEquals("", mutation.state.videoId)
        assertEquals(WatchPartyPlaybackStatus.ENDED, mutation.state.status)
    }

    @Test
    fun `removing an item that is not the active one keeps playback untouched`() {
        val playlist = listOf(item("a", "aaaaaaaaaaa", 1L), item("b", "bbbbbbbbbbb", 2L))
        val state = emptyState().copy(activePlaylistItemId = "a", videoId = "aaaaaaaaaaa", status = WatchPartyPlaybackStatus.PLAYING)
        val mutation = WatchPartyPlaylistLogic.removeItem(state, playlist, "b", "host_1", 5_000L) as PlaylistMutation.Success
        assertEquals("a", mutation.state.activePlaylistItemId)
        assertEquals(WatchPartyPlaybackStatus.PLAYING, mutation.state.status)
    }

    @Test
    fun `selecting an item plays it from the start`() {
        val playlist = listOf(item("a", "aaaaaaaaaaa", 1L), item("b", "bbbbbbbbbbb", 2L))
        val state = emptyState().copy(activePlaylistItemId = "a", videoId = "aaaaaaaaaaa")
        val mutation = WatchPartyPlaylistLogic.selectItem(state, playlist, "b", "host_1", 5_000L) as PlaylistMutation.Success
        assertEquals("b", mutation.state.activePlaylistItemId)
        assertEquals("bbbbbbbbbbb", mutation.state.videoId)
        assertEquals(WatchPartyPlaybackStatus.PLAYING, mutation.state.status)
    }

    @Test
    fun `selecting a missing item is rejected`() {
        val mutation = WatchPartyPlaylistLogic.selectItem(emptyState(), emptyList(), "missing", "host_1", 5_000L)
        assertTrue(mutation is PlaylistMutation.Rejected)
    }

    @Test
    fun `next item advances in add order and stops at the end`() {
        val playlist = listOf(
            item("b", "bbbbbbbbbbb", 2L),
            item("a", "aaaaaaaaaaa", 1L),
            item("c", "ccccccccccc", 3L)
        )
        assertEquals("b", WatchPartyPlaylistLogic.nextItem(playlist, "a").item?.id)
        assertEquals("c", WatchPartyPlaylistLogic.nextItem(playlist, "b").item?.id)
        assertNull(WatchPartyPlaylistLogic.nextItem(playlist, "c").item)
    }

    @Test
    fun `reconcile picks a fallback when the active item disappeared`() {
        val playlist = listOf(item("a", "aaaaaaaaaaa", 1L))
        val state = emptyState().copy(activePlaylistItemId = "deleted", videoId = "zzzzzzzzzzz")
        val reconciled = WatchPartyPlaylistLogic.reconcileActiveItem(state, playlist)!!
        assertEquals("a", reconciled.activePlaylistItemId)
        assertEquals("aaaaaaaaaaa", reconciled.videoId)
    }

    @Test
    fun `reconcile reports nothing when the active item still exists`() {
        val playlist = listOf(item("a", "aaaaaaaaaaa", 1L))
        val state = emptyState().copy(activePlaylistItemId = "a", videoId = "aaaaaaaaaaa")
        assertNull(WatchPartyPlaylistLogic.reconcileActiveItem(state, playlist))
    }

    @Test
    fun `turn off and on preserve the queue`() {
        val state = emptyState().copy(playlist = listOf(item("a", "aaaaaaaaaaa", 1L)))
        val off = WatchPartyPlaylistLogic.turnOff(state, "host_1", 5_000L).state
        assertTrue(off.isTurnedOff)
        assertEquals(1, off.playlist.size)

        val on = WatchPartyPlaylistLogic.turnOn(off, "host_1", 6_000L).state
        assertEquals(false, on.isTurnedOff)
        assertEquals(1, on.playlist.size)
    }

    @Test
    fun `initial state starts unstarted with the selected video`() {
        val state = WatchPartyPlaylistLogic.initialState(
            roomId = "room_9",
            hostUid = "host_1",
            selection = selection("dQw4w9WgXcQ"),
            nowMillis = 1_000L
        )
        assertEquals("dQw4w9WgXcQ", state.videoId)
        assertEquals("host_1", state.hostUserId)
        assertEquals(WatchPartyPlaybackStatus.UNSTARTED, state.status)
        assertEquals(1L, state.revision)
    }
}
