package com.synfusion.attendance.live.data

import com.synfusion.attendance.live.model.ChatMessage
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.model.LiveRoomMember
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState
import com.synfusion.attendance.live.watchparty.YouTubeSelection
import kotlinx.coroutines.flow.Flow

/** Everything the host supplies when creating a Live room. */
data class LiveRoomCreateRequest(
    val title: String,
    val category: String,
    val type: LiveRoomType,
    val tags: List<String> = emptyList(),
    val description: String = "",
    val isPublic: Boolean = true,
    val allowGuestRequests: Boolean = true,
    val showGifts: Boolean = true,
    val isChatEnabled: Boolean = true,
    val selection: YouTubeSelection? = null
)

/**
 * Persistence contract for a WorkDaily Live room.
 *
 * The production implementation is [com.synfusion.attendance.online.FirestoreLiveRoomBackend]
 * (Firestore + Firebase Auth). [InMemoryLiveRoomBackend] implements the exact same contract for
 * JVM unit tests so room creation, membership, playlist and revision behavior can be tested
 * without an emulator.
 *
 * Security boundaries that every implementation must uphold:
 * - room creation attributes the room to the authenticated host uid,
 * - playback/playlist writes are host-only,
 * - chat writes are stamped with the authenticated sender and require membership,
 * - ended rooms reject playback commands.
 */
interface LiveRoomBackend {

    suspend fun createRoom(
        host: LiveUserIdentity,
        request: LiveRoomCreateRequest
    ): LiveOperationResult<LiveRoom>

    fun observeRoom(roomId: String): Flow<LiveRoom?>

    fun observeActiveRooms(): Flow<List<LiveRoom>>

    suspend fun loadRoom(roomId: String): LiveOperationResult<LiveRoom?>

    suspend fun endRoom(roomId: String, hostUid: String): LiveOperationResult<Unit>

    suspend fun joinRoom(roomId: String, identity: LiveUserIdentity): LiveOperationResult<Unit>

    suspend fun heartbeat(roomId: String, uid: String): LiveOperationResult<Unit>

    suspend fun leaveRoom(roomId: String, uid: String): LiveOperationResult<Unit>

    suspend fun removeMember(roomId: String, hostUid: String, memberUid: String): LiveOperationResult<Unit>

    fun observeMembers(roomId: String): Flow<List<LiveRoomMember>>

    fun observeWatchParty(roomId: String): Flow<WatchPartyState?>

    fun observePlaylist(roomId: String): Flow<List<WatchPartyPlaylistItem>>

    /**
     * Applies a host playback command atomically.
     *
     * Implementations MUST reject the write when [expectedRevision] no longer equals the stored
     * revision (returns an [LiveOperationResult.Error] with `isConflict = true`) and when
     * [hostUid] is not the persisted room host.
     */
    suspend fun applyPlaybackCommand(
        roomId: String,
        hostUid: String,
        expectedRevision: Long,
        command: LivePlaybackCommand
    ): LiveOperationResult<WatchPartyState>

    suspend fun addPlaylistItem(
        roomId: String,
        hostUid: String,
        item: WatchPartyPlaylistItem
    ): LiveOperationResult<Unit>

    suspend fun removePlaylistItem(
        roomId: String,
        hostUid: String,
        itemId: String
    ): LiveOperationResult<Unit>

    fun observeMessages(roomId: String, limit: Int): Flow<List<ChatMessage>>

    suspend fun sendMessage(
        roomId: String,
        identity: LiveUserIdentity,
        text: String,
        clientMessageId: String
    ): LiveOperationResult<ChatMessage>

    suspend fun loadOlderMessages(
        roomId: String,
        beforeTimestampMillis: Long,
        limit: Int
    ): LiveOperationResult<List<ChatMessage>>

    suspend fun sendSystemEvent(
        roomId: String,
        message: ChatMessage
    ): LiveOperationResult<ChatMessage>
}
