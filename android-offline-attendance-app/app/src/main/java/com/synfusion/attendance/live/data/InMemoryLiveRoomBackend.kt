package com.synfusion.attendance.live.data

import com.synfusion.attendance.live.model.ChatMessage
import com.synfusion.attendance.live.model.LiveMemberRole
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.model.LiveRoomMember
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState
import com.synfusion.attendance.live.watchparty.ChatValidation
import com.synfusion.attendance.live.watchparty.LiveChatPolicy
import com.synfusion.attendance.live.watchparty.PlaylistMutation
import com.synfusion.attendance.live.watchparty.PresencePolicy
import com.synfusion.attendance.live.watchparty.WatchPartyPlaylistLogic
import com.synfusion.attendance.live.watchparty.WatchPartySyncPolicy
import com.synfusion.attendance.live.watchparty.YouTubeLinkParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Deterministic in-memory implementation of [LiveRoomBackend].
 *
 * It mirrors the authorization and revision rules of the Firestore implementation and is used by
 * the JVM unit tests (no emulator required) plus as a safe local backend when Firebase is not
 * configured on the device (e.g. a debug build without `google-services.json`). It never invents
 * users: every write is attributed to the identity passed by the caller.
 */
class InMemoryLiveRoomBackend(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString().replace("-", "").take(20) }
) : LiveRoomBackend {

    private val rooms = MutableStateFlow<Map<String, LiveRoom>>(emptyMap())
    private val watchPartyStates = MutableStateFlow<Map<String, WatchPartyState>>(emptyMap())
    private val playlists = MutableStateFlow<Map<String, List<WatchPartyPlaylistItem>>>(emptyMap())
    private val members = MutableStateFlow<Map<String, Map<String, LiveRoomMember>>>(emptyMap())
    private val messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    /** Rooms currently live per host, used to reject duplicate room creation. */
    private val activeRoomByHost = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun createRoom(
        host: LiveUserIdentity,
        request: LiveRoomCreateRequest
    ): LiveOperationResult<LiveRoom> {
        if (host.uid.isBlank()) {
            return LiveOperationResult.Error("Sign in to start a live room.", isAuthError = true)
        }
        val existingRoomId = activeRoomByHost.value[host.uid]
        if (existingRoomId != null && rooms.value[existingRoomId]?.isLive == true) {
            return LiveOperationResult.Error(
                "You already have an active live room. End it before starting a new one.",
                isConflict = true
            )
        }
        val now = clock()
        val roomId = "room_" + idGenerator()
        val initialWatchParty = if (request.type == LiveRoomType.YOUTUBE_WATCH_PARTY) {
            WatchPartyPlaylistLogic.initialState(roomId, host.uid, request.selection, now)
        } else {
            null
        }
        val room = LiveRoom(
            id = roomId,
            title = request.title.ifBlank { "WorkDaily Live" },
            creatorId = host.uid,
            creatorName = host.displayName,
            creatorAvatar = host.photoUrl,
            creatorProfession = host.profession.ifBlank { "Site Worker" },
            tradeRole = host.profession.ifBlank { "Site Worker" },
            category = request.category.ifBlank { "Site Safety" },
            type = request.type,
            viewerCount = 0,
            peakViewerCount = 0,
            isLive = true,
            isHost = true,
            hostUserId = host.uid,
            tags = request.tags,
            description = request.description,
            isPublic = request.isPublic,
            allowGuestRequests = request.allowGuestRequests,
            showGifts = request.showGifts,
            isChatEnabled = request.isChatEnabled,
            startTimeMillis = now,
            youtubeVideoUrl = request.selection?.canonicalUrl,
            youtubeVideoId = request.selection?.videoId,
            youtubePlaylistId = request.selection?.playlistId,
            watchPartyState = initialWatchParty,
            hostUsername = host.username,
            hostIsVerified = host.isVerified
        )

        rooms.update { it + (roomId to room) }
        activeRoomByHost.update { it + (host.uid to roomId) }

        initialWatchParty?.let { state ->
            watchPartyStates.update { it + (roomId to state) }
            val selection = request.selection
            if (selection != null && selection.isVideoPlayable) {
                val item = WatchPartyPlaylistItem(
                    id = selection.videoId!!,
                    videoId = selection.videoId,
                    videoUrl = selection.canonicalUrl,
                    title = "YouTube Video (${selection.videoId})",
                    thumbnailUrl = YouTubeLinkParser.thumbnailUrl(selection.videoId),
                    addedByUserId = host.uid,
                    addedByUserName = host.displayName,
                    addedAtMillis = now
                )
                playlists.update { it + (roomId to listOf(item)) }
                watchPartyStates.update { states ->
                    val current = states[roomId] ?: return@update states
                    states + (roomId to current.copy(
                        activePlaylistItemId = item.id,
                        videoId = item.videoId
                    ))
                }
            }
        }
        // The host is a room member from the moment the room exists.
        joinRoom(roomId, host)
        return LiveOperationResult.Success(rooms.value.getValue(roomId))
    }

    override fun observeRoom(roomId: String): Flow<LiveRoom?> = rooms.map { it[roomId] }

    override fun observeActiveRooms(): Flow<List<LiveRoom>> = rooms.map { map ->
        map.values.filter { it.isLive && it.endedAtMillis == null && it.isPublic }
    }

    override suspend fun loadRoom(roomId: String): LiveOperationResult<LiveRoom?> =
        LiveOperationResult.Success(rooms.value[roomId])

    override suspend fun endRoom(roomId: String, hostUid: String): LiveOperationResult<Unit> {
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        if (room.hostUserId != hostUid) {
            return LiveOperationResult.Error("Only the host can end this live room.", isAuthError = true)
        }
        rooms.update { current ->
            val existing = current[roomId] ?: return@update current
            current + (roomId to existing.copy(isLive = false, endedAtMillis = clock(), statusText = "ENDED"))
        }
        activeRoomByHost.update { it - hostUid }
        return LiveOperationResult.Success(Unit)
    }

    override suspend fun joinRoom(
        roomId: String,
        identity: LiveUserIdentity
    ): LiveOperationResult<Unit> {
        if (identity.uid.isBlank()) {
            return LiveOperationResult.Error("Sign in to join this live room.", isAuthError = true)
        }
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        if (!room.isLive) {
            return LiveOperationResult.Error("This live room has ended.")
        }
        val now = clock()
        members.update { current ->
            val roomMembers = current[roomId].orEmpty().toMutableMap()
            val existing = roomMembers[identity.uid]
            roomMembers[identity.uid] = identity.toMember(
                isHost = identity.uid == room.hostUserId,
                nowMillis = now
            ).copy(joinedAtMillis = existing?.joinedAtMillis ?: now)
            current + (roomId to roomMembers)
        }
        return LiveOperationResult.Success(Unit)
    }

    override suspend fun heartbeat(roomId: String, uid: String): LiveOperationResult<Unit> {
        val now = clock()
        var found = false
        members.update { current ->
            val roomMembers = current[roomId].orEmpty().toMutableMap()
            val existing = roomMembers[uid]
            if (existing != null) {
                found = true
                roomMembers[uid] = existing.copy(lastSeenAtMillis = now)
            }
            current + (roomId to roomMembers)
        }
        return if (found) {
            LiveOperationResult.Success(Unit)
        } else {
            LiveOperationResult.Error("Membership expired; rejoin the room.")
        }
    }

    override suspend fun leaveRoom(roomId: String, uid: String): LiveOperationResult<Unit> {
        members.update { current ->
            val roomMembers = current[roomId].orEmpty().toMutableMap()
            roomMembers.remove(uid)
            current + (roomId to roomMembers)
        }
        return LiveOperationResult.Success(Unit)
    }

    override suspend fun removeMember(
        roomId: String,
        hostUid: String,
        memberUid: String
    ): LiveOperationResult<Unit> {
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        if (!PresencePolicy.canModerate(hostUid, room.hostUserId, memberUid)) {
            return LiveOperationResult.Error(
                "Only the host can remove participants.",
                isAuthError = true
            )
        }
        members.update { current ->
            val roomMembers = current[roomId].orEmpty().toMutableMap()
            roomMembers.remove(memberUid)
            current + (roomId to roomMembers)
        }
        return LiveOperationResult.Success(Unit)
    }

    override fun observeMembers(roomId: String): Flow<List<LiveRoomMember>> =
        members.map { snapshot ->
            PresencePolicy.sort(snapshot[roomId]?.values.orEmpty())
        }

    override fun observeWatchParty(roomId: String): Flow<WatchPartyState?> =
        watchPartyStates.map { it[roomId] }

    override fun observePlaylist(roomId: String): Flow<List<WatchPartyPlaylistItem>> =
        playlists.map { WatchPartyPlaylistLogic.ordered(it[roomId].orEmpty()) }

    override suspend fun applyPlaybackCommand(
        roomId: String,
        hostUid: String,
        expectedRevision: Long,
        command: LivePlaybackCommand
    ): LiveOperationResult<WatchPartyState> {
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        if (room.hostUserId != hostUid) {
            return LiveOperationResult.Error("Only the host can change playback.", isAuthError = true)
        }
        if (!WatchPartySyncPolicy.canAcceptPlaybackCommands(room.isLive, room.endedAtMillis)) {
            return LiveOperationResult.Error("This live room has ended.")
        }
        val current = watchPartyStates.value[roomId]
            ?: return LiveOperationResult.Error("Watch Party is not enabled for this room.")
        if (current.revision != expectedRevision) {
            return LiveOperationResult.Error(
                "Playback state changed on another device (revision ${current.revision}).",
                isConflict = true
            )
        }
        val next = WatchPartySyncPolicy.applyCommand(
            current = current,
            command = command,
            actorUid = hostUid,
            nowMillis = clock(),
            commandId = "cmd-" + idGenerator()
        )
        watchPartyStates.update { it + (roomId to next) }
        return LiveOperationResult.Success(next)
    }

    override suspend fun addPlaylistItem(
        roomId: String,
        hostUid: String,
        item: WatchPartyPlaylistItem
    ): LiveOperationResult<Unit> {
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        if (room.hostUserId != hostUid) {
            return LiveOperationResult.Error("Only the host can edit the playlist.", isAuthError = true)
        }
        if (!room.isLive) return LiveOperationResult.Error("This live room has ended.")
        val currentPlaylist = playlists.value[roomId].orEmpty()
        if (currentPlaylist.any { it.id == item.id || it.videoId == item.videoId }) {
            return LiveOperationResult.Error("That video is already in the playlist.", isConflict = true)
        }
        val current = watchPartyStates.value[roomId]
            ?: return LiveOperationResult.Error("Watch Party is not enabled for this room.")
        val activeWasEmpty = current.activePlaylistItemId.isNullOrBlank() || current.videoId.isBlank()
        val nextState = current.copy(
            revision = current.revision + 1L,
            updatedAtMillis = clock(),
            updatedByUserId = hostUid,
            activePlaylistItemId = if (activeWasEmpty) item.id else current.activePlaylistItemId,
            videoId = if (activeWasEmpty) item.videoId else current.videoId,
            status = if (activeWasEmpty) WatchPartyPlaybackStatus.UNSTARTED else current.status
        )
        playlists.update { it + (roomId to WatchPartyPlaylistLogic.ordered(currentPlaylist + item)) }
        watchPartyStates.update { it + (roomId to nextState) }
        return LiveOperationResult.Success(Unit)
    }

    override suspend fun removePlaylistItem(
        roomId: String,
        hostUid: String,
        itemId: String
    ): LiveOperationResult<Unit> {
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        if (room.hostUserId != hostUid) {
            return LiveOperationResult.Error("Only the host can edit the playlist.", isAuthError = true)
        }
        val currentPlaylist = playlists.value[roomId].orEmpty()
        val current = watchPartyStates.value[roomId]
            ?: return LiveOperationResult.Error("Watch Party is not enabled for this room.")
        return when (val mutation = WatchPartyPlaylistLogic.removeItem(
            current = current,
            playlist = currentPlaylist,
            itemId = itemId,
            actorUid = hostUid,
            nowMillis = clock()
        )) {
            is PlaylistMutation.Success -> {
                playlists.update {
                    it + (roomId to currentPlaylist.filterNot { entry -> entry.id == itemId })
                }
                watchPartyStates.update { it + (roomId to mutation.state) }
                LiveOperationResult.Success(Unit)
            }

            is PlaylistMutation.Duplicate ->
                LiveOperationResult.Error("Unexpected duplicate playlist entry.", isConflict = true)

            is PlaylistMutation.Rejected ->
                LiveOperationResult.Error(mutation.reason, isConflict = true)
        }
    }

    override fun observeMessages(roomId: String, limit: Int): Flow<List<ChatMessage>> =
        messages.map { snapshot ->
            val list = snapshot[roomId].orEmpty().sortedWith(compareBy({ it.timestamp }, { it.id }))
            if (list.size <= limit) list else list.takeLast(limit)
        }

    override suspend fun sendMessage(
        roomId: String,
        identity: LiveUserIdentity,
        text: String,
        clientMessageId: String
    ): LiveOperationResult<ChatMessage> {
        if (identity.uid.isBlank()) {
            return LiveOperationResult.Error("Sign in to chat.", isAuthError = true)
        }
        val room = rooms.value[roomId]
            ?: return LiveOperationResult.Error("This live room no longer exists.")
        val member = members.value[roomId]?.get(identity.uid)
            ?: return LiveOperationResult.Error("Join the room before chatting.", isAuthError = true)
        if (!room.isLive) return LiveOperationResult.Error("This live room has ended.")
        if (!room.isChatEnabled) return LiveOperationResult.Error("Chat is disabled in this room.")

        val existing = messages.value[roomId].orEmpty().firstOrNull { it.clientMessageId == clientMessageId }
        if (existing != null) return LiveOperationResult.Success(existing)

        val validation = LiveChatPolicy.validateSend(
            rawText = text,
            isRoomLive = room.isLive,
            isChatEnabled = room.isChatEnabled,
            isMember = true,
            isBlocked = false
        )
        val validText = when (validation) {
            is ChatValidation.Valid -> validation.text
            is ChatValidation.Invalid -> return LiveOperationResult.Error(validation.message)
        }
        // Identity fields come from the persisted membership, never from the caller's payload.
        val message = ChatMessage(
            id = "msg_" + idGenerator(),
            roomId = roomId,
            senderId = member.uid,
            senderName = member.displayName,
            senderAvatar = member.photoUrl,
            senderUsername = member.username,
            message = validText,
            timestamp = clock(),
            clientMessageId = clientMessageId
        )
        messages.update { it + (roomId to (it[roomId].orEmpty() + message)) }
        return LiveOperationResult.Success(message)
    }

    override suspend fun loadOlderMessages(
        roomId: String,
        beforeTimestampMillis: Long,
        limit: Int
    ): LiveOperationResult<List<ChatMessage>> {
        val older = messages.value[roomId].orEmpty()
            .filter { it.timestamp < beforeTimestampMillis }
            .sortedWith(compareBy({ it.timestamp }, { it.id }))
        return LiveOperationResult.Success(if (older.size <= limit) older else older.takeLast(limit))
    }
}
