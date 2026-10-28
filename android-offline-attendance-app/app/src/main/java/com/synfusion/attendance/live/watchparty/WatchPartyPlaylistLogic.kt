package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState

/** Result of a playlist mutation performed by the host. */
sealed class PlaylistMutation {
    data class Success(val state: WatchPartyState, val item: WatchPartyPlaylistItem? = null) : PlaylistMutation()
    data class Duplicate(val existing: WatchPartyPlaylistItem) : PlaylistMutation()
    data class Rejected(val reason: String) : PlaylistMutation()
}

/** Which item should play after the current item finishes or is removed. */
data class PlaylistAdvance(val item: WatchPartyPlaylistItem?)

/**
 * Pure playlist rules for the Watch Party sheet.
 *
 * All mutations bump [WatchPartyState.revision] exactly once and never rewrite the whole playlist:
 * the backend persists the changed item document separately and only the playback document is
 * updated here, which is what keeps position updates cheap.
 */
object WatchPartyPlaylistLogic {

    const val MAX_PLAYLIST_SIZE = 200
    const val MAX_TITLE_LENGTH = 200

    fun ordered(items: List<WatchPartyPlaylistItem>): List<WatchPartyPlaylistItem> =
        items.sortedWith(compareBy({ it.addedAtMillis }, { it.id }))

    /**
     * Adds a validated YouTube selection. Duplicates are rejected explicitly (the sheet surfaces
     * "already in playlist") rather than silently creating a second identical row.
     */
    fun addItem(
        current: WatchPartyState,
        playlist: List<WatchPartyPlaylistItem>,
        selection: YouTubeSelection,
        identity: LiveUserIdentity,
        nowMillis: Long,
        itemId: String
    ): PlaylistMutation {
        if (playlist.size >= MAX_PLAYLIST_SIZE) {
            return PlaylistMutation.Rejected("This playlist is full (${MAX_PLAYLIST_SIZE} items).")
        }
        val duplicate = playlist.firstOrNull { existing ->
            if (selection.videoId != null) {
                existing.videoId.equals(selection.videoId, ignoreCase = true)
            } else {
                existing.videoId.isBlank() && existing.videoUrl.contains(selection.playlistId.orEmpty())
            }
        }
        if (duplicate != null) {
            return PlaylistMutation.Duplicate(duplicate)
        }

        val title = if (selection.isPlaylistOnly) {
            "YouTube Playlist (${selection.playlistId})"
        } else {
            "YouTube Video (${selection.videoId})"
        }

        val item = WatchPartyPlaylistItem(
            id = itemId,
            videoId = selection.videoId.orEmpty(),
            videoUrl = selection.canonicalUrl,
            title = title.take(MAX_TITLE_LENGTH),
            thumbnailUrl = selection.thumbnailUrl.orEmpty(),
            addedByUserId = identity.uid,
            addedByUserName = identity.displayName,
            addedAtMillis = nowMillis
        )

        val becomesActive = current.activePlaylistItemId.isNullOrBlank() || current.videoId.isBlank()
        val nextState = current.copy(
            revision = current.revision + 1L,
            updatedAtMillis = nowMillis,
            updatedByUserId = identity.uid,
            videoId = if (becomesActive) item.videoId else current.videoId,
            activePlaylistItemId = if (becomesActive) item.id else current.activePlaylistItemId,
            playlistId = current.playlistId ?: selection.playlistId,
            status = if (becomesActive) WatchPartyPlaybackStatus.UNSTARTED else current.status
        )
        return PlaylistMutation.Success(nextState, item)
    }

