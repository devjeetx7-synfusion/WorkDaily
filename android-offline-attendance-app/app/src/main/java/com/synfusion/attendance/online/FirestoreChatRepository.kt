package com.synfusion.attendance.online

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface ChatRepository {
    fun getConversationId(uid1: String, uid2: String): String
    suspend fun getOrCreateConversation(currentUid: String, friendUid: String): ProfileOperationResult<FirestoreConversation>
    fun observeConversations(currentUid: String): Flow<List<FirestoreConversation>>
    fun observeMessages(conversationId: String, limit: Int = 50): Flow<List<FirestoreChatMessage>>
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

    override fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    override suspend fun getOrCreateConversation(
        currentUid: String,
        friendUid: String
    ): ProfileOperationResult<FirestoreConversation> = withContext(Dispatchers.IO) {
        if (currentUid.isBlank() || friendUid.isBlank() || currentUid == friendUid) {
            return@withContext ProfileOperationResult.Error("Invalid conversation participants.")
        }

        if (blockRepository.isEitherBlocked(currentUid, friendUid)) {
            return@withContext ProfileOperationResult.Error("Messaging blocked due to user restriction.")
        }

        if (!followRepository.isMutual(currentUid, friendUid)) {
            return@withContext ProfileOperationResult.Error("Friends-only messaging requires mutual follow.")
        }

        try {
            val firestore = firestoreSupplier()
            val convId = getConversationId(currentUid, friendUid)
            val convRef = firestore.collection("conversations").document(convId)
            val snap = convRef.get().await()

            if (snap.exists()) {
                val data = snap.data
                val participants = (data?.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: listOf(currentUid, friendUid)
                val conversation = FirestoreConversation(
                    id = convId,
                    participants = participants,
                    lastMessage = data?.get("lastMessage") as? String ?: "",
                    lastSenderUid = data?.get("lastSenderUid") as? String ?: "",
                    lastTimestamp = (data?.get("lastTimestamp") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data?.get("createdAt") as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data?.get("updatedAt") as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                return@withContext ProfileOperationResult.Success(conversation)
            }

            val now = System.currentTimeMillis()
            val sortedParticipants = listOf(currentUid, friendUid).sorted()
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
            ProfileOperationResult.Success(newConv)
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
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
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

    override fun observeMessages(
        conversationId: String,
        limit: Int
    ): Flow<List<FirestoreChatMessage>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val firestore = firestoreSupplier()
        val query = firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(limit.toLong())

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val statusStr = data["status"] as? String ?: MessageStatus.SENT.name
                    val status = try { MessageStatus.valueOf(statusStr) } catch (e: Exception) { MessageStatus.SENT }

                    FirestoreChatMessage(
                        id = doc.id,
                        conversationId = conversationId,
                        senderUid = data["senderUid"] as? String ?: "",
                        recipientUid = data["recipientUid"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        status = status,
                        deliveredAt = (data["deliveredAt"] as? Number)?.toLong(),
                        readAt = (data["readAt"] as? Number)?.toLong()
                    )
                }
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
        if (senderUid.isBlank() || recipientUid.isBlank() || text.trim().isEmpty()) {
            return@withContext ProfileOperationResult.Error("Sender, recipient, and message text must not be empty.")
        }

        if (blockRepository.isEitherBlocked(senderUid, recipientUid)) {
            return@withContext ProfileOperationResult.Error("Cannot send message: User interaction is blocked.")
        }

        if (!followRepository.isMutual(senderUid, recipientUid)) {
            return@withContext ProfileOperationResult.Error("Cannot send message: Users are not mutual friends.")
        }

        try {
            val convResult = getOrCreateConversation(senderUid, recipientUid)
            if (convResult is ProfileOperationResult.Error) {
                return@withContext ProfileOperationResult.Error(convResult.message)
            }

            val convId = (convResult as ProfileOperationResult.Success).data.id
            val firestore = firestoreSupplier()
            val msgRef = firestore.collection("conversations").document(convId).collection("messages").document()
            val now = System.currentTimeMillis()

            val chatMsg = FirestoreChatMessage(
                id = msgRef.id,
                conversationId = convId,
                senderUid = senderUid,
                recipientUid = recipientUid,
                text = text.trim(),
                timestamp = now,
                status = MessageStatus.SENT
            )

            val msgData = mapOf(
                "id" to msgRef.id,
                "conversationId" to convId,
                "senderUid" to senderUid,
                "recipientUid" to recipientUid,
                "text" to text.trim(),
                "timestamp" to now,
                "status" to MessageStatus.SENT.name,
                "deliveredAt" to null,
                "readAt" to null
            )

            val convRef = firestore.collection("conversations").document(convId)
            val convUpdate = mapOf(
                "lastMessage" to text.trim(),
                "lastSenderUid" to senderUid,
                "lastTimestamp" to now,
                "updatedAt" to now
            )

            firestore.runBatch { batch ->
                batch.set(msgRef, msgData)
                batch.update(convRef, convUpdate)
            }.await()

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
