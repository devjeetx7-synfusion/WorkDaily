package com.synfusion.attendance.live.data

import com.synfusion.attendance.live.model.ChatMessage
import com.synfusion.attendance.live.model.LiveOperationResult
import com.synfusion.attendance.live.model.LivePlaybackCommand
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.model.LiveRoomMember
import com.synfusion.attendance.live.model.LiveRoomSessionState
import com.synfusion.attendance.live.model.LiveUserIdentity
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState
import com.synfusion.attendance.live.watchparty.ChatValidation
import com.synfusion.attendance.live.watchparty.LiveChatPolicy
import com.synfusion.attendance.live.watchparty.LiveShareLink
import com.synfusion.attendance.live.watchparty.PlaylistMutation
import com.synfusion.attendance.live.watchparty.PresencePolicy
import com.synfusion.attendance.live.watchparty.WatchPartyPlaylistLogic
import com.synfusion.attendance.live.watchparty.WatchPartySyncPolicy
import com.synfusion.attendance.live.watchparty.YouTubeLinkParser
import com.synfusion.attendance.live.watchparty.YouTubeParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** Transient, user-facing message produced by a room session (send failures, conflicts...). */
data class LiveSessionNotice(
    val id: Long = System.nanoTime(),
    val message: String,
    val isError: Boolean = false
)

data class LiveRoomResolution(
    val roomId: String,
    val room: LiveRoom?,
    val isActive: Boolean,
    val message: String? = null
)

/**
 * Orchestrates a single Live room: membership/heartbeat presence, realtime chat, shared Watch
 * Party playback and playlist operations.
 *
 * The session is backend agnostic ([LiveRoomBackend]) so the exact behavior is covered by JVM
 * tests with [InMemoryLiveRoomBackend], while production uses the Firestore implementation.
 */
