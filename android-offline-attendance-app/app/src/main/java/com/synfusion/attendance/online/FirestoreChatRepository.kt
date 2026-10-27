package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

sealed class ChatMessagesFeedState {
    object Loading : ChatMessagesFeedState()
    data class Loaded(
        val messages: List<FirestoreChatMessage>,
        val hasMoreOlder: Boolean = false,
        val isFromCache: Boolean = false
    ) : ChatMessagesFeedState()
    data class Error(val message: String) : ChatMessagesFeedState()
}

interface ChatRepository {
    fun getConversationId(uid1: String, uid2: String): String
    suspend fun getOrCreateConversation(currentUid: String, friendUid: String): ProfileOperationResult<FirestoreConversation>
    fun observeConversations(currentUid: String): Flow<List<FirestoreConversation>>
    fun observeMessages(conversationId: String, limit: Int = 50): Flow<List<FirestoreChatMessage>>
    fun observeMessagesFeed(conversationId: String, limit: Int = 40): Flow<ChatMessagesFeedState> {
        return observeMessages(conversationId, limit).map { list ->
            ChatMessagesFeedState.Loaded(
                messages = list,
                hasMoreOlder = list.size >= limit,
                isFromCache = false
            )
        }
    }
    suspend fun sendMessage(senderUid: String, recipientUid: String, text: String): ProfileOperationResult<FirestoreChatMessage>
    suspend fun markMessagesAsDelivered(conversationId: String, recipientUid: String): ProfileOperationResult<Boolean>
    suspend fun markMessagesAsRead(conversationId: String, recipientUid: String): ProfileOperationResult<Boolean>
}

