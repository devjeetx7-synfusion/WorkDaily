package com.synfusion.attendance.live.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.synfusion.attendance.live.data.DemoLiveRepository
import com.synfusion.attendance.live.data.LiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
import com.synfusion.attendance.live.data.LiveRoomSession
import com.synfusion.attendance.live.data.LiveSessionNotice
import com.synfusion.attendance.live.media.MediaConnection
import com.synfusion.attendance.live.model.*
import com.synfusion.attendance.online.FirestoreLiveRoomBackend
import com.synfusion.attendance.online.ProfileOperationResult
import com.synfusion.attendance.online.PublicProfile
import com.synfusion.attendance.online.SocialSessionCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

enum class HostStartupStep {
    IDLE,
    REQUESTING_PERMISSIONS,
    RESERVING,
    CONNECTING,
    PUBLISHING,
    ACTIVATING,
    LIVE,
    ENDING,
    ENDED,
    FAILED
}

data class HostStartupStatus(
    val step: HostStartupStep = HostStartupStep.IDLE,
    val roomId: String? = null,
    val errorMessage: String? = null
)

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DemoLiveRepository.getInstance(application)

    // ---------------------------------------------------------------------------------------
    // Production Live room layer (Firestore-backed; demo seeds remain for offline discovery)
    // ---------------------------------------------------------------------------------------
    private val backend: LiveRoomBackend = FirestoreLiveRoomBackend()

    /** Outlives the ViewModel so "leave room" cleanup still runs after the screen is destroyed. */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Created only when the future media flow explicitly requests it; no capture begins here.
    private val mediaManagerDelegate = lazy {
        com.synfusion.attendance.live.media.LiveKitMediaManager(application, viewModelScope)
    }
    val mediaManager get() = mediaManagerDelegate.value

    /** Resolves real playlist titles/thumbnails from YouTube's public oEmbed endpoint. */
    private val metadataProvider = com.synfusion.attendance.online.YouTubeOEmbedMetadataProvider()

    private var sessionOpenJob: Job? = null
    private val endLiveMutex = Mutex()

    private val _hostStartupStatus = MutableStateFlow(HostStartupStatus())
    val hostStartupStatus: StateFlow<HostStartupStatus> = _hostStartupStatus.asStateFlow()

    private var hostStartupGeneration: Long = 0L

    private val _activeSession = MutableStateFlow<LiveRoomSession?>(null)
    val activeSession: StateFlow<LiveRoomSession?> = _activeSession.asStateFlow()

    private val _liveIdentity = MutableStateFlow<LiveUserIdentity?>(null)
    val liveIdentity: StateFlow<LiveUserIdentity?> = _liveIdentity.asStateFlow()

    private val _liveHints = MutableStateFlow<LiveSessionNotice?>(null)

    /** Transient room error/notice surfaced as a snackbar in the Live UI. */
    val liveHint: StateFlow<LiveSessionNotice?> = _liveHints.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        refreshLiveIdentity()
        try {
            val auth = FirebaseAuth.getInstance()
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val uid = firebaseAuth.currentUser?.uid
                SocialSessionCache.onAuthenticatedUserChanged(uid)
                refreshLiveIdentity()
            }
            authListener = listener
            auth.addAuthStateListener(listener)
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase Auth unavailable for Live:", t)
        }
    }

    override fun onCleared() {
        authListener?.let { listener ->
            runCatching { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
        }
        authListener = null
        closeLiveRoom(leaveMembership = true)
        if (mediaManagerDelegate.isInitialized()) {
            cleanupScope.launch(Dispatchers.Main.immediate) { mediaManagerDelegate.value.disconnect() }
        }
        super.onCleared()
    }

    /** Realtime active rooms stream from Firestore backend. */
    val backendActiveRooms: StateFlow<List<LiveRoom>> = backend.observeActiveRooms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Production active Live rooms stream from Firestore backend.
     * Demo rooms are isolated and not mixed into production discovery.
     */
    val liveRooms: StateFlow<List<LiveRoom>> = backendActiveRooms

    /** Demo room fixtures preserved strictly for designated offline/preview test paths. */
    val demoRooms: StateFlow<List<LiveRoom>> = repository.liveRooms

    /**
     * Creators list with real-time active room state updated from real backend streams.
     */
    val creators: StateFlow<List<LiveCreator>> = kotlinx.coroutines.flow.combine(
        repository.creators,
        backendActiveRooms
    ) { currentCreators, realRooms ->
        currentCreators.map { creator ->
            val realRoom = realRooms.find { it.creatorId == creator.id || it.hostUserId == creator.id }
            if (realRoom != null) {
                creator.copy(isLiveNow = true, activeRoomId = realRoom.id)
            } else {
                creator.copy(isLiveNow = false, activeRoomId = null)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.creators.value)
    val gifts: StateFlow<List<LiveGift>> = repository.gifts
    val wallet: StateFlow<DemoWallet> = repository.wallet
    val creatorEarning: StateFlow<DemoCreatorEarning> = repository.creatorEarning
    val activeRoom: StateFlow<LiveRoom?> = repository.activeRoom
    val chatMessagesMap: StateFlow<Map<String, List<ChatMessage>>> = repository.chatMessagesMap
    val giftTransactions: StateFlow<List<GiftTransaction>> = repository.giftTransactions
    val followedCreatorIds: StateFlow<Set<String>> = repository.followedCreatorIds
    val guestRequestsMap: StateFlow<Map<String, List<GuestRequest>>> = repository.guestRequestsMap
    val audioSeatsMap: StateFlow<Map<String, List<AudioSeat>>> = repository.audioSeatsMap
    val roomParticipantsMap: StateFlow<Map<String, List<RoomParticipant>>> = repository.roomParticipantsMap
    val notifications: StateFlow<List<DemoNotification>> = repository.notifications
    val liveGoals: StateFlow<List<LiveGoal>> = repository.liveGoals
    val giftOverlayEvents: StateFlow<List<LiveGiftOverlayEvent>> = repository.giftOverlayEvents
    val lastEndSummary: StateFlow<LiveEndSummary?> = repository.lastEndSummary
    val blockedUserIds: StateFlow<Set<String>> = repository.blockedUserIds
    val mutedUserIdsMap: StateFlow<Map<String, Set<String>>> = repository.mutedUserIdsMap
    val soundEnabled: StateFlow<Boolean> = repository.soundEnabled
    val hapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabled

    // Social Flow Proxies
    val publicUsers: StateFlow<List<DemoPublicUser>> = repository.publicUsers
    val friendRequests: StateFlow<List<DemoFriendRequest>> = repository.friendRequests
    val friendships: StateFlow<List<DemoFriendship>> = repository.friendships
    val conversations: StateFlow<List<DemoConversation>> = repository.conversations
    val privateMessagesMap: StateFlow<Map<String, List<DemoPrivateMessage>>> = repository.privateMessagesMap
    val socialNotifications: StateFlow<List<DemoSocialNotification>> = repository.socialNotifications

    @OptIn(ExperimentalCoroutinesApi::class)
    val roomSessionState: StateFlow<LiveRoomSessionState> = _activeSession
        .flatMapLatest { it?.state ?: flowOf(LiveRoomSessionState.Idle) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveRoomSessionState.Idle)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionMessages: StateFlow<List<ChatMessage>> = _activeSession
        .flatMapLatest { it?.messages ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionMembers: StateFlow<List<LiveRoomMember>> = _activeSession
        .flatMapLatest { it?.members ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionWatchParty: StateFlow<WatchPartyState?> = _activeSession
        .flatMapLatest { it?.watchParty ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionPlaylist: StateFlow<List<WatchPartyPlaylistItem>> = _activeSession
        .flatMapLatest { it?.playlist ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionNotice: StateFlow<LiveSessionNotice?> = _activeSession
        .flatMapLatest { it?.notice ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionIsMember: StateFlow<Boolean> = _activeSession
        .flatMapLatest { it?.isMember ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionIsChatLoading: StateFlow<Boolean> = _activeSession
        .flatMapLatest { it?.isChatLoading ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionIsPlaylistLoading: StateFlow<Boolean> = _activeSession
        .flatMapLatest { it?.isPlaylistLoading ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionHasMoreHistory: StateFlow<Boolean> = _activeSession
        .flatMapLatest { it?.hasMoreHistory ?: flowOf(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Real follow relationships for the authenticated user (existing Firestore social graph). */
    val followingUids: StateFlow<Set<String>> = SocialSessionCache.manager.snapshot
        .map { snapshot -> snapshot.myFollowedUids?.value ?: emptySet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Target uids with an in-flight follow/unfollow mutation (prevents duplicate taps). */
    val followBusyUids: StateFlow<Set<String>> = SocialSessionCache.manager.snapshot
        .map { snapshot ->
            snapshot.activeMutations.mapNotNull { key ->
                key.split(":").takeIf { it.size == 3 && it[0] == "follow" }?.get(2)
            }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val cachedHostProfiles = mutableMapOf<String, PublicProfile>()

    // ---------------------------------------------------------------------------------------
    // Identity
    // ---------------------------------------------------------------------------------------

    fun refreshLiveIdentity() {
        viewModelScope.launch {
            val identity = resolveLiveIdentity()
            _liveIdentity.value = identity
            identity?.let { signedIn ->
                runCatching { SocialSessionCache.manager.onAuthenticatedUserChanged(signedIn.uid) }
            }
        }
    }

    /**
     * Resolves the authenticated Firebase account plus its persisted `public_profiles` document.
     * Returns null when no account is signed in or the profile cannot be loaded, in which case room
     * creation/join/chat are refused instead of fabricating an identity.
     */
    suspend fun resolveLiveIdentity(): LiveUserIdentity? {
        val firebaseUser = try {
            FirebaseAuth.getInstance().currentUser
        } catch (t: Throwable) {
            null
        } ?: return null

        // Shadowing the class-level `repository` (DemoLiveRepository) here was confusing.
        val profileRepository = SocialSessionCache.publicProfileRepository
        val cached = runCatching { profileRepository.getPublicProfile(firebaseUser.uid) }.getOrNull()
        val profile = (cached as? ProfileOperationResult.Success)?.data
            ?: (runCatching {
                profileRepository.ensureOrCreatePublicProfile(
                    firebaseUser.uid,
                    firebaseUser.displayName,
                    firebaseUser.photoUrl?.toString()
                )
            }.getOrNull() as? ProfileOperationResult.Success)?.data
            ?: return null

        if (profile.displayName.isBlank() && firebaseUser.displayName.isNullOrBlank()) {
            return null
        }

        val identity = LiveUserIdentity(
            uid = profile.uid,
            displayName = profile.displayName.ifBlank { firebaseUser.displayName ?: "WorkDaily Member" },
            username = profile.username,
            photoUrl = profile.photoUrl ?: firebaseUser.photoUrl?.toString().orEmpty(),
            profession = profile.profession,
            isVerified = profile.isVerified
        )
        _liveIdentity.value = identity
        return identity
    }

    /** Loads a public profile through the existing social cache (used by viewer/profile sheets). */
    suspend fun loadPublicProfile(uid: String): PublicProfile? {
        val viewerUid = _liveIdentity.value?.uid ?: return null
        cachedHostProfiles[uid]?.let { return it }
        return when (val result = SocialSessionCache.manager.getPublicProfile(viewerUid, uid)) {
            is ProfileOperationResult.Success -> result.data?.also { cachedHostProfiles[uid] = it }
            is ProfileOperationResult.Error -> null
        }
    }

    // ---------------------------------------------------------------------------------------
    // Room creation (real, persisted, authenticated host)
    // ---------------------------------------------------------------------------------------

    /** Creates and persists a Live room for the authenticated user. */
    suspend fun createLiveRoom(request: LiveRoomCreateRequest): LiveOperationResult<LiveRoom> {
        val identity = _liveIdentity.value ?: resolveLiveIdentity()
            ?: return LiveOperationResult.Error(
                "Sign in to start a live room. Your WorkDaily identity is required.",
                isAuthError = true
            )
        val result = backend.createRoom(identity, request)
        if (result is LiveOperationResult.Success) {
            repository.cacheLiveRoom(result.data)
            preloadSocialGraph()
        }
        return result
    }

    /**
     * Executes the lifecycle-aware host broadcast startup flow.
     */
    suspend fun startHostBroadcast(request: LiveRoomCreateRequest): LiveOperationResult<String> {
        val gen = ++hostStartupGeneration
        val identity = _liveIdentity.value ?: resolveLiveIdentity()
            ?: return LiveOperationResult.Error(
                "Sign in to start a live room. Your WorkDaily identity is required.",
                isAuthError = true
            )

        if (request.type == LiveRoomType.TEXT || request.type == LiveRoomType.YOUTUBE_WATCH_PARTY) {
            _hostStartupStatus.value = HostStartupStatus(HostStartupStep.RESERVING)
            val res = createLiveRoom(request)
            if (res is LiveOperationResult.Success) {
                if (gen != hostStartupGeneration) return LiveOperationResult.Error("Startup superseded.")
                openLiveRoom(res.data.id)
                _hostStartupStatus.value = HostStartupStatus(HostStartupStep.LIVE, roomId = res.data.id)
                return LiveOperationResult.Success(res.data.id)
            } else {
                val errMsg = (res as? LiveOperationResult.Error)?.message ?: "Failed to create room."
                _hostStartupStatus.value = HostStartupStatus(HostStartupStep.FAILED, errorMessage = errMsg)
                return LiveOperationResult.Error(errMsg)
            }
        }

        _hostStartupStatus.value = HostStartupStatus(HostStartupStep.RESERVING)
        var reservedRoomId: String? = null
        try {
            if (gen != hostStartupGeneration) return LiveOperationResult.Error("Startup superseded.")
            val reserveRes = backend.reserveMediaRoom(request)
            if (reserveRes is LiveOperationResult.Error) {
                _hostStartupStatus.value = HostStartupStatus(HostStartupStep.FAILED, errorMessage = reserveRes.message)
                return reserveRes
            }
            val roomId = (reserveRes as LiveOperationResult.Success).data
            reservedRoomId = roomId

            if (gen != hostStartupGeneration) {
                runCatching { backend.endRoom(roomId, identity.uid) }
                return LiveOperationResult.Error("Startup superseded.")
            }

            _hostStartupStatus.value = HostStartupStatus(HostStartupStep.CONNECTING, roomId = roomId)
            val publishVideo = (request.type == LiveRoomType.VIDEO)
            val publishAudio = (request.type == LiveRoomType.VIDEO || request.type == LiveRoomType.AUDIO)

            _hostStartupStatus.value = HostStartupStatus(HostStartupStep.PUBLISHING, roomId = roomId)
            mediaManager.connect(
                roomId = roomId,
                persistedHostUid = identity.uid,
                publishVideo = publishVideo,
                publishAudio = publishAudio
            )

            val mediaState = mediaManager.state.value
            check(mediaState.connection == MediaConnection.CONNECTED) { "LiveKit media connection lost." }
            if (publishVideo) check(mediaState.cameraEnabled) { "Camera publishing failed." }
            if (publishAudio) check(mediaState.microphoneEnabled) { "Microphone publishing failed." }

            if (gen != hostStartupGeneration) {
                runCatching { mediaManager.disconnect() }
                runCatching { backend.endRoom(roomId, identity.uid) }
                return LiveOperationResult.Error("Startup superseded.")
            }

            _hostStartupStatus.value = HostStartupStatus(HostStartupStep.ACTIVATING, roomId = roomId)
            val activateRes = backend.activateMediaRoom(roomId)
            if (activateRes is LiveOperationResult.Error) {
                runCatching { mediaManager.disconnect() }
                runCatching { backend.endRoom(roomId, identity.uid) }
                _hostStartupStatus.value = HostStartupStatus(HostStartupStep.FAILED, roomId = roomId, errorMessage = activateRes.message)
                return activateRes
            }

            if (gen != hostStartupGeneration) {
                runCatching { mediaManager.disconnect() }
                runCatching { backend.endRoom(roomId, identity.uid) }
                return LiveOperationResult.Error("Startup superseded.")
            }

            _hostStartupStatus.value = HostStartupStatus(HostStartupStep.LIVE, roomId = roomId)
            openLiveRoom(roomId)
            return LiveOperationResult.Success(roomId)
        } catch (e: Exception) {
            if (reservedRoomId != null) {
                runCatching { mediaManager.disconnect() }
                runCatching { backend.endRoom(reservedRoomId, identity.uid) }
            }
            val errMsg = e.message ?: "Failed to start live stream."
            _hostStartupStatus.value = HostStartupStatus(HostStartupStep.FAILED, roomId = reservedRoomId, errorMessage = errMsg)
            if (e is kotlinx.coroutines.CancellationException) throw e
            return LiveOperationResult.Error(errMsg)
        }
    }

    fun resetHostStartupStatus() {
        _hostStartupStatus.value = HostStartupStatus(HostStartupStep.IDLE)
    }

    fun ensureLiveRoomExists(roomId: String): LiveRoom? =
        liveRooms.value.firstOrNull { it.id == roomId }

    // ---------------------------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------------------------

    /**
     * Opens a room: joins membership, subscribes to presence/chat/playback. Must be called from the
     * Live feed for the page currently visible; [closeLiveRoom] leaves the membership again.
     */
    fun openLiveRoom(roomId: String) {
        if (roomId.isBlank()) return
        if (_activeSession.value?.roomId == roomId) return
        sessionOpenJob?.cancel()
        sessionOpenJob = viewModelScope.launch {
            // The Firebase session may still be restoring on first launch; resolve it before
            // joining so the membership document always carries the authenticated identity.
            if (_liveIdentity.value == null) resolveLiveIdentity()
            if (_activeSession.value?.roomId == roomId) return@launch
            _activeSession.value?.stop()
            val session = LiveRoomSession(
                roomId = roomId,
                backend = backend,
                identity = _liveIdentity.value,
                scope = viewModelScope,
                cleanupScope = cleanupScope,
                metadataProvider = metadataProvider
            )
            _activeSession.value = session
            session.start()
            preloadSocialGraph()
        }
    }

    /**
     * Closes the current room session. [leaveMembership] removes the presence document so the
     * viewer count drops immediately instead of waiting for the stale-heartbeat window.
     */
    fun closeLiveRoom(leaveMembership: Boolean = true) {
        val session = _activeSession.value ?: return
        if (leaveMembership) session.stop() else _activeSession.value = null
        _activeSession.value = null
    }

    fun retryActiveRoom() {
        _activeSession.value?.retry()
    }

    private fun preloadSocialGraph() {
        val uid = _liveIdentity.value?.uid ?: return
        viewModelScope.launch {
            runCatching { SocialSessionCache.manager.refreshOwnSocialOverview(uid) }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Watch Party (host-only commands; participants mirror the persisted state)
    // ---------------------------------------------------------------------------------------

    fun sendRoomMessage(roomId: String, text: String) {
        val session = _activeSession.value
        if (session != null && session.roomId == roomId) {
            session.sendMessage(text)
        } else {
            // Demo/offline room fallback: keeps existing local chat behavior without pretending
            // the message was persisted to the backend.
            sendChatMessage(roomId, text)
        }
    }

    fun retryRoomMessage(clientMessageId: String) {
        _activeSession.value?.retryMessage(clientMessageId)
    }

    fun loadOlderRoomMessages() {
        _activeSession.value?.loadOlderMessages()
    }

    fun hostPlay(positionSeconds: Float) {
        val session = _activeSession.value
        if (session != null) session.hostPlay(positionSeconds)
        else repository.updateWatchPartyState(activeRoomId(), WatchPartyPlaybackStatus.PLAYING, positionSeconds)
    }

    fun hostPause(positionSeconds: Float) {
        val session = _activeSession.value
        if (session != null) session.hostPause(positionSeconds)
        else repository.updateWatchPartyState(activeRoomId(), WatchPartyPlaybackStatus.PAUSED, positionSeconds)
    }

    fun hostSeek(positionSeconds: Float, resumePlaying: Boolean) {
        val session = _activeSession.value
        if (session != null) {
            session.hostSeek(positionSeconds, resumePlaying)
        } else {
            repository.updateWatchPartyState(
                activeRoomId(),
                if (resumePlaying) WatchPartyPlaybackStatus.PLAYING else WatchPartyPlaybackStatus.PAUSED,
                positionSeconds
            )
        }
    }

    fun hostSetMuted(muted: Boolean) {
        val session = _activeSession.value
        if (session != null) session.hostSetMuted(muted) else repository.toggleMuteWatchParty(activeRoomId())
    }

    fun hostTurnWatchPartyOff() {
        val session = _activeSession.value
        if (session != null) session.hostTurnOff() else repository.turnOffWatchParty(activeRoomId())
    }

    fun hostTurnWatchPartyOn() {
        val session = _activeSession.value
        if (session != null) {
            session.hostTurnOn()
        } else {
            // Demo rooms have no persisted state; re-select the current item locally.
            val room = liveRooms.value.find { it.id == activeRoomId() }
            val item = room?.watchPartyState?.playlist?.firstOrNull()
            if (item != null) repository.selectPlaylistItem(room.id, item.id)
        }
    }

    fun hostSelectPlaylistItem(item: WatchPartyPlaylistItem) =
        _activeSession.value?.hostSelectItem(item)

    fun hostNotifyPlayerCompleted() = _activeSession.value?.hostNotifyCompleted()

    fun hostPublishPositionCorrection(positionSeconds: Float) =
        _activeSession.value?.hostPublishPositionCorrection(positionSeconds)


    fun addPlaylistVideo(rawUrl: String) {
        val session = _activeSession.value
        if (session != null) session.addPlaylistVideo(rawUrl) else repository.addVideoToPlaylist(activeRoomId(), rawUrl)
    }

    fun removePlaylistVideo(itemId: String) {
        val session = _activeSession.value
        if (session != null) session.removePlaylistVideo(itemId) else repository.removeVideoFromPlaylist(activeRoomId(), itemId)
    }

    fun selectPlaylistVideo(itemId: String) {
        val session = _activeSession.value
        if (session != null) session.selectPlaylistVideo(itemId) else repository.selectPlaylistItem(activeRoomId(), itemId)
    }

    /** Room currently rendered; used only for the non-persisted demo rooms. */
    private fun activeRoomId(): String = _activeSession.value?.roomId ?: repository.activeRoom.value?.id.orEmpty()

    fun removeRoomParticipant(uid: String) = _activeSession.value?.removeParticipant(uid)

    fun consumeSessionNotice(noticeId: Long) {
        _activeSession.value?.consumeNotice(noticeId)
    }

    fun consumeLiveHint() {
        _liveHints.value = null
    }

    // ---------------------------------------------------------------------------------------
    // Follow / unfollow through the existing Firestore social graph
    // ---------------------------------------------------------------------------------------

    fun toggleFollowUser(targetUid: String) {
        if (targetUid.isBlank()) return
        val actorUid = _liveIdentity.value?.uid
        if (actorUid == null) {
            _liveHints.value = LiveSessionNotice(
                message = "Sign in to follow WorkDaily members.",
                isError = true
            )
            return
        }
        if (actorUid == targetUid) return
        val wantFollow = !followingUids.value.contains(targetUid)
        viewModelScope.launch {
            val result = SocialSessionCache.manager.setFollowState(actorUid, targetUid, wantFollow)
            when (result) {
                null -> Unit // duplicate tap while a mutation is in flight
                is ProfileOperationResult.Success -> {
                    if (wantFollow) {
                        val session = _activeSession.value
                        val identity = _liveIdentity.value
                        if (session != null && identity != null) {
                            val targetProfile = loadPublicProfile(targetUid)
                            val targetName = targetProfile?.displayName ?: "host"
                            val clientMessageId = "sys_follow_${session.roomId}_${actorUid}_${targetUid}"
                            val followMsg = ChatMessage(
                                id = clientMessageId,
                                roomId = session.roomId,
                                senderId = actorUid,
                                senderName = identity.displayName,
                                senderAvatar = identity.photoUrl,
                                senderUsername = identity.username,
                                message = "${identity.displayName} started following $targetName",
                                timestamp = System.currentTimeMillis(),
                                eventType = "USER_FOLLOWED",
                                clientMessageId = clientMessageId
                            )
                            session.sendSystemEvent(followMsg)
                        }
                    }
                }
                is ProfileOperationResult.Error -> {
                    _liveHints.value = LiveSessionNotice(message = result.message, isError = true)
                }
            }
        }
    }

    /** Room host ends the whole Live room (not just Watch Party playback). */
    suspend fun endActiveLiveRoom(): LiveOperationResult<Unit> {
        if (!endLiveMutex.tryLock()) return LiveOperationResult.Error("Ending this room is already in progress.")
        try {
            val session = _activeSession.value
                ?: return LiveOperationResult.Error("No active live room session.")
            if (!session.isHost()) return LiveOperationResult.Error("Only the host can end this live room.", isAuthError = true)
            // If a host media connection has been established, stop capture before ending.
            // Do not instantiate a media manager for text/watch-party rooms.
            if (mediaManagerDelegate.isInitialized() && mediaManager.state.value.isHost) {
                mediaManager.disconnect()
            }
            val result = session.endRoom()
            if (result is LiveOperationResult.Success) {
                repository.endLiveStream(session.roomId)
            }
            return result
        } finally {
            endLiveMutex.unlock()
        }
    }

    fun sendFriendRequest(targetUserId: String): Boolean {
        return repository.sendFriendRequest(targetUserId)
    }

    fun acceptFriendRequest(requestId: String): Boolean {
        return repository.acceptFriendRequest(requestId)
    }

    fun declineFriendRequest(requestId: String) {
        repository.declineFriendRequest(requestId)
    }

    fun cancelFriendRequest(requestId: String) {
        repository.cancelFriendRequest(requestId)
    }

    fun unfriendUser(targetUserId: String) {
        repository.unfriendUser(targetUserId)
    }

    fun sendPrivateMessage(friendId: String, text: String): Boolean {
        return repository.sendPrivateMessage(friendId, text)
    }

    fun deletePrivateMessage(conversationId: String, messageId: String) {
        repository.deletePrivateMessage(conversationId, messageId)
    }

    fun clearConversation(conversationId: String) {
        repository.clearConversation(conversationId)
    }

    fun unblockUser(userId: String) {
        repository.unblockUser(userId)
    }

    fun resetSocialDemoData() {
        repository.resetSocialDemoData()
    }

    fun toggleFollowCreator(creatorId: String, roomId: String? = null) {
        repository.toggleFollowCreator(creatorId, roomId)
    }

    fun removeGiftOverlayEvent(eventId: String) {
        repository.removeGiftOverlayEvent(eventId)
    }

    fun onUserJoinedRoom(roomId: String, userName: String = "Dev") {
        repository.onUserJoinedRoom(roomId, userName)
    }

    fun onShareLiveStream(roomId: String, senderName: String = "You") {
        repository.onShareLiveStream(roomId, senderName)
    }

    fun triggerHostFollowInvitation(roomId: String, hostName: String) {
        repository.triggerHostFollowInvitation(roomId, hostName)
    }

    fun topUpDiamonds(amount: Int) {
        repository.topUpDiamonds(amount)
    }

    fun addFreeDiamonds(amount: Int = 1000) {
        repository.addFreeDiamonds(amount)
    }

    fun sendGift(roomId: String, gift: LiveGift, quantity: Int = 1): Boolean {
        return repository.sendGift(roomId, gift, quantity)
    }

    fun sendHearts(roomId: String, count: Int = 1) {
        repository.sendHearts(roomId, count)
    }

    fun sendChatMessage(roomId: String, text: String, senderName: String = "You") {
        val identity = liveIdentity.value
        val name = identity?.displayName ?: senderName
        val avatar = identity?.photoUrl.orEmpty()
        repository.sendChatMessage(roomId, text, name, avatar)
    }

    fun pinChatMessage(roomId: String, messageId: String) {
        repository.pinChatMessage(roomId, messageId)
    }

    fun unpinChatMessage(roomId: String) {
        repository.unpinChatMessage(roomId)
    }

    fun deleteChatMessage(roomId: String, messageId: String) {
        repository.deleteChatMessage(roomId, messageId)
    }

    fun toggleChatEnabled(roomId: String) {
        repository.toggleChatEnabled(roomId)
    }

    fun setActiveRoom(roomId: String?) {
        repository.setActiveRoom(roomId)
    }

    fun updateWatchPartyState(roomId: String, status: WatchPartyPlaybackStatus, positionSeconds: Float) {
        repository.updateWatchPartyState(roomId, status, positionSeconds)
    }

    fun addVideoToPlaylist(roomId: String, videoUrl: String, title: String = ""): Boolean {
        return repository.addVideoToPlaylist(roomId, videoUrl, title)
    }

    fun removeVideoFromPlaylist(roomId: String, playlistItemId: String): Boolean {
        return repository.removeVideoFromPlaylist(roomId, playlistItemId)
    }

    fun selectPlaylistItem(roomId: String, playlistItemId: String): Boolean {
        return repository.selectPlaylistItem(roomId, playlistItemId)
    }

    fun turnOffWatchParty(roomId: String): Boolean {
        return repository.turnOffWatchParty(roomId)
    }

    fun toggleMuteWatchParty(roomId: String): Boolean {
        return repository.toggleMuteWatchParty(roomId)
    }

    fun startLiveStream(
        title: String,
        category: String,
        type: LiveRoomType,
        tags: List<String>,
        description: String = "",
        isPublic: Boolean = true,
        allowGuestRequests: Boolean = true,
        showGifts: Boolean = true,
        youtubeVideoUrl: String? = null,
        youtubeVideoId: String? = null,
        youtubePlaylistId: String? = null
    ): LiveRoom {
        return repository.startLiveStream(
            title, category, type, tags, description, isPublic, allowGuestRequests, showGifts,
            youtubeVideoUrl, youtubeVideoId, youtubePlaylistId
        )
    }

    fun endLiveStream(roomId: String): LiveEndSummary? {
        return repository.endLiveStream(roomId)
    }

    fun requestToJoinGuest(roomId: String, userName: String = "You"): GuestRequest {
        return repository.requestToJoinGuest(roomId, userName)
    }

    fun approveGuestRequest(roomId: String, requestId: String) {
        repository.approveGuestRequest(roomId, requestId)
    }

    fun rejectGuestRequest(roomId: String, requestId: String) {
        repository.rejectGuestRequest(roomId, requestId)
    }

    fun occupyAudioSeat(roomId: String, seatIndex: Int, userId: String, userName: String) {
        repository.occupyAudioSeat(roomId, seatIndex, userId, userName)
    }

    fun leaveAudioSeat(roomId: String, seatIndex: Int) {
        repository.leaveAudioSeat(roomId, seatIndex)
    }

    fun toggleAudioSeatMute(roomId: String, seatIndex: Int) {
        repository.toggleAudioSeatMute(roomId, seatIndex)
    }

    fun toggleLockSeat(roomId: String, seatIndex: Int) {
        repository.toggleLockSeat(roomId, seatIndex)
    }

    fun blockUser(userId: String) {
        repository.blockUser(userId)
    }

    fun muteUserInRoom(roomId: String, userId: String) {
        repository.muteUserInRoom(roomId, userId)
    }

    fun markNotificationRead(id: String) {
        repository.markNotificationRead(id)
    }

    fun toggleSoundEnabled() {
        repository.toggleSoundEnabled()
    }

    fun toggleHapticsEnabled() {
        repository.toggleHapticsEnabled()
    }

    fun cashoutEarnings(diamonds: Int): Boolean {
        return repository.cashoutEarnings(diamonds)
    }

    fun updateDemoDataOverrides(
        diamonds: Int? = null,
        creatorDiamonds: Int? = null,
        followersCount: Int? = null,
        viewerCount: Int? = null,
        likeCount: Int? = null
    ) {
        repository.updateDemoDataOverrides(diamonds, creatorDiamonds, followersCount, viewerCount, likeCount)
    }

    fun resetDemoData() {
        repository.resetDemoData()
    }
}

private const val TAG = "LiveViewModel"

class LiveViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiveViewModel::class.java)) {
            return LiveViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