class LiveRoomSession(
    val roomId: String,
    private val backend: LiveRoomBackend,
    private val identity: LiveUserIdentity?,
    private val scope: CoroutineScope,
    private val cleanupScope: CoroutineScope = scope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val metadataProvider: VideoMetadataProvider = NoOpVideoMetadataProvider,
    private val heartbeatIntervalMillis: Long = WatchPartySyncPolicy.PRESENCE_HEARTBEAT_INTERVAL_MILLIS,
    private val presenceTickMillis: Long = 10_000L,
    private val historyLimit: Int = LiveChatPolicy.HISTORY_PAGE_SIZE
) {
    private val _state = MutableStateFlow<LiveRoomSessionState>(LiveRoomSessionState.Idle)
    val state: StateFlow<LiveRoomSessionState> = _state.asStateFlow()

    private val _members = MutableStateFlow<List<LiveRoomMember>>(emptyList())
    val members: StateFlow<List<LiveRoomMember>> = _members.asStateFlow()

    private val _watchParty = MutableStateFlow<WatchPartyState?>(null)
    val watchParty: StateFlow<WatchPartyState?> = _watchParty.asStateFlow()

    private val _playlist = MutableStateFlow<List<WatchPartyPlaylistItem>>(emptyList())
    val playlist: StateFlow<List<WatchPartyPlaylistItem>> = _playlist.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val persistedMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val pendingMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    private val _notice = MutableStateFlow<LiveSessionNotice?>(null)
    val notice: StateFlow<LiveSessionNotice?> = _notice.asStateFlow()

    private val _isMember = MutableStateFlow(false)
    val isMember: StateFlow<Boolean> = _isMember.asStateFlow()

    private val _isChatLoading = MutableStateFlow(true)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isPlaylistLoading = MutableStateFlow(true)
    val isPlaylistLoading: StateFlow<Boolean> = _isPlaylistLoading.asStateFlow()

    private val _hasMoreHistory = MutableStateFlow(false)
    val hasMoreHistory: StateFlow<Boolean> = _hasMoreHistory.asStateFlow()

    private val pendingPlaylistAdds = mutableSetOf<String>()

    /**
     * Serializes host playback commands. Without this, two quick actions (e.g. play then turn off)
     * could both read the same revision and the second write would be rejected as a conflict.
     */
    private val hostCommandMutex = Mutex()
    private val endRoomMutex = Mutex()
    /**
     * Set as soon as [stop] runs. Guards the heartbeat coroutine so an in-flight heartbeat that
     * finishes after the leave write cannot re-create the presence document.
     */
    @Volatile
    private var closed = false

    private var room: LiveRoom? = null
    private var sessionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var historyCursor: Long? = null

    val currentUid: String? get() = identity?.uid

    val currentRoom: LiveRoom? get() = room

    fun isHost(): Boolean = room?.hostUserId != null && room?.hostUserId == identity?.uid

    /** Starts loading/observing the room. Safe to call repeatedly (idempotent). */
    fun start() {
        if (sessionJob?.isActive == true) return
        closed = false
        sessionJob = scope.launch {
            val loaded = loadInitial()
            if (!loaded) return@launch

            launch { backend.observeRoom(roomId).collect { onRoomSnapshot(it) } }
            launch {
                backend.observeMembers(roomId).collect { incoming ->
                    _members.value = PresencePolicy.merge(_members.value, incoming)
                    publishActiveState()
                }
            }
            launch {
                backend.observeWatchParty(roomId).collect { state ->
                    if (state != null) _watchParty.value = state
                }
            }
            launch {
                backend.observePlaylist(roomId).collect { items ->
                    _playlist.value = WatchPartyPlaylistLogic.ordered(items)
                    _isPlaylistLoading.value = false
                }
            }
            launch {
                backend.observeMessages(roomId, historyLimit).collect { incoming ->
                    persistedMessages.value = incoming
                    _isChatLoading.value = false
                    rebuildMessages()
                }
            }
            launch {
                while (isActive) {
                    delay(presenceTickMillis)
                    publishActiveState()
                }
            }
            heartbeatLoop()
        }
    }

    /** Leaves the room (membership cleanup) and stops every listener. */
    fun stop() {
        closed = true
        sessionJob?.cancel()
        sessionJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        val uid = identity?.uid
        if (!uid.isNullOrBlank()) {
            cleanupScope.launch {
                runCatching { backend.leaveRoom(roomId, uid) }
            }
        }
        _state.value = LiveRoomSessionState.Idle
    }

    /** Retries loading after a transient error. */
    fun retry() {
        stop()
        start()
    }

    private suspend fun loadInitial(): Boolean {
        _state.value = LiveRoomSessionState.Loading
        return when (val result = backend.loadRoom(roomId)) {
            is LiveOperationResult.Error -> {
                _state.value = LiveRoomSessionState.Error(result.message)
                false
            }

            is LiveOperationResult.Success -> {
                val loadedRoom = result.data
                if (loadedRoom == null) {
                    _state.value = LiveRoomSessionState.Unavailable(
                        roomId,
                        "This live room does not exist or was removed."
                    )
                    return false
                }
                room = loadedRoom
                if (!loadedRoom.isLive || loadedRoom.endedAtMillis != null) {
                    _state.value = LiveRoomSessionState.Ended(loadedRoom)
                    return false
                }
                loadedRoom.watchPartyState?.let { _watchParty.value = it }

                val currentIdentity = identity
                if (currentIdentity != null) {
                    val joinResult = backend.joinRoom(roomId, currentIdentity)
                    when (joinResult) {
                        is LiveOperationResult.Success -> _isMember.value = true
                        is LiveOperationResult.Error -> {
                            if (joinResult.isAuthError) {
                                _state.value = LiveRoomSessionState.Error(joinResult.message)
                                return false
                            }
                            emitNotice(joinResult.message, isError = true)
                        }
                    }
                }
                publishActiveState()
                true
            }
        }
    }

    private fun onRoomSnapshot(snapshot: LiveRoom?) {
        // An ended session is terminal until explicitly reopened; ignore stale live snapshots.
        if (closed || _state.value is LiveRoomSessionState.Ended) return
        if (snapshot == null) {
            if (_state.value is LiveRoomSessionState.Active) {
                _state.value = LiveRoomSessionState.Unavailable(
                    roomId,
                    "This live room is no longer available."
                )
            }
            return
        }
        room = snapshot
        if (!snapshot.isLive || snapshot.endedAtMillis != null) {
            _state.value = LiveRoomSessionState.Ended(snapshot)
            heartbeatJob?.cancel()
            return
        }
        publishActiveState()
    }

    private fun publishActiveState() {
        if (closed || _state.value is LiveRoomSessionState.Ended) return
        val currentRoom = room ?: return
        if (!currentRoom.isLive || currentRoom.endedAtMillis != null) {
            _state.value = LiveRoomSessionState.Ended(currentRoom)
            return
        }
        val now = clock()
        val active = PresencePolicy.activeMembers(_members.value, now)
        _state.value = LiveRoomSessionState.Active(
            room = currentRoom,
            isHost = currentRoom.hostUserId == identity?.uid,
            viewerCount = active.count { !it.isHost },
            totalActiveCount = active.size
        )
    }

    private suspend fun heartbeatLoop() {
        val currentIdentity = identity ?: return
        var lastHeartbeat = 0L
        heartbeatJob = scope.launch {
            while (isActive && !closed) {
                val now = clock()
                if (PresencePolicy.isHeartbeatDue(lastHeartbeat, now)) {
                    if (_isMember.value) {
                        val result = backend.heartbeat(roomId, currentIdentity.uid)
                        if (result is LiveOperationResult.Error && !closed) {
                            _isMember.value = false
                            ensureMembership()
                        }
                    } else {
                        ensureMembership()
                    }
                    lastHeartbeat = now
                    pruneStaleMembersIfHost(now)
                }
                delay(heartbeatIntervalMillis.coerceAtLeast(1_000L))
            }
        }
    }

    private suspend fun ensureMembership() {
        if (closed) return
        val currentIdentity = identity ?: return
        when (val joinResult = backend.joinRoom(roomId, currentIdentity)) {
            is LiveOperationResult.Success -> _isMember.value = true
            is LiveOperationResult.Error -> {
                _isMember.value = false
                if (joinResult.isAuthError) emitNotice(joinResult.message, isError = true)
            }
        }
    }

    /** The host prunes memberships whose heartbeat expired (crashed/offline devices). */
    private fun pruneStaleMembersIfHost(nowMillis: Long) {
        if (!isHost()) return
        val stale = PresencePolicy.staleMemberUids(_members.value, nowMillis)
        if (stale.isEmpty()) return
        scope.launch {
            stale.forEach { uid ->
                runCatching { backend.removeMember(roomId, identity?.uid.orEmpty(), uid) }
            }
            _members.value = _members.value.filterNot { stale.contains(it.uid) }
            publishActiveState()
        }
    }

    // ---------------------------------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------------------------------

    private fun rebuildMessages() {
        _messages.value = LiveChatPolicy.mergeMessages(
            persisted = persistedMessages.value,
            pending = pendingMessages.value
        )
        _hasMoreHistory.value = persistedMessages.value.isNotEmpty() &&
            persistedMessages.value.size >= historyLimit
        historyCursor = LiveChatPolicy.olderHistoryCursor(persistedMessages.value)
    }

    fun sendSystemEvent(message: ChatMessage) {
        scope.launch {
            backend.sendSystemEvent(roomId, message)
        }
    }

    fun sendMessage(rawText: String, isMuted: Boolean = false) {
        val currentIdentity = identity
        if (currentIdentity == null) {
            emitNotice("Sign in to send messages.", isError = true)
            return
        }
        val currentRoom = room
        if (currentRoom == null) {
            emitNotice("Room is still loading.", isError = true)
            return
        }
        val validation = LiveChatPolicy.validateSend(
            rawText = rawText,
            isRoomLive = currentRoom.isLive,
            isChatEnabled = currentRoom.isChatEnabled,
            isMember = _isMember.value,
            isBlocked = false,
            isMuted = isMuted
        )
        val text = when (validation) {
            is ChatValidation.Valid -> validation.text
            is ChatValidation.Invalid -> {
                emitNotice(validation.message, isError = true)
                return
            }
        }
        val clientMessageId = "c_" + UUID.randomUUID().toString().replace("-", "").take(24)
        val optimistic = ChatMessage(
            id = clientMessageId,
            roomId = roomId,
            senderId = currentIdentity.uid,
            senderName = currentIdentity.displayName,
            senderAvatar = currentIdentity.photoUrl,
            senderUsername = currentIdentity.username,
            message = text,
            timestamp = clock(),
            clientMessageId = clientMessageId,
            isPending = true
        )
        pendingMessages.update { it + optimistic }
        rebuildMessages()
        dispatchSend(optimistic)
    }

    /** Re-sends a failed message. The same client id is reused so the backend stays idempotent. */
    fun retryMessage(clientMessageId: String) {
        val failed = pendingMessages.value.firstOrNull { it.clientMessageId == clientMessageId } ?: return
        pendingMessages.update { list ->
            list.map { if (it.clientMessageId == clientMessageId) it.copy(isFailed = false, isPending = true) else it }
        }
        rebuildMessages()
        dispatchSend(failed.copy(isFailed = false, isPending = true))
    }

    private fun dispatchSend(message: ChatMessage) {
        val currentIdentity = identity ?: return
        val clientMessageId = message.clientMessageId ?: return
        scope.launch {
            when (val result = backend.sendMessage(roomId, currentIdentity, message.message, clientMessageId)) {
                is LiveOperationResult.Success -> {
                    pendingMessages.update { LiveChatPolicy.markSent(it, clientMessageId) }
                    // Show the acknowledged message immediately; the realtime snapshot replaces it.
                    val acknowledged = result.data
                    if (persistedMessages.value.none { it.id == acknowledged.id }) {
                        persistedMessages.update { existing ->
                            (existing + acknowledged).sortedWith(compareBy({ it.timestamp }, { it.id }))
                        }
                    }
                    rebuildMessages()
                }

                is LiveOperationResult.Error -> {
                    pendingMessages.update { LiveChatPolicy.markFailed(it, clientMessageId) }
                    rebuildMessages()
                    emitNotice(result.message, isError = true)
                }
            }
        }
    }

    fun loadOlderMessages() {
        val cursor = historyCursor ?: return
        scope.launch {
            when (val result = backend.loadOlderMessages(roomId, cursor, historyLimit)) {
                is LiveOperationResult.Success -> {
                    val older = result.data
                    persistedMessages.update { existing ->
                        val known = existing.map { it.id }.toSet()
                        (older.filterNot { known.contains(it.id) } + existing)
                            .sortedWith(compareBy({ it.timestamp }, { it.id }))
                    }
                    _hasMoreHistory.value = older.size >= historyLimit
                    rebuildMessages()
                }

                is LiveOperationResult.Error -> emitNotice(result.message, isError = true)
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Watch Party playback (host only)
    // ---------------------------------------------------------------------------------------

    private fun publishHostCommand(command: LivePlaybackCommand, optimistic: Boolean = true) {
        val currentIdentity = identity
        val currentRoom = room
        if (currentIdentity == null || currentRoom == null || _watchParty.value == null) {
            emitNotice("Watch Party state is not available yet.", isError = true)
            return
        }
        if (!WatchPartySyncPolicy.isAuthorizedHost(currentIdentity.uid, currentRoom.hostUserId)) {
            emitNotice("Only the host can control Watch Party playback.", isError = true)
            return
        }
        if (!WatchPartySyncPolicy.canAcceptPlaybackCommands(currentRoom.isLive, currentRoom.endedAtMillis)) {
            emitNotice("This live room has ended.", isError = true)
            return
        }
        scope.launch {
            hostCommandMutex.withLock {
                val currentState = _watchParty.value ?: return@withLock
                val expectedRevision = currentState.revision
                if (optimistic) {
                    _watchParty.value = WatchPartySyncPolicy.applyCommand(
                        current = currentState,
                        command = command,
                        actorUid = currentIdentity.uid,
                        nowMillis = clock(),
                        commandId = "local-" + UUID.randomUUID().toString().take(8)
                    )
                }
                when (val result = backend.applyPlaybackCommand(
                    roomId = roomId,
                    hostUid = currentIdentity.uid,
                    expectedRevision = expectedRevision,
                    command = command
                )) {
                    is LiveOperationResult.Success -> _watchParty.value = result.data
                    is LiveOperationResult.Error -> {
                        // Roll the optimistic state back so the UI never shows unpersisted playback.
                        _watchParty.value = currentState
                        if (result.isConflict) {
                            emitNotice("Playback was updated on another host device. Syncing…")
                        } else {
                            emitNotice(result.message, isError = true)
                        }
                    }
                }
            }
        }
    }

    fun hostPlay(positionSeconds: Float) =
        publishHostCommand(LivePlaybackCommand.Play(positionSeconds.coerceAtLeast(0f)))

    fun hostPause(positionSeconds: Float) =
        publishHostCommand(LivePlaybackCommand.Pause(positionSeconds.coerceAtLeast(0f)))

    fun hostSeek(positionSeconds: Float, resumePlaying: Boolean) =
        publishHostCommand(LivePlaybackCommand.Seek(positionSeconds.coerceAtLeast(0f), resumePlaying))

    fun hostSetMuted(muted: Boolean) = publishHostCommand(LivePlaybackCommand.SetMuted(muted))

    fun hostTurnOff() = publishHostCommand(LivePlaybackCommand.TurnOff)

    fun hostTurnOn() = publishHostCommand(LivePlaybackCommand.TurnOn)

    fun hostSelectItem(item: WatchPartyPlaylistItem) {
        if (item.videoId.isBlank()) {
            emitNotice("Only single videos can be played.", isError = true)
            return
        }
        publishHostCommand(
            LivePlaybackCommand.SelectItem(
                itemId = item.id,
                videoId = item.videoId,
                videoUrl = item.videoUrl
            )
        )
    }

    /** Called by the host player when a video ends; advances to the next playlist item. */
    fun hostNotifyCompleted() {
        val currentState = _watchParty.value ?: return
        val next = WatchPartyPlaylistLogic.nextItem(_playlist.value, currentState.activePlaylistItemId).item
        publishHostCommand(
            LivePlaybackCommand.Completed(
                nextItemId = next?.id,
                nextVideoId = next?.videoId,
                nextVideoUrl = next?.videoUrl
            )
        )
    }

    /**
     * Periodic drift correction: the host republishes its playhead (bumping the revision) so late
     * joiners and participants stay close without a high-frequency position stream.
     */
    fun hostPublishPositionCorrection(positionSeconds: Float) {
        val currentState = _watchParty.value ?: return
        if (!isHost()) return
        if (currentState.status != com.synfusion.attendance.live.model.WatchPartyPlaybackStatus.PLAYING) return
        publishHostCommand(LivePlaybackCommand.Play(positionSeconds.coerceAtLeast(0f)))
    }

    fun addPlaylistVideo(rawUrl: String) {
        val currentIdentity = identity
        val currentRoom = room
        val currentState = _watchParty.value
        if (currentIdentity == null || currentRoom == null || currentState == null) {
            emitNotice("Playlist is not ready yet.", isError = true)
            return
        }
        if (!WatchPartySyncPolicy.isAuthorizedHost(currentIdentity.uid, currentRoom.hostUserId)) {
            emitNotice("Only the host can edit the playlist.", isError = true)
            return
        }
        if (!currentRoom.isLive) {
            emitNotice("This live room has ended.", isError = true)
            return
        }
        val selection = when (val parsed = YouTubeLinkParser.parse(rawUrl)) {
            is YouTubeParseResult.Success -> parsed.selection
            is YouTubeParseResult.Failure -> {
                emitNotice(parsed.message, isError = true)
                return
            }
        }
        if (!selection.isVideoPlayable) {
            emitNotice(
                "Playlist links can be set as the room's playlist, but the Watch Party queue accepts single videos.",
                isError = true
            )
            return
        }
        val videoId = selection.videoId ?: return
        if (_playlist.value.any { it.videoId == videoId } || pendingPlaylistAdds.contains(videoId)) {
            emitNotice("That video is already in the playlist.", isError = true)
            return
        }
        pendingPlaylistAdds.add(videoId)
        scope.launch {
            // Real public metadata (title/thumbnail) when YouTube's oEmbed endpoint answers; the
            // derived title is a truthful fallback, never an invented one.
            val metadata = runCatching { metadataProvider.fetch(videoId) }.getOrNull()
            val item = WatchPartyPlaylistItem(
                id = videoId,
                videoId = videoId,
                videoUrl = selection.canonicalUrl,
                title = metadata?.title ?: "YouTube Video ($videoId)",
                thumbnailUrl = metadata?.thumbnailUrl ?: YouTubeLinkParser.thumbnailUrl(videoId),
                addedByUserId = currentIdentity.uid,
                addedByUserName = currentIdentity.displayName,
                addedAtMillis = clock()
            )
            when (val result = backend.addPlaylistItem(roomId, currentIdentity.uid, item)) {
                is LiveOperationResult.Success -> {
                    // The playlist observer is the single source of truth; appending locally here
                    // would duplicate the row when the snapshot arrives.
                    emitNotice("Added to the Watch Party playlist.")
                }

                is LiveOperationResult.Error -> emitNotice(result.message, isError = true)
            }
            pendingPlaylistAdds.remove(videoId)
        }
    }

    fun removePlaylistVideo(itemId: String) {
        val currentIdentity = identity ?: return
        if (!isHost()) {
            emitNotice("Only the host can edit the playlist.", isError = true)
            return
        }
        scope.launch {
            when (val result = backend.removePlaylistItem(roomId, currentIdentity.uid, itemId)) {
                is LiveOperationResult.Success -> emitNotice("Removed from the playlist.")
                is LiveOperationResult.Error -> emitNotice(result.message, isError = true)
            }
        }
    }

    fun selectPlaylistVideo(itemId: String) {
        val currentState = _watchParty.value ?: return
        if (!isHost()) {
            emitNotice("Only the host can change the playing video.", isError = true)
            return
        }
        when (val mutation = WatchPartyPlaylistLogic.selectItem(
            current = currentState,
            playlist = _playlist.value,
            itemId = itemId,
            actorUid = identity?.uid.orEmpty(),
            nowMillis = clock()
        )) {
            is PlaylistMutation.Success -> publishHostCommand(
                LivePlaybackCommand.SelectItem(
                    itemId = itemId,
                    videoId = mutation.item?.videoId.orEmpty(),
                    videoUrl = mutation.item?.videoUrl.orEmpty()
                )
            )

            is PlaylistMutation.Rejected -> emitNotice(mutation.reason, isError = true)
            is PlaylistMutation.Duplicate -> emitNotice("That item already exists.", isError = true)
        }
    }

    /** Host ends the Live room entirely (not just the Watch Party playback). */
    suspend fun endRoom(): LiveOperationResult<Unit> {
        val currentIdentity = identity ?: return LiveOperationResult.Error("Sign in to end this live room.", isAuthError = true)
        if (!isHost()) {
            val err = "Only the host can end this live room."
            emitNotice(err, isError = true)
            return LiveOperationResult.Error(err, isAuthError = true)
        }
        if (!endRoomMutex.tryLock()) return LiveOperationResult.Error("Ending this room is already in progress.")
        try {
            val result = backend.endRoom(roomId, currentIdentity.uid)
            when (result) {
                is LiveOperationResult.Success -> {
                    val endedRoom = room?.copy(isLive = false, endedAtMillis = clock()) ?: LiveRoom(
                        id = roomId,
                        title = room?.title.orEmpty(),
                        creatorId = currentIdentity.uid,
                        creatorName = currentIdentity.displayName,
                        isLive = false,
                        endedAtMillis = clock()
                    )
                    room = endedRoom
                    _state.value = LiveRoomSessionState.Ended(endedRoom)
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                }
                is LiveOperationResult.Error -> emitNotice(result.message, isError = true)
            }
            return result
        } finally {
            endRoomMutex.unlock()
        }
    }

    /** Removes another participant; only the host is authorized (mirrored by backend rules). */
    fun removeParticipant(memberUid: String) {
        val currentIdentity = identity ?: return
        val currentRoom = room ?: return
        if (!PresencePolicy.canModerate(currentIdentity.uid, currentRoom.hostUserId, memberUid)) {
            emitNotice("Only the host can remove participants.", isError = true)
            return
        }
        scope.launch {
            when (val result = backend.removeMember(roomId, currentIdentity.uid, memberUid)) {
                is LiveOperationResult.Success -> {
                    _members.value = _members.value.filterNot { it.uid == memberUid }
                    publishActiveState()
                    emitNotice("Participant removed from the room.")
                }

                is LiveOperationResult.Error -> emitNotice(result.message, isError = true)
            }
        }
    }

    fun consumeNotice(noticeId: Long) {
        if (_notice.value?.id == noticeId) _notice.value = null
    }

    fun emitNotice(message: String, isError: Boolean = false) {
        _notice.value = LiveSessionNotice(message = message, isError = isError)
    }

    companion object {
        /** Builds the canonical share link for a room. */
        fun shareLink(roomId: String): String = LiveShareLink.buildShareUrl(roomId)

        /**
         * Resolves a shared/deep link to a room, used by the share sheet and intent handler.
         * Ended or missing rooms are reported explicitly so the UI can render "Live Ended".
         */
        suspend fun resolveLink(backend: LiveRoomBackend, roomId: String): LiveRoomResolution {
            if (!LiveShareLink.isValidRoomId(roomId)) {
                return LiveRoomResolution(roomId, null, false, "That link is not a WorkDaily Live link.")
            }
            return when (val result = backend.loadRoom(roomId)) {
                is LiveOperationResult.Success -> {
                    val room = result.data
                    when {
                        room == null -> LiveRoomResolution(
                            roomId,
                            null,
                            false,
                            "This live room does not exist or was removed."
                        )

                        !room.isLive || room.endedAtMillis != null -> LiveRoomResolution(
                            roomId,
                            room,
                            false,
                            "This live room has ended."
                        )

                        else -> LiveRoomResolution(roomId, room, true)
                    }
                }

                is LiveOperationResult.Error -> LiveRoomResolution(roomId, null, false, result.message)
            }
        }
    }
}
