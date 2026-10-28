package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.synfusion.attendance.live.model.ChatMessage
import com.synfusion.attendance.live.model.WatchPartyPlaybackStatus
import com.synfusion.attendance.live.model.WatchPartyState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreLiveRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) {

    fun observeWatchPartyState(roomId: String): Flow<WatchPartyState?> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val firestore = try { firestoreSupplier() } catch (_: Exception) { null }
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("live_rooms").document(roomId)

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }

            val data = snapshot.data ?: return@addSnapshotListener
            val wpMap = data["watchPartyState"] as? Map<*, *>
            if (wpMap != null) {
                val statusStr = wpMap["status"] as? String ?: WatchPartyPlaybackStatus.UNSTARTED.name
                val status = try { WatchPartyPlaybackStatus.valueOf(statusStr) } catch (_: Exception) { WatchPartyPlaybackStatus.UNSTARTED }
                val state = WatchPartyState(
                    roomId = wpMap["roomId"] as? String ?: roomId,
                    videoId = wpMap["videoId"] as? String ?: "",
                    playlistId = wpMap["playlistId"] as? String,
                    status = status,
                    positionSeconds = (wpMap["positionSeconds"] as? Number)?.toFloat() ?: 0f,
                    updatedAtMillis = (wpMap["updatedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    hostUserId = wpMap["hostUserId"] as? String ?: "",
                    revision = (wpMap["revision"] as? Number)?.toLong() ?: 0L
                )
                trySend(state)
            } else {
                trySend(null)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun publishWatchPartyState(roomId: String, state: WatchPartyState): ProfileOperationResult<Boolean> {
        if (roomId.isBlank()) return ProfileOperationResult.Error("Room ID cannot be empty.")
        val currentUser = try { authSupplier().currentUser } catch (_: Exception) { null }
            ?: return ProfileOperationResult.Error("Authentication required to publish watch party state.")

        return try {
            val firestore = firestoreSupplier()
            val docRef = firestore.collection("live_rooms").document(roomId)

            val stateMap = mapOf(
                "roomId" to state.roomId,
                "videoId" to state.videoId,
                "playlistId" to state.playlistId,
                "status" to state.status.name,
                "positionSeconds" to state.positionSeconds,
                "updatedAtMillis" to state.updatedAtMillis,
                "hostUserId" to currentUser.uid,
                "revision" to state.revision
            )

            docRef.set(
                mapOf("watchPartyState" to stateMap, "hostUserId" to currentUser.uid),
                SetOptions.merge()
            ).await()
            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to publish watch party state", cause = e)
        }
    }

    fun observeLiveChatMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val firestore = try { firestoreSupplier() } catch (_: Exception) { null }
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = firestore.collection("live_rooms")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(50)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ChatMessage(
                    id = doc.id,
                    roomId = roomId,
                    senderId = data["senderId"] as? String ?: "",
                    senderName = data["senderName"] as? String ?: "Worker",
                    senderAvatar = data["senderAvatar"] as? String ?: "",
                    message = data["message"] as? String ?: "",
                    isGiftAnnouncement = (data["isGiftAnnouncement"] as? Boolean) ?: false,
                    giftName = data["giftName"] as? String,
                    giftDiamondCost = (data["giftDiamondCost"] as? Number)?.toInt(),
                    isSystemMessage = (data["isSystemMessage"] as? Boolean) ?: false,
                    isPinned = (data["isPinned"] as? Boolean) ?: false,
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    eventType = data["eventType"] as? String
                )
            }
            trySend(list)
        }

        awaitClose { registration.remove() }
    }

    suspend fun sendLiveChatMessage(roomId: String, message: ChatMessage): ProfileOperationResult<Boolean> {
        if (roomId.isBlank() || message.message.isBlank()) {
            return ProfileOperationResult.Error("Room ID and message must not be empty.")
        }

        val currentUser = try { authSupplier().currentUser } catch (_: Exception) { null }
            ?: return ProfileOperationResult.Error("Authentication required to send chat message.")

        return try {
            val firestore = firestoreSupplier()
            val msgRef = firestore.collection("live_rooms").document(roomId).collection("messages").document()

            val msgData = mapOf(
                "id" to msgRef.id,
                "roomId" to roomId,
                "senderId" to currentUser.uid,
                "senderName" to message.senderName,
                "senderAvatar" to message.senderAvatar,
                "message" to message.message,
                "isGiftAnnouncement" to message.isGiftAnnouncement,
                "giftName" to message.giftName,
                "giftDiamondCost" to message.giftDiamondCost,
                "isSystemMessage" to message.isSystemMessage,
                "isPinned" to message.isPinned,
                "timestamp" to message.timestamp,
                "eventType" to message.eventType
            )

            msgRef.set(msgData).await()
            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to send chat message", cause = e)
        }
    }
}