    /**
     * Removes an item. Removing the currently playing item advances to the next remaining item
     * (or the previous one when the removed item was last) and pauses it at 0 so the host decides
     * when to resume; when nothing remains the Watch Party simply has no active video.
     */
    fun removeItem(
        current: WatchPartyState,
        playlist: List<WatchPartyPlaylistItem>,
        itemId: String,
        actorUid: String,
        nowMillis: Long
    ): PlaylistMutation {
        val orderedPlaylist = ordered(playlist)
        val index = orderedPlaylist.indexOfFirst { it.id == itemId }
        if (index < 0) return PlaylistMutation.Rejected("That playlist item no longer exists.")

        val remaining = orderedPlaylist.filterNot { it.id == itemId }
        val wasActive = current.activePlaylistItemId == itemId
        val replacement = if (wasActive) {
            remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))
        } else {
            remaining.firstOrNull { it.id == current.activePlaylistItemId }
        }

        val nextState = current.copy(
            revision = current.revision + 1L,
            updatedAtMillis = nowMillis,
            updatedByUserId = actorUid,
            activePlaylistItemId = replacement?.id,
            videoId = when {
                wasActive && replacement == null -> ""
                wasActive -> replacement?.videoId.orEmpty()
                else -> current.videoId
            },
            status = when {
                wasActive && replacement == null -> WatchPartyPlaybackStatus.ENDED
                wasActive -> WatchPartyPlaybackStatus.PAUSED
                else -> current.status
            },
            positionSeconds = if (wasActive) 0f else current.positionSeconds
        )
        return PlaylistMutation.Success(nextState, replacement)
    }

    fun selectItem(
        current: WatchPartyState,
        playlist: List<WatchPartyPlaylistItem>,
        itemId: String,
        actorUid: String,
        nowMillis: Long
    ): PlaylistMutation {
        val item = playlist.firstOrNull { it.id == itemId }
            ?: return PlaylistMutation.Rejected("That playlist item no longer exists.")
        if (item.videoId.isBlank()) {
            return PlaylistMutation.Rejected("Only single videos can be selected for playback.")
        }
        val nextState = WatchPartySyncPolicy.applyCommand(
            current = current,
            command = LivePlaybackCommand.SelectItem(
                itemId = item.id,
                videoId = item.videoId,
                videoUrl = item.videoUrl
            ),
            actorUid = actorUid,
            nowMillis = nowMillis,
            commandId = "select-${item.id}-$nowMillis"
        )
        return PlaylistMutation.Success(nextState, item)
    }

    fun turnOff(current: WatchPartyState, actorUid: String, nowMillis: Long): PlaylistMutation.Success {
        val next = WatchPartySyncPolicy.applyCommand(
            current = current,
            command = LivePlaybackCommand.TurnOff,
            actorUid = actorUid,
            nowMillis = nowMillis,
            commandId = "off-$nowMillis"
        )
        return PlaylistMutation.Success(next)
    }

    fun turnOn(current: WatchPartyState, actorUid: String, nowMillis: Long): PlaylistMutation.Success {
        val next = WatchPartySyncPolicy.applyCommand(
            current = current,
            command = LivePlaybackCommand.TurnOn,
            actorUid = actorUid,
            nowMillis = nowMillis,
            commandId = "on-$nowMillis"
        )
        return PlaylistMutation.Success(next)
    }

    /**
     * Next item after [currentItemId] in playback order, used when a video completes.
     * Returns `null` when the end of the playlist was reached.
     */
    fun nextItem(playlist: List<WatchPartyPlaylistItem>, currentItemId: String?): PlaylistAdvance {
        val orderedPlaylist = ordered(playlist).filter { it.videoId.isNotBlank() }
        if (orderedPlaylist.isEmpty()) return PlaylistAdvance(null)
        if (currentItemId.isNullOrBlank()) return PlaylistAdvance(orderedPlaylist.first())
        val index = orderedPlaylist.indexOfFirst { it.id == currentItemId }
        if (index < 0) return PlaylistAdvance(orderedPlaylist.first())
        return PlaylistAdvance(orderedPlaylist.getOrNull(index + 1))
    }

    /** Active item is missing from the playlist (deleted on another device) -> choose a fallback. */
    fun reconcileActiveItem(
        current: WatchPartyState,
        playlist: List<WatchPartyPlaylistItem>
    ): WatchPartyState? {
        if (playlist.isEmpty()) {
            if (current.activePlaylistItemId == null && current.videoId.isBlank()) return null
            return current.copy(
                activePlaylistItemId = null,
                videoId = "",
                status = WatchPartyPlaybackStatus.ENDED,
                positionSeconds = 0f
            )
        }
        val activeExists = playlist.any { it.id == current.activePlaylistItemId }
        if (activeExists) return null
        val first = ordered(playlist).first()
        return current.copy(
            activePlaylistItemId = first.id,
            videoId = first.videoId,
            status = WatchPartyPlaybackStatus.PAUSED,
            positionSeconds = 0f
        )
    }

    /** Builds a state when a room is created with an initial selection. */
    fun initialState(
        roomId: String,
        hostUid: String,
        selection: YouTubeSelection?,
        nowMillis: Long
    ): WatchPartyState {
        val videoId = selection?.videoId.orEmpty()
        return WatchPartyState(
            roomId = roomId,
            videoId = videoId,
            playlistId = selection?.playlistId,
            status = WatchPartyPlaybackStatus.UNSTARTED,
            positionSeconds = 0f,
            updatedAtMillis = nowMillis,
            hostUserId = hostUid,
            revision = 1L,
            playlist = emptyList(),
            activePlaylistItemId = null,
            updatedByUserId = hostUid,
            commandId = "init-$roomId"
        )
    }
}
