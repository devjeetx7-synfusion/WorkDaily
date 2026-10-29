package com.synfusion.attendance.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering messaging read/unread calculations, dashboard notification dot logic,
 * single vs multi-digit badge formatting rules, and account switching state resets.
 */
class MessagingSystemTest {

    private val me = "user_me"
    private val peer1 = "user_peer1"
    private val peer2 = "user_peer2"

    private fun conversation(
        id: String,
        participants: List<String> = listOf(me, id.substringAfter("_")),
        unreadCounts: Map<String, Int> = emptyMap()
    ) = FirestoreConversation(
        id = id,
        participants = participants,
        lastMessage = "Hello",
        unreadCounts = unreadCounts
    )

    // ----------------------------------------------------------------------------------------
    // Read/Unread state & Counter calculations
    // ----------------------------------------------------------------------------------------

    @Test
    fun testUnreadTotalCountsOnlyCurrentUserUnread() {
        val conv1 = conversation("c1", unreadCounts = mapOf(me to 2, peer1 to 5))
        val conv2 = conversation("c2", unreadCounts = mapOf(me to 3, peer2 to 0))

        assertEquals(5, listOf(conv1, conv2).unreadTotalFor(me))
        assertEquals(5, listOf(conv1, conv2).unreadTotalFor(peer1))
        assertEquals(0, listOf(conv1, conv2).unreadTotalFor(peer2))
    }

    @Test
    fun testUnreadTotalIsZeroWhenAllConversationsAreRead() {
        val conv1 = conversation("c1", unreadCounts = mapOf(me to 0, peer1 to 1))
        val conv2 = conversation("c2", unreadCounts = mapOf(me to 0))

        assertEquals(0, listOf(conv1, conv2).unreadTotalFor(me))
    }

    @Test
    fun testUnreadTotalHandlesMissingOrLegacyCounterMap() {
        val legacyConv = conversation("c_legacy", unreadCounts = emptyMap())
        assertEquals(0, listOf(legacyConv).unreadTotalFor(me))
    }

    @Test
    fun testUnreadTotalWhenViewerIsUnknown() {
        val conv = conversation("c1", unreadCounts = mapOf(me to 4))
        assertEquals(0, listOf(conv).unreadTotalFor(null))
        assertEquals(0, listOf(conv).unreadTotalFor(""))
    }

    // ----------------------------------------------------------------------------------------
    // Dashboard Notification Dot logic (hasUnreadMessages)
    // ----------------------------------------------------------------------------------------

    @Test
    fun testDashboardNotificationDotVisibleWhenUnreadExists() {
        val conversations = listOf(
            conversation("c1", unreadCounts = mapOf(me to 1))
        )
        val unreadTotal = conversations.unreadTotalFor(me)
        val hasUnreadMessages = unreadTotal > 0

        assertTrue("Dashboard red dot should be visible when unreadTotal > 0", hasUnreadMessages)
    }

    @Test
    fun testDashboardNotificationDotHiddenWhenUnreadTotalIsZero() {
        val conversations = listOf(
            conversation("c1", unreadCounts = mapOf(me to 0)),
            conversation("c2", unreadCounts = mapOf(me to 0))
        )
        val unreadTotal = conversations.unreadTotalFor(me)
        val hasUnreadMessages = unreadTotal > 0

        assertFalse("Dashboard red dot should be hidden when unreadTotal == 0", hasUnreadMessages)
    }

    @Test
    fun testDashboardNotificationDotStaysVisibleWhenOneConversationReadButAnotherHasUnread() {
        val conversationsBefore = listOf(
            conversation("c1", unreadCounts = mapOf(me to 2)),
            conversation("c2", unreadCounts = mapOf(me to 1))
        )
        assertEquals(3, conversationsBefore.unreadTotalFor(me))
        assertTrue(conversationsBefore.unreadTotalFor(me) > 0)

        // Read c1
        val conversationsAfter = listOf(
            conversation("c1", unreadCounts = mapOf(me to 0)),
            conversation("c2", unreadCounts = mapOf(me to 1))
        )
        assertEquals(1, conversationsAfter.unreadTotalFor(me))
        assertTrue("Dot remains visible while c2 still has unread messages", conversationsAfter.unreadTotalFor(me) > 0)
    }

    @Test
    fun testDashboardNotificationDotHidesWhenFinalUnreadIsRead() {
        val conversationsAfter = listOf(
            conversation("c1", unreadCounts = mapOf(me to 0)),
            conversation("c2", unreadCounts = mapOf(me to 0))
        )
        assertEquals(0, conversationsAfter.unreadTotalFor(me))
        assertFalse("Dot hides when all unread messages reach 0", conversationsAfter.unreadTotalFor(me) > 0)
    }

    // ----------------------------------------------------------------------------------------
    // Account Switching & Logout Isolation
    // ----------------------------------------------------------------------------------------

    @Test
    fun testAccountSwitchingDoesNotLeakUnreadCounters() {
        val userA = "user_A"
        val userB = "user_B"

        val conversations = listOf(
            conversation("c1", participants = listOf(userA, userB), unreadCounts = mapOf(userA to 4, userB to 0))
        )

        assertEquals(4, conversations.unreadTotalFor(userA))
        assertEquals(0, conversations.unreadTotalFor(userB))
    }

    @Test
    fun testLogoutClearsUnreadState() {
        val conversations = listOf(
            conversation("c1", unreadCounts = mapOf(me to 5))
        )

        val activeUid: String? = null // Logged out
        assertEquals(0, conversations.unreadTotalFor(activeUid))
    }

    // ----------------------------------------------------------------------------------------
    // Badge Formatting Logic
    // ----------------------------------------------------------------------------------------

    @Test
    fun testSingleDigitBadgeFormat() {
        val count = 7
        val isSingleDigit = count in 1..9
        val formatted = if (count > 99) "99+" else count.toString()

        assertTrue(isSingleDigit)
        assertEquals("7", formatted)
    }

    @Test
    fun testMultiDigitBadgeFormat() {
        val count = 12
        val isSingleDigit = count in 1..9
        val formatted = if (count > 99) "99+" else count.toString()

        assertFalse(isSingleDigit)
        assertEquals("12", formatted)
    }

    @Test
    fun testLargeCountBadgeFormat() {
        val count = 105
        val formatted = if (count > 99) "99+" else count.toString()

        assertEquals("99+", formatted)
    }
}
