package com.synfusion.attendance.live.watchparty

import com.synfusion.attendance.live.model.ChatMessage

/** Why a chat message cannot be sent. */
enum class ChatSendBlock { ROOM_ENDED, CHAT_DISABLED, NOT_A_MEMBER, BLOCKED, EMPTY, TOO_LONG }

sealed class ChatValidation {
    data class Valid(val text: String) : ChatValidation()
    data class Invalid(val block: ChatSendBlock, val message: String) : ChatValidation()
}

/**
 * Client-side mirror of the chat rules enforced by Firestore.
 *
 * Sender identity is never taken from the message payload: the repository always stamps
 * `senderId`/`senderName`/`senderAvatar` from the authenticated Firebase user and their
 * `public_profiles` document, and the security rules reject a mismatch.
 */
object LiveChatPolicy {

    const val MAX_MESSAGE_LENGTH = 500
    const val CLIENT_MESSAGE_ID_LENGTH = 40
    const val HISTORY_PAGE_SIZE = 60
    const val MAX_RENDERED_MESSAGES = 240

    fun validateSend(
        rawText: String,
        isRoomLive: Boolean,
        isChatEnabled: Boolean,
        isMember: Boolean,
        isBlocked: Boolean
    ): ChatValidation {
        if (!isRoomLive) {
            return ChatValidation.Invalid(ChatSendBlock.ROOM_ENDED, "This live room has ended.")
        }
        if (!isChatEnabled) {
            return ChatValidation.Invalid(ChatSendBlock.CHAT_DISABLED, "The host disabled chat for this room.")
        }
        if (!isMember) {
            return ChatValidation.Invalid(ChatSendBlock.NOT_A_MEMBER, "Join the room before sending messages.")
        }
        if (isBlocked) {
            return ChatValidation.Invalid(ChatSendBlock.BLOCKED, "You cannot message this room.")
        }
        val text = sanitize(rawText)
        if (text.isEmpty()) {
            return ChatValidation.Invalid(ChatSendBlock.EMPTY, "Message cannot be empty.")
        }
        if (text.length > MAX_MESSAGE_LENGTH) {
            return ChatValidation.Invalid(
                ChatSendBlock.TOO_LONG,
                "Messages are limited to $MAX_MESSAGE_LENGTH characters."
            )
        }
        return ChatValidation.Valid(text)
    }

    /** Collapses excessive whitespace and strips control characters. */
    fun sanitize(rawText: String): String = rawText
        .filter { it == '\n' || it == '\t' || !it.isISOControl() }
        .trim()
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")

    /**
     * Merges persisted history with optimistic pending messages and removes duplicates.
     *
     * Dedupe keys are the server document id and the client message id, so a message cannot appear
     * twice when the realtime snapshot arrives while the optimistic copy is still on screen.
     * The result is sorted oldest -> newest and bounded to avoid unbounded growth.
     */
    fun mergeMessages(
        persisted: List<ChatMessage>,
        pending: List<ChatMessage>,
        limit: Int = MAX_RENDERED_MESSAGES
    ): List<ChatMessage> {
        val byKey = LinkedHashMap<String, ChatMessage>()
        (persisted + pending).forEach { message ->
            val key = message.clientMessageId?.takeIf { it.isNotBlank() } ?: message.id
            val existing = byKey[key]
            byKey[key] = when {
                existing == null -> message
                // Persisted copy always wins over an optimistic one.
                existing.isPending && !message.isPending -> message
                !existing.isPending && message.isPending -> existing
                else -> message
            }
        }
        val merged = byKey.values.sortedWith(compareBy({ it.timestamp }, { it.id }))
        return if (merged.size <= limit) merged else merged.takeLast(limit)
    }

    /** Pending send results, used to reconcile a failed optimistic message. */
    fun markFailed(messages: List<ChatMessage>, clientMessageId: String): List<ChatMessage> =
        messages.map { message ->
            if (message.clientMessageId == clientMessageId) {
                message.copy(isFailed = true, isPending = false)
            } else {
                message
            }
        }

    fun markSent(messages: List<ChatMessage>, clientMessageId: String): List<ChatMessage> =
        messages.filterNot { it.clientMessageId == clientMessageId && it.isPending }

    /** True when a message was written by the authenticated user. */
    fun isOwnMessage(message: ChatMessage, currentUid: String?): Boolean =
        currentUid != null && message.senderId == currentUid

    /**
     * A message is renderable when it has a real persisted identity. Legacy/demo messages without a
     * sender id are still displayed but are never treated as sendable history for a new room.
     */
    fun hasPersistedSender(message: ChatMessage): Boolean = message.senderId.isNotBlank()

    /** Oldest timestamp used as the pagination cursor when loading older history. */
    fun olderHistoryCursor(messages: List<ChatMessage>): Long? =
        messages.filter { !it.isPending && !it.isFailed }.minOfOrNull { it.timestamp }
}
