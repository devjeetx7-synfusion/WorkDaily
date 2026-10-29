package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.synfusion.attendance.live.data.LiveRoomBackend
import com.synfusion.attendance.live.data.LiveRoomCreateRequest
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
import com.synfusion.attendance.live.watchparty.LiveChatPolicy
import com.synfusion.attendance.live.watchparty.PlaylistMutation
import com.synfusion.attendance.live.watchparty.PresencePolicy
import com.synfusion.attendance.live.watchparty.WatchPartyPlaylistLogic
import com.synfusion.attendance.live.watchparty.WatchPartySyncPolicy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * Production Firestore implementation of [LiveRoomBackend].
 *
 * DATA LAYOUT (extends the existing `live_rooms` collection instead of introducing a parallel one)
 *
 * ```
 * live_rooms/{roomId}                                  room + host metadata + live flag
 * live_rooms/{roomId}/members/{uid}                    presence/membership (uid-keyed => no dupes)
 * live_rooms/{roomId}/watch_party/state                shared playback state (single doc)
 * live_rooms/{roomId}/watch_party_playlist/{videoId}   playlist items (rarely written)
 * live_rooms/{roomId}/messages/{clientMessageId}       persistent chat (bounded reads)
 * ```
 *
 * Splitting the playback state from the playlist documents is what keeps a play/pause/seek or a
 * periodic position correction from rewriting the entire playlist.
 *
 * The matching security rules live in `firestore.rules`; every write below additionally validates
 * the authenticated uid client-side so failures surface as clear errors instead of permission
 * exceptions.
 */
