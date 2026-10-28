package com.synfusion.attendance.live.model

import com.synfusion.attendance.live.watchparty.PresencePolicy

/**
 * Role of a participant inside a Live room.
 *
 * The role is derived from the persisted room document (`hostUserId`) and the persisted
 * membership document. It is never inferred from local UI state, so a participant cannot
 * promote themselves to host by editing client state.
 */
enum class LiveMemberRole {
    HOST,
    MODERATOR,
    VIEWER
}

/**
 * A single persisted room membership record (`live_rooms/{roomId}/members/{uid}`).
 *
 * [lastSeenAtMillis] is refreshed by a heartbeat while the member is inside the room and is used
 * to filter stale presence after unexpected disconnects.
 */
data class LiveRoomMember(
    val uid: String,
    val displayName: String,
    val username: String = "",
    val photoUrl: String = "",
    val profession: String = "",
    val isVerified: Boolean = false,
    val role: LiveMemberRole = LiveMemberRole.VIEWER,
    val joinedAtMillis: Long = 0L,
    val lastSeenAtMillis: Long = 0L
) {
    val isHost: Boolean get() = role == LiveMemberRole.HOST

    fun isActiveAt(nowMillis: Long, windowMillis: Long = PresencePolicy.ACTIVE_WINDOW_MILLIS): Boolean {
        if (isHost) return true
        if (lastSeenAtMillis <= 0L) return false
        return nowMillis - lastSeenAtMillis <= windowMillis
    }
}

/**
 * The authenticated identity of the current device user, resolved from Firebase Auth plus the
 * persisted `public_profiles` document. Every room/member/chat write is attributed to [uid].
 */
data class LiveUserIdentity(
    val uid: String,
    val displayName: String,
    val username: String = "",
    val photoUrl: String = "",
    val profession: String = "Site Worker",
    val isVerified: Boolean = false
) {
    fun toMember(isHost: Boolean, nowMillis: Long): LiveRoomMember = LiveRoomMember(
        uid = uid,
        displayName = displayName,
        username = username,
        photoUrl = photoUrl,
        profession = profession,
        isVerified = isVerified,
        role = if (isHost) LiveMemberRole.HOST else LiveMemberRole.VIEWER,
        joinedAtMillis = nowMillis,
        lastSeenAtMillis = nowMillis
    )
}

/** Load state of a single Live room session. */
sealed class LiveRoomSessionState {
    object Idle : LiveRoomSessionState()
    object Loading : LiveRoomSessionState()

    data class Active(
        val room: LiveRoom,
        val isHost: Boolean,
        /** Viewer count rule: active unique non-host members. See [PresencePolicy]. */
        val viewerCount: Int,
        val totalActiveCount: Int
    ) : LiveRoomSessionState()

    data class Ended(val room: LiveRoom?) : LiveRoomSessionState()

    data class Unavailable(val roomId: String, val message: String) : LiveRoomSessionState()

    data class Error(val message: String) : LiveRoomSessionState()
}

/**
 * A playback command issued by the room host. It is the only mutation the shared Watch Party
 * state accepts; participants can never publish one and the backend rejects unauthorized writes.
 */
sealed class LivePlaybackCommand {
    abstract val positionSeconds: Float

    data class Play(override val positionSeconds: Float = 0f) : LivePlaybackCommand()
    data class Pause(override val positionSeconds: Float = 0f) : LivePlaybackCommand()
    data class Seek(override val positionSeconds: Float, val resumePlaying: Boolean) : LivePlaybackCommand()

    /**
     * Selects and starts a playlist item. The item details travel with the command so the
     * persisted playback document can be updated without rewriting the whole playlist.
     */
    data class SelectItem(
        val itemId: String,
        val videoId: String,
        val videoUrl: String,
        val playlistId: String? = null,
        override val positionSeconds: Float = 0f
    ) : LivePlaybackCommand()

    /** Turns the Watch Party playback off without ending the Live room. */
    object TurnOff : LivePlaybackCommand() {
        override val positionSeconds: Float = 0f
    }

    object TurnOn : LivePlaybackCommand() {
        override val positionSeconds: Float = 0f
    }

    data class SetMuted(val muted: Boolean) : LivePlaybackCommand() {
        override val positionSeconds: Float = 0f
    }

    /** Emitted by the host player when a video reaches its end. */
    data class Completed(
        val nextItemId: String? = null,
        val nextVideoId: String? = null,
        val nextVideoUrl: String? = null,
        val nextPlaylistId: String? = null
    ) : LivePlaybackCommand() {
        override val positionSeconds: Float = 0f
    }
}

/** Shared playback state returned by a backend mutation. */
data class LivePlaybackUpdate(
    val state: WatchPartyState,
    val applied: Boolean,
    val conflict: Boolean = false
)
