package com.synfusion.attendance.online

import org.junit.Assert.*
import org.junit.Test

class SocialModelsTest {

    @Test
    fun testConversationIdDeterministicGeneration() {
        val repo = FirestoreChatRepository(
            firestoreSupplier = { throw IllegalStateException("Not used in unit test") },
            authSupplier = { throw IllegalStateException("Not used in unit test") }
        )

        val convId1 = repo.getConversationId("user_a", "user_b")
        val convId2 = repo.getConversationId("user_b", "user_a")

        assertEquals("user_a_user_b", convId1)
        assertEquals("user_a_user_b", convId2)
        assertEquals(convId1, convId2)
    }

    @Test
    fun testMessageStatusEnumValues() {
        assertEquals(4, MessageStatus.values().size)
        assertEquals(MessageStatus.PENDING, MessageStatus.valueOf("PENDING"))
        assertEquals(MessageStatus.SENT, MessageStatus.valueOf("SENT"))
        assertEquals(MessageStatus.DELIVERED, MessageStatus.valueOf("DELIVERED"))
        assertEquals(MessageStatus.READ, MessageStatus.valueOf("READ"))
    }

    @Test
    fun testChatMessageCreation() {
        val msg = FirestoreChatMessage(
            id = "msg_123",
            conversationId = "user_1_user_2",
            senderUid = "user_1",
            recipientUid = "user_2",
            text = "Hello WorkDaily!",
            status = MessageStatus.SENT
        )

        assertEquals("msg_123", msg.id)
        assertEquals("user_1_user_2", msg.conversationId)
        assertEquals("user_1", msg.senderUid)
        assertEquals("user_2", msg.recipientUid)
        assertEquals("Hello WorkDaily!", msg.text)
        assertEquals(MessageStatus.SENT, msg.status)
        assertNull(msg.deliveredAt)
        assertNull(msg.readAt)
    }
}