class FirestoreLiveRoomBackend(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString().replace("-", "").take(20) }
) : LiveRoomBackend {

    private companion object {
        const val ROOMS = "live_rooms"
        const val MEMBERS = "members"
        const val MESSAGES = "messages"
        const val WATCH_PARTY = "watch_party"
        const val STATE = "state"

        /**
         * Playlist rows live in their own room-level collection.
         *
         * A Firestore path alternates collection/document, so `watch_party/playlist/{id}` (as a
         * sibling of the `state` document) is not addressable: `CollectionReference` exposes no
         * `collection()` and the security-rule path would have an odd number of segments. Keeping the
         * queue in `watch_party_playlist` preserves the important property (playback writes never
         * rewrite the queue) with a valid path.
         */
        const val WATCH_PARTY_PLAYLIST = "watch_party_playlist"
    }

    private fun currentUid(): String? = try {
        authSupplier().currentUser?.uid
    } catch (_: Throwable) {
        null
    }

    private fun rooms() = firestoreSupplier().collection(ROOMS)
    private fun roomRef(roomId: String) = rooms().document(roomId)
    private fun stateRef(roomId: String) = roomRef(roomId).collection(WATCH_PARTY).document(STATE)
    private fun playlistRef(roomId: String) = roomRef(roomId).collection(WATCH_PARTY_PLAYLIST)
    private fun membersRef(roomId: String) = roomRef(roomId).collection(MEMBERS)
    private fun messagesRef(roomId: String) = roomRef(roomId).collection(MESSAGES)

    // ---------------------------------------------------------------------------------------
    // Room lifecycle
    // ---------------------------------------------------------------------------------------

    override suspend fun createRoom(
        host: LiveUserIdentity,
        request: LiveRoomCreateRequest
    ): LiveOperationResult<LiveRoom> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in to start a live room.", isAuthError = true)
        if (uid != host.uid) {
            return LiveOperationResult.Error("Signed-in account changed. Try again.", isAuthError = true)
        }
        if (host.displayName.isBlank()) {
            return LiveOperationResult.Error("Your WorkDaily profile is incomplete. Set a display name first.")
        }

        return try {
            val firestore = firestoreSupplier()
            // Single equality filter keeps this query on the automatic index (no composite index
            // deployment required); the live flag is filtered client-side.
            val existing = firestore.collection(ROOMS)
                .whereEqualTo("hostUserId", uid)
                .limit(20)
                .get()
                .awaitTask()
            if (existing.documents.any { it.getBoolean("isLive") == true }) {
                return LiveOperationResult.Error(
                    "You already have an active live room. End it before starting a new one.",
                    isConflict = true
                )
            }

            val now = clock()
            val roomRef = rooms().document()
            val roomId = roomRef.id
            val initialWatchParty = if (request.type == LiveRoomType.YOUTUBE_WATCH_PARTY) {
                WatchPartyPlaylistLogic.initialState(roomId, host.uid, request.selection, now)
            } else {
                null
            }

            val roomData = mutableMapOf<String, Any?>(
                "id" to roomId,
                "hostUserId" to host.uid,
                "creatorId" to host.uid,
                "creatorName" to host.displayName,
                "hostName" to host.displayName,
                "hostUsername" to host.username,
                "hostAvatar" to host.photoUrl,
                "creatorAvatar" to host.photoUrl,
                "creatorProfession" to host.profession,
                "tradeRole" to host.profession,
                "title" to request.title.ifBlank { "WorkDaily Live" },
                "category" to request.category.ifBlank { "Site Safety" },
                "type" to request.type.name,
                "tags" to request.tags,
                "description" to request.description,
                "isPublic" to request.isPublic,
                "allowGuestRequests" to request.allowGuestRequests,
                "showGifts" to request.showGifts,
                "isChatEnabled" to request.isChatEnabled,
                "isLive" to true,
                "statusText" to "LIVE",
                "startTimeMillis" to now,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                // Explicit nulls keep the security-rule predicates (isRoomLive) evaluable on the
                // freshly created document instead of erroring on a missing field.
                "endedAt" to null,
                "endedAtMillis" to null,
                "youtubeVideoId" to request.selection?.videoId,
                "youtubeVideoUrl" to request.selection?.canonicalUrl,
                "youtubePlaylistId" to request.selection?.playlistId,
                "diamondEarnings" to 0,
                "likeCount" to 0,
                "peakViewerCount" to 0
            )

            // The room document is written first: every sub-collection rule below verifies that the
            // parent room exists and is live, so it cannot be created in the same atomic batch.
            roomRef.set(roomData).awaitTask()

            // Ensure host room membership is immediately written and persisted
            val hostMember = host.toMember(isHost = true, nowMillis = now)
            membersRef(roomId).document(host.uid).set(memberToMap(hostMember)).awaitTask()

            initialWatchParty?.let { state ->
                val batch = firestore.batch()
                batch.set(stateRef(roomId), stateToMap(state))
                val selection = request.selection
                val selectedVideoId = selection?.videoId
                if (selection != null && selection.isVideoPlayable && selectedVideoId != null) {
                    val item = WatchPartyPlaylistItem(
                        id = selectedVideoId,
                        videoId = selectedVideoId,
                        videoUrl = selection.canonicalUrl,
                        title = "YouTube Video ($selectedVideoId)",
                        thumbnailUrl = com.synfusion.attendance.live.watchparty.YouTubeLinkParser.thumbnailUrl(selectedVideoId),
                        addedByUserId = host.uid,
                        addedByUserName = host.displayName,
                        addedAtMillis = now
                    )
                    batch.set(playlistRef(roomId).document(item.id), itemToMap(item))
                }
                batch.commit().awaitTask()
            }

            // Write initial system messages for room creation
            val startMsgId = "sys_start_$roomId"
            val startMsgData = mapOf(
                "roomId" to roomId,
                "senderId" to host.uid,
                "senderName" to host.displayName,
                "senderUsername" to host.username,
                "senderAvatar" to host.photoUrl,
                "message" to "${host.displayName} started the live broadcast",
                "clientMessageId" to startMsgId,
                "timestamp" to FieldValue.serverTimestamp(),
                "timestampMillis" to now,
                "isGiftAnnouncement" to false,
                "isSystemMessage" to true,
                "isPinned" to false,
                "eventType" to "LIVE_STARTED"
            )
            messagesRef(roomId).document(startMsgId).set(startMsgData).awaitTask()

            if (request.type == LiveRoomType.YOUTUBE_WATCH_PARTY) {
                val wpMsgId = "sys_wp_$roomId"
                val wpMsgData = mapOf(
                    "roomId" to roomId,
                    "senderId" to host.uid,
                    "senderName" to "WorkDaily System",
                    "senderUsername" to "workdaily",
                    "senderAvatar" to "",
                    "message" to "${host.displayName} started a Watch Party",
                    "clientMessageId" to wpMsgId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "timestampMillis" to now + 1,
                    "isGiftAnnouncement" to false,
                    "isSystemMessage" to true,
                    "isPinned" to false,
                    "eventType" to "WATCH_PARTY_STARTED"
                )
                messagesRef(roomId).document(wpMsgId).set(wpMsgData).awaitTask()
            }

            // Host membership is written once the room exists so presence exists from creation time.
            membersRef(roomId).document(host.uid)
                .set(memberToMap(host.toMember(isHost = true, nowMillis = now)))
                .awaitTask()

            val created = LiveRoom(
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
            LiveOperationResult.Success(created)
        } catch (e: Exception) {
            mapException(e, "Failed to create the live room")
        }
    }

    override fun observeRoom(roomId: String): Flow<LiveRoom?> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = try {
            roomRef(roomId).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.takeIf { it.exists() }?.let { documentToRoom(it.id, it.data.orEmpty()) })
            }
        } catch (_: Exception) {
            trySend(null)
            close()
            return@callbackFlow
        }
        awaitClose { registration.remove() }
    }

    override fun observeActiveRooms(): Flow<List<LiveRoom>> = callbackFlow {
        val registration = try {
            // Single equality filter keeps query on the automatic index.
            rooms()
                .whereEqualTo("isLive", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val activeRooms = snapshot?.documents?.mapNotNull { doc ->
                        doc.takeIf { it.exists() }?.let { documentToRoom(it.id, it.data.orEmpty()) }
                    }?.filter { room ->
                        room.isLive && room.endedAtMillis == null && room.isPublic
                    }.orEmpty()
                    trySend(activeRooms)
                }
        } catch (_: Exception) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        awaitClose { registration.remove() }
    }

    override suspend fun loadRoom(roomId: String): LiveOperationResult<LiveRoom?> {
        if (roomId.isBlank()) return LiveOperationResult.Error("Invalid room id.")
        if (currentUid() == null) {
            return LiveOperationResult.Error("Sign in to open a live room.", isAuthError = true)
        }
        return try {
            val snapshot = roomRef(roomId).get().awaitTask()
            if (!snapshot.exists()) {
                LiveOperationResult.Success(null)
            } else {
                LiveOperationResult.Success(documentToRoom(snapshot.id, snapshot.data.orEmpty()))
            }
        } catch (e: Exception) {
            mapException(e, "Failed to load the live room")
        }
    }

    override suspend fun endRoom(roomId: String, hostUid: String): LiveOperationResult<Unit> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in first.", isAuthError = true)
        if (uid != hostUid) {
            return LiveOperationResult.Error("Only the host can end this live room.", isAuthError = true)
        }
        return try {
            val firestore = firestoreSupplier()
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef(roomId))
                if (!roomSnapshot.exists()) throw LiveRoomFailure("This live room no longer exists.")
                val persistedHost = roomSnapshot.getString("hostUserId").orEmpty()
                if (persistedHost != uid) {
                    throw LiveRoomFailure("Only the host can end this live room.", isAuthError = true)
                }

                // READ PHASE: All document reads must execute before any writes!
                val stateSnapshot = transaction.get(stateRef(roomId))

                // WRITE PHASE
                transaction.update(
                    roomRef(roomId),
                    mapOf(
                        "isLive" to false,
                        "statusText" to "ENDED",
                        "endedAt" to FieldValue.serverTimestamp(),
                        "endedAtMillis" to clock(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                // Close the Watch Party playback for late joiners.
                if (stateSnapshot.exists()) {
                    transaction.update(
                        stateRef(roomId),
                        mapOf(
                            "status" to WatchPartyPlaybackStatus.ENDED.name,
                            "updatedAt" to FieldValue.serverTimestamp(),
                            "updatedAtMillis" to clock(),
                            "revision" to ((stateSnapshot.getLong("revision") ?: 0L) + 1L)
                        )
                    )
                }

                // Write system message for room end
                val endMsgId = "sys_end_$roomId"
                val endMsgData = mapOf(
                    "roomId" to roomId,
                    "senderId" to uid,
                    "senderName" to "WorkDaily System",
                    "senderUsername" to "workdaily",
                    "senderAvatar" to "",
                    "message" to "Live broadcast ended by host",
                    "clientMessageId" to endMsgId,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "timestampMillis" to clock(),
                    "isGiftAnnouncement" to false,
                    "isSystemMessage" to true,
                    "isPinned" to false,
                    "eventType" to "LIVE_ENDED"
                )
                transaction.set(messagesRef(roomId).document(endMsgId), endMsgData)
            }.awaitTask()
            LiveOperationResult.Success(Unit)
        } catch (e: Exception) {
            mapException(e, "Failed to end the live room")
        }
    }

    // ---------------------------------------------------------------------------------------
    // Membership / presence
    // ---------------------------------------------------------------------------------------

    override suspend fun joinRoom(
        roomId: String,
        identity: LiveUserIdentity
    ): LiveOperationResult<Unit> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in to join this live room.", isAuthError = true)
        if (uid != identity.uid) {
            return LiveOperationResult.Error("Signed-in account changed. Try again.", isAuthError = true)
        }
        return try {
            val firestore = firestoreSupplier()
            firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef(roomId))
                if (!roomSnapshot.exists()) throw LiveRoomFailure("This live room no longer exists.")
                if (roomSnapshot.getBoolean("isLive") != true) {
                    throw LiveRoomFailure("This live room has ended.")
                }
                val hostUid = roomSnapshot.getString("hostUserId").orEmpty()
                val membershipRef = membersRef(roomId).document(uid)
                val existing = transaction.get(membershipRef)

                val joinMsgId = "sys_join_${roomId}_${uid}"
                val joinMsgRef = messagesRef(roomId).document(joinMsgId)
                val existingMsg = transaction.get(joinMsgRef)

                // READ PHASE COMPLETE: Perform all writes after reads!
                val now = clock()
                val member = identity.toMember(isHost = uid == hostUid, nowMillis = now).copy(
                    joinedAtMillis = (existing.getLong("joinedAtMillis") ?: now),
                    lastSeenAtMillis = now
                )
                transaction.set(membershipRef, memberToMap(member), SetOptions.merge())

                // Write system join event idempotently
                if (!existingMsg.exists()) {
                    val joinMsgData = mapOf(
                        "roomId" to roomId,
                        "senderId" to uid,
                        "senderName" to identity.displayName,
                        "senderUsername" to identity.username,
                        "senderAvatar" to identity.photoUrl,
                        "message" to "${identity.displayName} joined the live",
                        "clientMessageId" to joinMsgId,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "timestampMillis" to now,
                        "isGiftAnnouncement" to false,
                        "isSystemMessage" to false,
                        "isPinned" to false,
                        "eventType" to "USER_JOINED"
                    )
                    transaction.set(joinMsgRef, joinMsgData)
                }
            }.awaitTask()
            LiveOperationResult.Success(Unit)
        } catch (e: Exception) {
            mapException(e, "Failed to join the live room")
        }
    }

    override suspend fun heartbeat(roomId: String, uid: String): LiveOperationResult<Unit> {
        val current = currentUid()
            ?: return LiveOperationResult.Error("Sign in first.", isAuthError = true)
        if (current != uid) {
            return LiveOperationResult.Error("Signed-in account changed.", isAuthError = true)
        }
        return try {
            val snapshot = membersRef(roomId).document(uid).get().awaitTask()
            if (!snapshot.exists()) {
                LiveOperationResult.Error("Membership expired; rejoin the room.")
            } else {
                membersRef(roomId).document(uid).update(
                    mapOf(
                        "lastSeenAtMillis" to clock(),
                        "lastSeenAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
                LiveOperationResult.Success(Unit)
            }
        } catch (e: Exception) {
            mapException(e, "Presence heartbeat failed")
        }
    }

    override suspend fun leaveRoom(roomId: String, uid: String): LiveOperationResult<Unit> {
        val current = currentUid()
        if (current != uid) {
            return LiveOperationResult.Error("Signed-in account changed.", isAuthError = true)
        }
        return try {
            membersRef(roomId).document(uid).delete().awaitTask()
            LiveOperationResult.Success(Unit)
        } catch (e: Exception) {
            mapException(e, "Failed to leave the live room")
        }
    }

    override suspend fun removeMember(
        roomId: String,
        hostUid: String,
        memberUid: String
    ): LiveOperationResult<Unit> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in first.", isAuthError = true)
        if (uid != hostUid) {
            return LiveOperationResult.Error("Only the host can remove participants.", isAuthError = true)
        }
        return try {
            val roomSnapshot = roomRef(roomId).get().awaitTask()
            val persistedHost = roomSnapshot.getString("hostUserId").orEmpty()
            if (!PresencePolicy.canModerate(uid, persistedHost, memberUid)) {
                return LiveOperationResult.Error("Only the host can remove participants.", isAuthError = true)
            }
            membersRef(roomId).document(memberUid).delete().awaitTask()
            LiveOperationResult.Success(Unit)
        } catch (e: Exception) {
            mapException(e, "Failed to remove the participant")
        }
    }

    override fun observeMembers(roomId: String): Flow<List<LiveRoomMember>> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = try {
            membersRef(roomId)
                .limit(PresencePolicy.MEMBER_QUERY_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val members = snapshot?.documents?.mapNotNull { document ->
                        documentToMember(document.id, document.data.orEmpty())
                    }.orEmpty()
                    trySend(PresencePolicy.sort(members))
                }
        } catch (_: Exception) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        awaitClose { registration.remove() }
    }

    // ---------------------------------------------------------------------------------------
    // Watch Party playback + playlist
    // ---------------------------------------------------------------------------------------

    override fun observeWatchParty(roomId: String): Flow<WatchPartyState?> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = try {
            stateRef(roomId).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                } else {
                    trySend(documentToWatchParty(roomId, snapshot.data.orEmpty()))
                }
            }
        } catch (_: Exception) {
            trySend(null)
            close()
            return@callbackFlow
        }
        awaitClose { registration.remove() }
    }

    override fun observePlaylist(roomId: String): Flow<List<WatchPartyPlaylistItem>> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = try {
            playlistRef(roomId).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val items = snapshot?.documents?.mapNotNull { document ->
                    documentToPlaylistItem(document.id, document.data.orEmpty())
                }.orEmpty()
                trySend(WatchPartyPlaylistLogic.ordered(items))
            }
        } catch (_: Exception) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        awaitClose { registration.remove() }
    }

    override suspend fun applyPlaybackCommand(
        roomId: String,
        hostUid: String,
        expectedRevision: Long,
        command: LivePlaybackCommand
    ): LiveOperationResult<WatchPartyState> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in to control playback.", isAuthError = true)
        if (uid != hostUid) {
            return LiveOperationResult.Error("Only the host can change playback.", isAuthError = true)
        }
        return try {
            val firestore = firestoreSupplier()
            val result = firestore.runTransaction { transaction ->
                val roomSnapshot = transaction.get(roomRef(roomId))
                if (!roomSnapshot.exists()) throw LiveRoomFailure("This live room no longer exists.")
                val persistedHost = roomSnapshot.getString("hostUserId").orEmpty()
                if (persistedHost != uid) {
                    throw LiveRoomFailure("Only the host can change playback.", isAuthError = true)
                }
                val isLive = roomSnapshot.getBoolean("isLive") == true
                val endedAtMillis = roomSnapshot.getLong("endedAtMillis")
                if (!WatchPartySyncPolicy.canAcceptPlaybackCommands(isLive, endedAtMillis)) {
                    throw LiveRoomFailure("This live room has ended.")
                }

                val stateSnapshot = transaction.get(stateRef(roomId))
                if (!stateSnapshot.exists()) {
                    throw LiveRoomFailure("Watch Party is not enabled for this room.")
                }
                val current = documentToWatchParty(roomId, stateSnapshot.data.orEmpty())
                if (current.revision != expectedRevision) {
                    throw LiveRoomConflict("Playback changed on another device (revision ${current.revision}).")
                }
                val next = WatchPartySyncPolicy.applyCommand(
                    current = current,
                    command = command,
                    actorUid = uid,
                    nowMillis = clock(),
                    commandId = "cmd-" + idGenerator()
                )
                transaction.set(stateRef(roomId), stateToMap(next), SetOptions.merge())
                next
            }.awaitTask()
            LiveOperationResult.Success(result)
        } catch (e: Exception) {
            mapException(e, "Failed to update playback")
        }
    }

    override suspend fun addPlaylistItem(
        roomId: String,
        hostUid: String,
        item: WatchPartyPlaylistItem
    ): LiveOperationResult<Unit> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in to edit the playlist.", isAuthError = true)
        if (uid != hostUid) {
            return LiveOperationResult.Error("Only the host can edit the playlist.", isAuthError = true)
        }
        return try {
            // NOTE: the Android SDK's Transaction can only read Documents (there is no
            // Transaction.get(Query)), so the playlist duplicate/capacity checks use plain reads.
            // The transaction below then re-reads the room + playback documents and performs the
            // actual writes atomically against the freshest state.
            val roomSnapshot = roomRef(roomId).get().awaitTask()
            if (!roomSnapshot.exists()) {
                return LiveOperationResult.Error("This live room no longer exists.")
            }
            if (roomSnapshot.getString("hostUserId") != uid) {
                return LiveOperationResult.Error("Only the host can edit the playlist.", isAuthError = true)
            }
            if (roomSnapshot.getBoolean("isLive") != true) {
                return LiveOperationResult.Error("This live room has ended.")
            }

            val existing = playlistRef(roomId).get().awaitTask().documents.mapNotNull { document ->
                documentToPlaylistItem(document.id, document.data.orEmpty())
            }
            if (existing.any { it.id == item.id || it.videoId == item.videoId }) {
                return LiveOperationResult.Error("That video is already in the playlist.", isConflict = true)
            }
            if (existing.size >= WatchPartyPlaylistLogic.MAX_PLAYLIST_SIZE) {
                return LiveOperationResult.Error("This playlist is full.")
            }

            val firestore = firestoreSupplier()
            firestore.runTransaction { transaction ->
                val freshRoom = transaction.get(roomRef(roomId))
                if (!freshRoom.exists()) throw LiveRoomFailure("This live room no longer exists.")
                if (freshRoom.getString("hostUserId") != uid) {
                    throw LiveRoomFailure("Only the host can edit the playlist.", isAuthError = true)
                }
                if (freshRoom.getBoolean("isLive") != true) throw LiveRoomFailure("This live room has ended.")

                val stateSnapshot = transaction.get(stateRef(roomId))
                if (!stateSnapshot.exists()) throw LiveRoomFailure("Watch Party is not enabled for this room.")
                val currentState = documentToWatchParty(roomId, stateSnapshot.data.orEmpty())

                val activeWasEmpty = currentState.activePlaylistItemId.isNullOrBlank() || currentState.videoId.isBlank()
                val nextState = currentState.copy(
                    revision = currentState.revision + 1L,
                    updatedAtMillis = clock(),
                    updatedByUserId = uid,
                    activePlaylistItemId = if (activeWasEmpty) item.id else currentState.activePlaylistItemId,
                    videoId = if (activeWasEmpty) item.videoId else currentState.videoId,
                    status = if (activeWasEmpty) WatchPartyPlaybackStatus.UNSTARTED else currentState.status
                )
                transaction.set(playlistRef(roomId).document(item.id), itemToMap(item))
                transaction.set(stateRef(roomId), stateToMap(nextState), SetOptions.merge())
            }.awaitTask()
            LiveOperationResult.Success(Unit)
        } catch (e: Exception) {
            mapException(e, "Failed to add the video")
        }
    }

    override suspend fun removePlaylistItem(
        roomId: String,
        hostUid: String,
        itemId: String
    ): LiveOperationResult<Unit> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in to edit the playlist.", isAuthError = true)
        if (uid != hostUid) {
            return LiveOperationResult.Error("Only the host can edit the playlist.", isAuthError = true)
        }
        return try {
            // Plain read for the queue (Transaction cannot read a Query); the transaction re-reads
            // the room + playback documents before writing so the revision chain stays intact.
            val roomSnapshot = roomRef(roomId).get().awaitTask()
            if (!roomSnapshot.exists()) {
                return LiveOperationResult.Error("This live room no longer exists.")
            }
            if (roomSnapshot.getString("hostUserId") != uid) {
                return LiveOperationResult.Error("Only the host can edit the playlist.", isAuthError = true)
            }
            if (roomSnapshot.getBoolean("isLive") != true) {
                return LiveOperationResult.Error("This live room has ended.")
            }

            val existing = playlistRef(roomId).get().awaitTask().documents.mapNotNull { document ->
                documentToPlaylistItem(document.id, document.data.orEmpty())
            }
            if (existing.none { it.id == itemId }) {
                return LiveOperationResult.Error("Playlist entry was already removed.", isConflict = true)
            }

            val firestore = firestoreSupplier()
            firestore.runTransaction { transaction ->
                val freshRoom = transaction.get(roomRef(roomId))
                if (!freshRoom.exists()) throw LiveRoomFailure("This live room no longer exists.")
                if (freshRoom.getString("hostUserId") != uid) {
                    throw LiveRoomFailure("Only the host can edit the playlist.", isAuthError = true)
                }
                if (freshRoom.getBoolean("isLive") != true) throw LiveRoomFailure("This live room has ended.")

                val stateSnapshot = transaction.get(stateRef(roomId))
                if (!stateSnapshot.exists()) throw LiveRoomFailure("Watch Party is not enabled for this room.")
                val currentState = documentToWatchParty(roomId, stateSnapshot.data.orEmpty())

                when (val mutation = WatchPartyPlaylistLogic.removeItem(
                    current = currentState,
                    playlist = existing,
                    itemId = itemId,
                    actorUid = uid,
                    nowMillis = clock()
                )) {
                    is PlaylistMutation.Success -> {
                        transaction.delete(playlistRef(roomId).document(itemId))
                        transaction.set(stateRef(roomId), stateToMap(mutation.state), SetOptions.merge())
                    }

                    is PlaylistMutation.Rejected -> throw LiveRoomFailure(mutation.reason)
                    is PlaylistMutation.Duplicate -> throw LiveRoomConflict("Playlist entry was already removed.")
                }
            }.awaitTask()
            LiveOperationResult.Success(Unit)
        } catch (e: Exception) {
            mapException(e, "Failed to remove the video")
        }
    }

    // ---------------------------------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------------------------------

    override fun observeMessages(roomId: String, limit: Int): Flow<List<ChatMessage>> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = messagesRef(roomId)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        val registration = try {
            query.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { document ->
                    documentToChatMessage(roomId, document.id, document.data.orEmpty())
                }.orEmpty().sortedWith(compareBy({ it.timestamp }, { it.id }))
                trySend(list)
            }
        } catch (_: Exception) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(
        roomId: String,
        identity: LiveUserIdentity,
        text: String,
        clientMessageId: String
    ): LiveOperationResult<ChatMessage> {
        val uid = currentUid()
            ?: return LiveOperationResult.Error("Sign in to chat.", isAuthError = true)
        if (uid != identity.uid) {
            return LiveOperationResult.Error("Signed-in account changed. Try again.", isAuthError = true)
        }
        if (!isSafeDocumentId(clientMessageId)) {
            return LiveOperationResult.Error("Invalid message id.")
        }
        return try {
            // Membership is a hard requirement; the security rules enforce it server side too.
            var membership = membersRef(roomId).document(uid).get().awaitTask()
            if (!membership.exists()) {
                val joinResult = joinRoom(roomId, identity)
                if (joinResult is LiveOperationResult.Success) {
                    membership = membersRef(roomId).document(uid).get().awaitTask()
                }
            }
            if (!membership.exists()) {
                return LiveOperationResult.Error("Join the room before chatting.", isAuthError = true)
            }
            val roomSnapshot = roomRef(roomId).get().awaitTask()
            if (!roomSnapshot.exists()) {
                return LiveOperationResult.Error("This live room no longer exists.")
            }
            if (roomSnapshot.getBoolean("isLive") != true) {
                return LiveOperationResult.Error("This live room has ended.")
            }
            if (roomSnapshot.getBoolean("isChatEnabled") == false) {
                return LiveOperationResult.Error("Chat is disabled in this room.")
            }
            val messageRef = messagesRef(roomId).document(clientMessageId)
            val existing = messageRef.get().awaitTask()
            if (existing.exists()) {
                return LiveOperationResult.Success(
                    documentToChatMessage(roomId, clientMessageId, existing.data.orEmpty())
                )
            }
            val validation = LiveChatPolicy.validateSend(
                rawText = text,
                isRoomLive = true,
                isChatEnabled = true,
                isMember = true,
                isBlocked = false
            )
            val safeText = when (validation) {
                is com.synfusion.attendance.live.watchparty.ChatValidation.Valid -> validation.text
                is com.synfusion.attendance.live.watchparty.ChatValidation.Invalid ->
                    return LiveOperationResult.Error(validation.message)
            }
            val now = clock()
            val senderName = membership.getString("displayName") ?: identity.displayName
            val senderUsername = membership.getString("username") ?: identity.username
            val senderAvatar = membership.getString("photoUrl") ?: identity.photoUrl
            val data = mapOf(
                "roomId" to roomId,
                // Identity is stamped from the authenticated account + persisted membership.
                "senderId" to uid,
                "senderName" to senderName,
                "senderUsername" to senderUsername,
                "senderAvatar" to senderAvatar,
                "message" to safeText,
                "clientMessageId" to clientMessageId,
                "timestamp" to FieldValue.serverTimestamp(),
                "timestampMillis" to now,
                "isGiftAnnouncement" to false,
                "isSystemMessage" to false,
                "isPinned" to false,
                "eventType" to null
            )
            messageRef.set(data).awaitTask()
            LiveOperationResult.Success(
                ChatMessage(
                    id = clientMessageId,
                    roomId = roomId,
                    senderId = uid,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    senderUsername = senderUsername,
                    message = safeText,
                    timestamp = now,
                    clientMessageId = clientMessageId
                )
            )
        } catch (e: Exception) {
            mapException(e, "Failed to send the message")
        }
    }

    override suspend fun loadOlderMessages(
        roomId: String,
        beforeTimestampMillis: Long,
        limit: Int
    ): LiveOperationResult<List<ChatMessage>> {
        if (currentUid() == null) {
            return LiveOperationResult.Error("Sign in to read chat.", isAuthError = true)
        }
        return try {
            val snapshot = messagesRef(roomId)
                .orderBy("timestampMillis", Query.Direction.DESCENDING)
                .whereLessThan("timestampMillis", beforeTimestampMillis)
                .limit(limit.toLong())
                .get()
                .awaitTask()
            val items = snapshot.documents.mapNotNull { document ->
                documentToChatMessage(roomId, document.id, document.data.orEmpty())
            }.sortedWith(compareBy({ it.timestamp }, { it.id }))
            LiveOperationResult.Success(items)
        } catch (e: Exception) {
            mapException(e, "Failed to load older messages")
        }
    }

    override suspend fun sendSystemEvent(
        roomId: String,
        message: ChatMessage
    ): LiveOperationResult<ChatMessage> {
        val clientMsgId = message.clientMessageId ?: message.id
        return try {
            val messageRef = messagesRef(roomId).document(clientMsgId)
            val existing = messageRef.get().awaitTask()
            if (existing.exists()) {
                return LiveOperationResult.Success(documentToChatMessage(roomId, clientMsgId, existing.data.orEmpty()))
            }
            val data = mapOf(
                "roomId" to roomId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderUsername" to message.senderUsername,
                "senderAvatar" to message.senderAvatar,
                "message" to message.message,
                "clientMessageId" to clientMsgId,
                "timestamp" to FieldValue.serverTimestamp(),
                "timestampMillis" to message.timestamp,
                "isGiftAnnouncement" to message.isGiftAnnouncement,
                "isSystemMessage" to message.isSystemMessage,
                "isPinned" to message.isPinned,
                "eventType" to message.eventType
            )
            messageRef.set(data).awaitTask()
            LiveOperationResult.Success(message)
        } catch (e: Exception) {
            mapException(e, "Failed to send system event")
        }
    }

    // ---------------------------------------------------------------------------------------
    // Mapping helpers
    // ---------------------------------------------------------------------------------------

    private fun documentToRoom(roomId: String, data: Map<String, Any?>): LiveRoom {
        val hostUid = data["hostUserId"] as? String ?: data["creatorId"] as? String ?: ""
        val hostName = data["creatorName"] as? String ?: data["hostName"] as? String ?: "WorkDaily Host"
        val hostAvatar = data["creatorAvatar"] as? String ?: data["hostAvatar"] as? String ?: ""
        val type = runCatching {
            LiveRoomType.valueOf(data["type"] as? String ?: LiveRoomType.VIDEO.name)
        }.getOrDefault(LiveRoomType.VIDEO)
        val endedAtMillis = (data["endedAtMillis"] as? Number)?.toLong()
        return LiveRoom(
            id = roomId,
            title = data["title"] as? String ?: "WorkDaily Live",
            creatorId = hostUid,
            creatorName = hostName,
            creatorAvatar = hostAvatar,
            creatorProfession = data["creatorProfession"] as? String ?: "Site Worker",
            tradeRole = data["tradeRole"] as? String ?: "Site Worker",
            category = data["category"] as? String ?: "Site Safety",
            type = type,
            viewerCount = (data["viewerCount"] as? Number)?.toInt() ?: 0,
            peakViewerCount = (data["peakViewerCount"] as? Number)?.toInt() ?: 0,
            diamondEarnings = (data["diamondEarnings"] as? Number)?.toInt() ?: 0,
            likeCount = (data["likeCount"] as? Number)?.toInt() ?: 0,
            isLive = data["isLive"] as? Boolean ?: false,
            isHost = false,
            hostUserId = hostUid,
            tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
            description = data["description"] as? String ?: "",
            isPublic = data["isPublic"] as? Boolean ?: true,
            allowGuestRequests = data["allowGuestRequests"] as? Boolean ?: true,
            showGifts = data["showGifts"] as? Boolean ?: true,
            isChatEnabled = data["isChatEnabled"] as? Boolean ?: true,
            startTimeMillis = (data["startTimeMillis"] as? Number)?.toLong() ?: clock(),
            statusText = data["statusText"] as? String ?: if (data["isLive"] == true) "LIVE" else "ENDED",
            youtubeVideoId = data["youtubeVideoId"] as? String,
            youtubeVideoUrl = data["youtubeVideoUrl"] as? String,
            youtubePlaylistId = data["youtubePlaylistId"] as? String,
            hostUsername = data["hostUsername"] as? String ?: "",
            hostIsVerified = data["hostIsVerified"] as? Boolean ?: false,
            endedAtMillis = endedAtMillis
        )
    }

    private fun documentToMember(uid: String, data: Map<String, Any?>): LiveRoomMember {
        val role = runCatching {
            LiveMemberRole.valueOf(data["role"] as? String ?: LiveMemberRole.VIEWER.name)
        }.getOrDefault(LiveMemberRole.VIEWER)
        val lastSeen = (data["lastSeenAtMillis"] as? Number)?.toLong() ?: 0L
        return LiveRoomMember(
            uid = uid,
            displayName = data["displayName"] as? String ?: "WorkDaily Member",
            username = data["username"] as? String ?: "",
            photoUrl = data["photoUrl"] as? String ?: "",
            profession = data["profession"] as? String ?: "",
            isVerified = data["isVerified"] as? Boolean ?: false,
            role = role,
            joinedAtMillis = (data["joinedAtMillis"] as? Number)?.toLong() ?: lastSeen,
            lastSeenAtMillis = lastSeen
        )
    }

    private fun memberToMap(member: LiveRoomMember): Map<String, Any?> = mapOf(
        "uid" to member.uid,
        "displayName" to member.displayName,
        "username" to member.username,
        "photoUrl" to member.photoUrl,
        "profession" to member.profession,
        "isVerified" to member.isVerified,
        "role" to member.role.name,
        "joinedAtMillis" to member.joinedAtMillis,
        "joinedAt" to FieldValue.serverTimestamp(),
        "lastSeenAtMillis" to member.lastSeenAtMillis,
        "lastSeenAt" to FieldValue.serverTimestamp()
    )

    private fun stateToMap(state: WatchPartyState): Map<String, Any?> = mapOf(
        "roomId" to state.roomId,
        "hostUserId" to state.hostUserId,
        "videoId" to state.videoId,
        "playlistId" to state.playlistId,
        "activePlaylistItemId" to state.activePlaylistItemId,
        "status" to state.status.name,
        "positionSeconds" to state.positionSeconds,
        "revision" to state.revision,
        "updatedByUserId" to state.updatedByUserId,
        "commandId" to state.commandId,
        "isMuted" to state.isMuted,
        "isTurnedOff" to state.isTurnedOff,
        "updatedAtMillis" to state.updatedAtMillis,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun documentToWatchParty(roomId: String, data: Map<String, Any?>): WatchPartyState {
        val status = runCatching {
            WatchPartyPlaybackStatus.valueOf(
                data["status"] as? String ?: WatchPartyPlaybackStatus.UNSTARTED.name
            )
        }.getOrDefault(WatchPartyPlaybackStatus.UNSTARTED)
        val serverMillis = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
        return WatchPartyState(
            roomId = roomId,
            videoId = data["videoId"] as? String ?: "",
            playlistId = data["playlistId"] as? String,
            status = status,
            positionSeconds = (data["positionSeconds"] as? Number)?.toFloat() ?: 0f,
            updatedAtMillis = (data["updatedAtMillis"] as? Number)?.toLong() ?: clock(),
            hostUserId = data["hostUserId"] as? String ?: "",
            revision = (data["revision"] as? Number)?.toLong() ?: 0L,
            playlist = emptyList(),
            activePlaylistItemId = data["activePlaylistItemId"] as? String,
            isMuted = data["isMuted"] as? Boolean ?: false,
            isTurnedOff = data["isTurnedOff"] as? Boolean ?: false,
            updatedByUserId = data["updatedByUserId"] as? String ?: "",
            commandId = data["commandId"] as? String ?: "",
            serverUpdatedAtMillis = serverMillis
        )
    }

    private fun itemToMap(item: WatchPartyPlaylistItem): Map<String, Any?> = mapOf(
        "id" to item.id,
        "videoId" to item.videoId,
        "videoUrl" to item.videoUrl,
        "title" to item.title,
        "thumbnailUrl" to item.thumbnailUrl,
        "addedByUserId" to item.addedByUserId,
        "addedByUserName" to item.addedByUserName,
        "addedAtMillis" to item.addedAtMillis,
        "addedAt" to FieldValue.serverTimestamp()
    )

    private fun documentToPlaylistItem(id: String, data: Map<String, Any?>): WatchPartyPlaylistItem =
        WatchPartyPlaylistItem(
            id = data["id"] as? String ?: id,
            videoId = data["videoId"] as? String ?: "",
            videoUrl = data["videoUrl"] as? String ?: "",
            title = data["title"] as? String ?: "YouTube video",
            thumbnailUrl = data["thumbnailUrl"] as? String ?: "",
            addedByUserId = data["addedByUserId"] as? String ?: "",
            addedByUserName = data["addedByUserName"] as? String ?: "",
            addedAtMillis = (data["addedAtMillis"] as? Number)?.toLong() ?: 0L
        )

    private fun documentToChatMessage(roomId: String, id: String, data: Map<String, Any?>): ChatMessage {
        val serverMillis = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
        return ChatMessage(
            id = id,
            roomId = roomId,
            senderId = data["senderId"] as? String ?: "",
            senderName = data["senderName"] as? String ?: "WorkDaily Member",
            senderAvatar = data["senderAvatar"] as? String ?: "",
            senderUsername = data["senderUsername"] as? String ?: "",
            message = data["message"] as? String ?: "",
            isGiftAnnouncement = data["isGiftAnnouncement"] as? Boolean ?: false,
            giftName = data["giftName"] as? String,
            giftDiamondCost = (data["giftDiamondCost"] as? Number)?.toInt(),
            isSystemMessage = data["isSystemMessage"] as? Boolean ?: false,
            isPinned = data["isPinned"] as? Boolean ?: false,
            timestamp = serverMillis
                ?: (data["timestampMillis"] as? Number)?.toLong()
                ?: clock(),
            eventType = data["eventType"] as? String,
            clientMessageId = data["clientMessageId"] as? String
        )
    }

    private fun isSafeDocumentId(clientMessageId: String): Boolean =
        clientMessageId.isNotBlank() && clientMessageId.length <= LiveChatPolicy.CLIENT_MESSAGE_ID_LENGTH + 8 &&
            clientMessageId.all { it.isLetterOrDigit() || it == '_' || it == '-' }

    private class LiveRoomFailure(
        message: String,
        val isAuthError: Boolean = false
    ) : Exception(message)

    private class LiveRoomConflict(message: String) : Exception(message)

    private fun <T> mapException(exception: Exception, fallback: String): LiveOperationResult<T> {
        val cause: Throwable = if (exception is java.util.concurrent.ExecutionException) {
            exception.cause ?: exception
        } else {
            exception
        }
        if (cause is LiveRoomConflict) {
            return LiveOperationResult.Error(cause.message ?: fallback, isConflict = true, cause = cause)
        }
        if (cause is LiveRoomFailure) {
            return LiveOperationResult.Error(cause.message ?: fallback, isAuthError = cause.isAuthError, cause = cause)
        }
        if (cause is FirebaseFirestoreException) {
            return when (cause.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    LiveOperationResult.Error(
                        "Your account is not allowed to do that in this room.",
                        isAuthError = true,
                        cause = cause
                    )

                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.ABORTED,
                FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                    LiveOperationResult.Error(
                        "Network is unstable. Please retry.",
                        isTransient = true,
                        cause = cause
                    )

                else -> LiveOperationResult.Error(cause.message ?: fallback, cause = cause)
            }
        }
        return LiveOperationResult.Error(cause.message ?: fallback, cause = cause)
    }
}