class FirestoreChatRepository(
    private val firestoreSupplier: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authSupplier: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val followRepository: FollowRepository = FirestoreFollowRepository(),
    private val blockRepository: BlockRepository = FirestoreBlockRepository()
) : ChatRepository {

    @Volatile
    private var verifiedOwnerUid: String? = null
    private val verifiedConversationIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private fun ensureVerifiedScope(uid: String) {
        if (verifiedOwnerUid != uid) {
            verifiedConversationIds.clear()
            verifiedOwnerUid = uid
        }
    }

    fun clearVerifiedCache() {
        verifiedConversationIds.clear()
        verifiedOwnerUid = null
    }

    override fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    private suspend fun checkChatEligibility(
        uidA: String,
        uidB: String,
        blockedMessage: String,
        notMutualMessage: String
    ): String? = coroutineScope {
        val blockedDeferred = async { blockRepository.isEitherBlocked(uidA, uidB) }
        val mutualDeferred = async { followRepository.isMutual(uidA, uidB) }
        if (blockedDeferred.await()) {
            mutualDeferred.cancel()
            blockedMessage
        } else if (!mutualDeferred.await()) {
            notMutualMessage
        } else {
            null
        }
    }

    private suspend fun ensureConversationDocument(
        currentUid: String,
        friendUid: String,
        forceServerCheck: Boolean = false
    ): FirestoreConversation {
        ensureVerifiedScope(currentUid)
        val convId = getConversationId(currentUid, friendUid)
        val sortedParticipants = listOf(currentUid, friendUid).sorted()

        if (!forceServerCheck && verifiedConversationIds.contains(convId)) {
            val now = System.currentTimeMillis()
            return FirestoreConversation(
                id = convId,
                participants = sortedParticipants,
                createdAt = now,
                updatedAt = now
            )
        }

        val firestore = firestoreSupplier()
        val convRef = firestore.collection("conversations").document(convId)
        val snap = convRef.get().await()

        if (snap.exists()) {
            verifiedConversationIds.add(convId)
            val data = snap.data
            val participants = (data?.get("participants") as? List<*>)?.mapNotNull { it as? String }
                ?: sortedParticipants
            return FirestoreConversation(
                id = convId,
                participants = participants,
                lastMessage = data?.get("lastMessage") as? String ?: "",
                lastSenderUid = data?.get("lastSenderUid") as? String ?: "",
                lastTimestamp = (data?.get("lastTimestamp") as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdAt = (data?.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data?.get("updatedAt") as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        val now = System.currentTimeMillis()
        val newConv = FirestoreConversation(
            id = convId,
            participants = sortedParticipants,
            lastMessage = "",
            lastSenderUid = "",
            lastTimestamp = now,
            createdAt = now,
            updatedAt = now
        )

        val convData = mapOf(
            "id" to convId,
            "participants" to sortedParticipants,
            "lastMessage" to "",
            "lastSenderUid" to "",
            "lastTimestamp" to now,
            "createdAt" to now,
            "updatedAt" to now
        )

        convRef.set(convData).await()
        verifiedConversationIds.add(convId)
        return newConv
    }

    override suspend fun getOrCreateConversation(
        currentUid: String,
        friendUid: String
    ): ProfileOperationResult<FirestoreConversation> = withContext(Dispatchers.IO) {
        if (currentUid.isBlank() || friendUid.isBlank() || currentUid == friendUid) {
            return@withContext ProfileOperationResult.Error("Invalid conversation participants.")
        }

        val eligibilityError = checkChatEligibility(
            uidA = currentUid,
            uidB = friendUid,
            blockedMessage = "Messaging blocked due to user restriction.",
            notMutualMessage = "Friends-only messaging requires mutual follow."
        )
        if (eligibilityError != null) {
            return@withContext ProfileOperationResult.Error(eligibilityError)
        }

        try {
            val conversation = ensureConversationDocument(currentUid, friendUid, forceServerCheck = true)
            ProfileOperationResult.Success(conversation)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to get or create conversation", cause = e)
        }
    }

    override fun observeConversations(currentUid: String): Flow<List<FirestoreConversation>> = callbackFlow {
        if (currentUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val firestore = firestoreSupplier()
        val query = firestore.collection("conversations")
            .whereArrayContains("participants", currentUid)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                ensureVerifiedScope(currentUid)
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    verifiedConversationIds.add(doc.id)
                    val participants = (data["participants"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    FirestoreConversation(
                        id = doc.id,
                        participants = participants,
                        lastMessage = data["lastMessage"] as? String ?: "",
                        lastSenderUid = data["lastSenderUid"] as? String ?: "",
                        lastTimestamp = (data["lastTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }

    override fun observeMessagesFeed(
        conversationId: String,
        limit: Int
    ): Flow<ChatMessagesFeedState> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(ChatMessagesFeedState.Loaded(emptyList(), hasMoreOlder = false, isFromCache = false))
            close()
            return@callbackFlow
        }

        val boundedLimit = limit.coerceAtLeast(1)
        val firestore = firestoreSupplier()
        // Query DESCENDING + limit so we always load the LATEST messages first, then order ASCENDING for display.
        val query = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(boundedLimit.toLong())

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Note: if conversation doc doesn't exist yet before the first message is sent,
                // Firestore rules `get(/conversations/$(conversationId)).data.participants` can fail
                // with PERMISSION_DENIED on a brand-new empty conversation. Handle real errors vs brand-new empty doc cleanly.
                val msg = error.message ?: "Failed to load messages"
                trySend(ChatMessagesFeedState.Error(msg))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val activeUid = try { authSupplier().currentUser?.uid } catch (_: Exception) { null }
                if (!activeUid.isNullOrBlank() && snapshot.documents.isNotEmpty()) {
                    ensureVerifiedScope(activeUid)
                    verifiedConversationIds.add(conversationId)
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val statusStr = data["status"] as? String ?: MessageStatus.SENT.name
                    val status = try { MessageStatus.valueOf(statusStr) } catch (_: Exception) { MessageStatus.SENT }

                    FirestoreChatMessage(
                        id = doc.id,
                        conversationId = conversationId,
                        senderUid = data["senderUid"] as? String ?: "",
                        recipientUid = data["recipientUid"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        status = status,
                        deliveredAt = (data["deliveredAt"] as? Number)?.toLong(),
                        readAt = (data["readAt"] as? Number)?.toLong()
                    )
                }.sortedWith(compareBy<FirestoreChatMessage> { it.timestamp }.thenBy { it.id })

                trySend(
                    ChatMessagesFeedState.Loaded(
                        messages = messages,
                        hasMoreOlder = snapshot.documents.size >= boundedLimit,
                        isFromCache = snapshot.metadata.isFromCache
                    )
                )
            }
        }

        awaitClose { registration.remove() }
    }

    override fun observeMessages(
        conversationId: String,
        limit: Int
    ): Flow<List<FirestoreChatMessage>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val boundedLimit = limit.coerceAtLeast(1)
        val firestore = firestoreSupplier()
        val query = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(boundedLimit.toLong())

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val statusStr = data["status"] as? String ?: MessageStatus.SENT.name
                    val status = try { MessageStatus.valueOf(statusStr) } catch (_: Exception) { MessageStatus.SENT }

                    FirestoreChatMessage(
                        id = doc.id,
                        conversationId = conversationId,
                        senderUid = data["senderUid"] as? String ?: "",
                        recipientUid = data["recipientUid"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
                        status = status,
                        deliveredAt = (data["deliveredAt"] as? Number)?.toLong(),
                        readAt = (data["readAt"] as? Number)?.toLong()
                    )
                }.sortedWith(compareBy<FirestoreChatMessage> { it.timestamp }.thenBy { it.id })

                trySend(messages)
            }
        }

        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(
        senderUid: String,
        recipientUid: String,
        text: String
    ): ProfileOperationResult<FirestoreChatMessage> = withContext(Dispatchers.IO) {
        val trimmedText = text.trim()
        if (senderUid.isBlank() || recipientUid.isBlank() || trimmedText.isEmpty()) {
            return@withContext ProfileOperationResult.Error("Sender, recipient, and message text must not be empty.")
        }

        val eligibilityError = checkChatEligibility(
            uidA = senderUid,
            uidB = recipientUid,
            blockedMessage = "Cannot send message: User interaction is blocked.",
            notMutualMessage = "Cannot send message: Users are not mutual friends."
        )
        if (eligibilityError != null) {
            return@withContext ProfileOperationResult.Error(eligibilityError)
        }

        try {
            val conv = ensureConversationDocument(senderUid, recipientUid, forceServerCheck = false)
            val convId = conv.id
            val firestore = firestoreSupplier()
            val msgRef = firestore.collection("conversations").document(convId).collection("messages").document()
            val now = System.currentTimeMillis()

            val chatMsg = FirestoreChatMessage(
                id = msgRef.id,
                conversationId = convId,
                senderUid = senderUid,
                recipientUid = recipientUid,
                text = trimmedText,
                timestamp = now,
                status = MessageStatus.SENT
            )

            val msgData = mapOf(
                "id" to msgRef.id,
                "conversationId" to convId,
                "senderUid" to senderUid,
                "recipientUid" to recipientUid,
                "text" to trimmedText,
                "timestamp" to now,
                "status" to MessageStatus.SENT.name,
                "deliveredAt" to null,
                "readAt" to null
            )

            val convRef = firestore.collection("conversations").document(convId)
            val convUpdate = mapOf(
                "lastMessage" to trimmedText,
                "lastSenderUid" to senderUid,
                "lastTimestamp" to now,
                "updatedAt" to now
            )

            try {
                firestore.runBatch { batch ->
                    batch.set(msgRef, msgData)
                    batch.update(convRef, convUpdate)
                }.await()
            } catch (_: Exception) {
                // Ensure conversation doc exists on server if verified cache was stale, then commit once
                ensureConversationDocument(senderUid, recipientUid, forceServerCheck = true)
                firestore.runBatch { batch ->
                    batch.set(msgRef, msgData)
                    batch.update(convRef, convUpdate)
                }.await()
            }

            ProfileOperationResult.Success(chatMsg)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to send message", cause = e)
        }
    }

    override suspend fun markMessagesAsDelivered(
        conversationId: String,
        recipientUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (conversationId.isBlank() || recipientUid.isBlank()) {
            return@withContext ProfileOperationResult.Success(false)
        }

        try {
            val firestore = firestoreSupplier()
            val querySnap = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("recipientUid", recipientUid)
                .whereEqualTo("status", MessageStatus.SENT.name)
                .get()
                .await()

            if (querySnap.documents.isEmpty()) {
                return@withContext ProfileOperationResult.Success(true)
            }

            val now = System.currentTimeMillis()
            firestore.runBatch { batch ->
                for (doc in querySnap.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "status" to MessageStatus.DELIVERED.name,
                            "deliveredAt" to now
                        )
                    )
                }
            }.await()

            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to mark messages delivered", cause = e)
        }
    }

    override suspend fun markMessagesAsRead(
        conversationId: String,
        recipientUid: String
    ): ProfileOperationResult<Boolean> = withContext(Dispatchers.IO) {
        if (conversationId.isBlank() || recipientUid.isBlank()) {
            return@withContext ProfileOperationResult.Success(false)
        }

        try {
            val firestore = firestoreSupplier()
            val querySnap = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("recipientUid", recipientUid)
                .whereIn("status", listOf(MessageStatus.SENT.name, MessageStatus.DELIVERED.name))
                .get()
                .await()

            if (querySnap.documents.isEmpty()) {
                return@withContext ProfileOperationResult.Success(true)
            }

            val now = System.currentTimeMillis()
            firestore.runBatch { batch ->
                for (doc in querySnap.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "status" to MessageStatus.READ.name,
                            "readAt" to now
                        )
                    )
                }
            }.await()

            ProfileOperationResult.Success(true)
        } catch (e: Exception) {
            ProfileOperationResult.Error(e.message ?: "Failed to mark messages read", cause = e)
        }
    }
}
