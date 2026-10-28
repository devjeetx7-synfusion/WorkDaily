package com.synfusion.attendance.online

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the conversation filtering and unread aggregation behind the chat list.
 *
 * These are pure functions on [FirestoreConversation], so they run as plain JVM tests with no
 * Firestore and no Compose. They exist because both rules were previously inline in
 * `ChatListScreen` and therefore untested, and because getting either wrong is invisible: a wrong
 * peer filter renders a chat with oneself or navigates to an empty route, and wrong unread
 * aggregation silently shows one user's badge count on another's row.
 */
class ChatListMappingTest {

    private val me = "uid_asha"
    private val peer = "uid_ravi"

    private fun conversation(
        id: String = "${me}_$peer",
        participants: List<String> = listOf(me, peer),
        unreadCounts: Map<String, Int> = emptyMap()
    ) = FirestoreConversation(id = id, participants = participants, unreadCounts = unreadCounts)

    // ----------------------------------------------------------------------------------------
    // Peer resolution
    // ----------------------------------------------------------------------------------------

    @Test
    fun testOtherParticipantReturnsThePeerNotTheViewer() {
        assertEquals(peer, conversation().otherParticipant(me))
    }

    @Test
    fun testOtherParticipantIsSymmetricAcrossBothViewers() {
        // Conversation IDs are ordered min_max, so the participant array is not ordered by
        // "me first". Peer resolution must work regardless of which end the viewer is.
        val conv = conversation(participants = listOf(peer, me))
        assertEquals(me, conv.otherParticipant(peer))
        assertEquals(peer, conv.otherParticipant(me))
    }

    @Test
    fun testOtherParticipantIsNullForSelfOnlyConversation() {
        // A malformed or migrated document listing only the viewer has no peer. Returning null
        // lets the caller skip the row instead of opening a chat with oneself.
        assertNull(conversation(participants = listOf(me)).otherParticipant(me))
    }

    @Test
    fun testOtherParticipantIsNullForEmptyParticipantList() {
        assertNull(conversation(participants = emptyList()).otherParticipant(me))
    }

    @Test
    fun testOtherParticipantReturnsFirstEntryWhenViewerUnknown() {
        // With no viewer there is nothing to exclude, so the first participant is returned. The
        // chat list never reaches this case: it emits an empty list when the UID is null.
        assertEquals(me, conversation(participants = listOf(me, peer)).otherParticipant(null))
    }

    // ----------------------------------------------------------------------------------------
    // Unread aggregation
    // ----------------------------------------------------------------------------------------

    @Test
    fun testUnreadTotalCountsOnlyTheViewersOwnUnread() {
        // The counter map is keyed per participant. Summing every value would credit the viewer
        // with the peer's unread messages too, which is the bug this guards against.
        val conv = conversation(unreadCounts = mapOf(me to 3, peer to 7))
        assertEquals(3, listOf(conv).unreadTotalFor(me))
        assertEquals(7, listOf(conv).unreadTotalFor(peer))
    }

    @Test
    fun testUnreadTotalSumsAcrossConversations() {
        val a = conversation(id = "c1", unreadCounts = mapOf(me to 2, peer to 5))
        val b = conversation(id = "c2", unreadCounts = mapOf(me to 4))
        assertEquals(6, listOf(a, b).unreadTotalFor(me))
    }

    @Test
    fun testUnreadTotalIsZeroWhenCounterAbsent() {
        // Every conversation written before the counter existed has no unreadCounts field at all.
        // That must read as zero, not as missing data or a crash.
        assertEquals(0, listOf(conversation()).unreadTotalFor(me))
    }

    @Test
    fun testUnreadTotalIsZeroForEmptyList() {
        assertEquals(0, emptyList<FirestoreConversation>().unreadTotalFor(me))
    }

    @Test
    fun testUnreadTotalIsZeroWhenViewerUnknown() {
        val conv = conversation(unreadCounts = mapOf(me to 3))
        assertEquals(0, listOf(conv).unreadTotalFor(null))
    }

    @Test
    fun testUnreadTotalIgnoresUnrelatedParticipants() {
        val conv = conversation(unreadCounts = mapOf("uid_someone_else" to 9))
        assertEquals(0, listOf(conv).unreadTotalFor(me))
    }
}
