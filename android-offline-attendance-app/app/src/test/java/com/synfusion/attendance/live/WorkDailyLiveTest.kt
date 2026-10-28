package com.synfusion.attendance.live

import com.synfusion.attendance.live.data.DemoLiveRepository
import com.synfusion.attendance.live.model.LiveRoomType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorkDailyLiveTest {

    private lateinit var repository: DemoLiveRepository

    @Before
    fun setUp() {
        repository = DemoLiveRepository(context = null)
        repository.resetDemoData()
    }

    @Test
    fun testInitialSeedDataAndGiftsCount() {
        val gifts = repository.gifts.value
        val rooms = repository.liveRooms.value
        val creators = repository.creators.value

        assertTrue("Should have 20 or more construction gifts", gifts.size >= 20)
        assertTrue("Should have initial active live rooms", rooms.isNotEmpty())
        assertTrue("Should have initial creators", creators.isNotEmpty())
    }

    @Test
    fun testTradeRolesDefinedInRooms() {
        val rooms = repository.liveRooms.value
        assertTrue("Live rooms should have trade roles defined", rooms.all { it.tradeRole.isNotBlank() })
    }

    @Test
    fun testTopUpAndSendGiftFlow() {
        val initialWallet = repository.wallet.value
        val gifts = repository.gifts.value
        val targetGift = gifts.first { it.diamondCost == 20 } // Hard Hat = 20 diamonds
        val initialRooms = repository.liveRooms.value
        val targetRoom = initialRooms.first()

        // 1. Top up 100 diamonds
        repository.topUpDiamonds(100)
        val walletAfterTopUp = repository.wallet.value
        assertEquals(initialWallet.diamondBalance + 100, walletAfterTopUp.diamondBalance)

        // 2. Send gift
        val success = repository.sendGift(targetRoom.id, targetGift)
        assertTrue("Gift sending should succeed", success)

        // 3. Verify balance deducted
        val walletAfterGift = repository.wallet.value
        assertEquals(walletAfterTopUp.diamondBalance - 20, walletAfterGift.diamondBalance)

        // 4. Verify chat announcement posted
        val chatMap = repository.chatMessagesMap.value
        val roomChats = chatMap[targetRoom.id] ?: emptyList()
        assertTrue("Gift announcement should be posted in chat", roomChats.any { it.isGiftAnnouncement && it.giftName == targetGift.name })
    }

    @Test
    fun testSendGiftInsufficientBalanceFails() {
        repository.resetDemoData()
        val expensiveGift = repository.gifts.value.first { it.diamondCost == 5000 } // Master Builder Trophy
        val rooms = repository.liveRooms.value
        val room = rooms.first()

        // Wallet balance starts at 500
        val walletBefore = repository.wallet.value
        val success = repository.sendGift(room.id, expensiveGift)

        assertFalse("Sending gift without enough balance should fail", success)
        assertEquals("Wallet balance should remain unchanged", walletBefore.diamondBalance, repository.wallet.value.diamondBalance)
    }

    @Test
    fun testToggleFollowCreator() {
        val creators = repository.creators.value
        val target = creators.first()

        val initialFollowed = target.isFollowing
        repository.toggleFollowCreator(target.id)

        val updatedCreators = repository.creators.value
        val updatedTarget = updatedCreators.first { it.id == target.id }

        assertNotEquals("Follow state should be toggled", initialFollowed, updatedTarget.isFollowing)
    }

    @Test
    fun testStartLiveStreamAndCashout() {
        val newRoom = repository.startLiveStream(
            title = "Test Site Inspection",
            category = "Engineering",
            type = LiveRoomType.VIDEO,
            tags = listOf("Test", "Inspection")
        )

        assertNotNull("Created room should not be null", newRoom)
        assertEquals(newRoom.id, repository.activeRoom.value?.id)

        // Test Cashout
        val creatorEarningBefore = repository.creatorEarning.value
        val cashoutSuccess = repository.cashoutEarnings(200)

        assertTrue("Cashout should succeed when balance is sufficient", cashoutSuccess)
        val creatorEarningAfter = repository.creatorEarning.value
        assertEquals(creatorEarningBefore.totalDiamondsReceived - 200, creatorEarningAfter.totalDiamondsReceived)
        assertEquals(1, creatorEarningAfter.cashoutHistory.size)
    }

    @Test
    fun testFriendRequestAndMessagingFlow() {
        val targetUserId = "user_102"

        // 1. Cannot send private message before becoming friends
        val preMessageSuccess = repository.sendPrivateMessage(targetUserId, "Hello!")
        assertFalse("Messaging non-friends should fail", preMessageSuccess)

        // 2. Send friend request
        val requestSent = repository.sendFriendRequest(targetUserId)
        assertTrue("Friend request should be sent", requestSent)

        // 3. Find pending request and accept it
        val req = repository.friendRequests.value.first { it.receiverId == targetUserId || it.senderId == targetUserId }
        val accepted = repository.acceptFriendRequest(req.id)
        assertTrue("Friend request acceptance should succeed", accepted)

        // 4. Send private message now that users are friends
        val postMessageSuccess = repository.sendPrivateMessage(targetUserId, "Hello friend!")
        assertTrue("Messaging friends should succeed", postMessageSuccess)

        // 5. Verify conversation and message content
        val conv = repository.conversations.value.first { it.friendId == targetUserId }
        val messages = repository.privateMessagesMap.value[conv.id] ?: emptyList()
        assertEquals(1, messages.size)
        assertEquals("Hello friend!", messages.first().message)
    }

    @Test
    fun testBlockUserDisablesMessagingAndFriendship() {
        val friendId = "creator_1" // Initial friend in seed data

        // 1. Initial messaging works
        val msgSent = repository.sendPrivateMessage(friendId, "Hey Rajesh!")
        assertTrue("Initial message to friend should succeed", msgSent)

        // 2. Block user
        repository.blockUser(friendId)

        // 3. Subsequent messaging fails
        val blockedMsgSent = repository.sendPrivateMessage(friendId, "Are you there?")
        assertFalse("Messaging blocked user should fail", blockedMsgSent)
    }

    @Test
    fun testGiftOverlayEventAndQueueManagement() {
        val room = repository.liveRooms.value.first()
        val gift = repository.gifts.value.first { it.diamondCost == 100 } // Power Drill

        // 1. Send gift with quantity = 2
        val success = repository.sendGift(room.id, gift, quantity = 2)
        assertTrue("Gift sending should succeed", success)

        // 2. Verify Gift Overlay Event is generated with correct unit and total costs
        val overlayEvents = repository.giftOverlayEvents.value
        val event = overlayEvents.find { it.roomId == room.id && it.giftName == gift.name }
        assertNotNull("Overlay event should be created", event)
        assertEquals(2, event?.quantity)
        assertEquals(100, event?.unitDiamondCost)
        assertEquals(200, event?.totalDiamondCost)
        assertEquals(200, event?.diamondCost)

        // 3. Remove overlay event on animation complete
        repository.removeGiftOverlayEvent(event!!.id)
        val updatedEvents = repository.giftOverlayEvents.value
        assertFalse("Overlay event should be removed after animation completion", updatedEvents.any { it.id == event.id })
    }

    @Test
    fun testChronologicalGiftOverlayEventOrdering() {
        val room = repository.liveRooms.value.first()
        val gift1 = repository.gifts.value.first { it.diamondCost == 1 } // Safety Helmet
        val gift2 = repository.gifts.value.first { it.diamondCost == 5 } // Measuring Tape

        repository.sendGift(room.id, gift1, quantity = 1)
        repository.sendGift(room.id, gift2, quantity = 1)

        val events = repository.giftOverlayEvents.value
        assertTrue("Queue should contain at least 2 events", events.size >= 2)
        assertEquals("First sent gift should be first in chronological queue", gift1.name, events[events.size - 2].giftName)
        assertEquals("Second sent gift should be second in chronological queue", gift2.name, events.last().giftName)
    }

    @Test
    fun testRealtimeSystemChatEventsAndDeduplication() {
        val room = repository.liveRooms.value.first()

        // 1. User joined event
        repository.onUserJoinedRoom(room.id, "Dev")
        val chatsAfterJoin = repository.chatMessagesMap.value[room.id] ?: emptyList()
        assertTrue("USER_JOINED message should be in chat", chatsAfterJoin.any { it.eventType == "USER_JOINED" && it.senderName == "Dev" })

        // Duplicate join within recent history should be ignored
        val joinCountBefore = chatsAfterJoin.count { it.eventType == "USER_JOINED" && it.senderName == "Dev" }
        repository.onUserJoinedRoom(room.id, "Dev")
        val chatsAfterDupJoin = repository.chatMessagesMap.value[room.id] ?: emptyList()
        val joinCountAfter = chatsAfterDupJoin.count { it.eventType == "USER_JOINED" && it.senderName == "Dev" }
        assertEquals("Duplicate join should not be posted twice", joinCountBefore, joinCountAfter)

        // 2. Share Live event
        repository.onShareLiveStream(room.id, "Anu")
        val chatsAfterShare = repository.chatMessagesMap.value[room.id] ?: emptyList()
        assertTrue("LIVE_SHARED message should be in chat", chatsAfterShare.any { it.eventType == "LIVE_SHARED" && it.senderName == "Anu" })

        // 3. Follow invitation
        repository.triggerHostFollowInvitation(room.id, "Rajesh")
        val chatsAfterInvite = repository.chatMessagesMap.value[room.id] ?: emptyList()
        assertTrue("FOLLOW_INVITATION message should be in chat", chatsAfterInvite.any { it.eventType == "FOLLOW_INVITATION" && it.senderName == "Rajesh" })
    }

    @Test
    fun testToggleFollowWithRoomContextEmitsChatEvent() {
        val creators = repository.creators.value
        val target = creators.first { !it.isFollowing }
        val room = repository.liveRooms.value.first()

        repository.toggleFollowCreator(target.id, room.id)

        val chats = repository.chatMessagesMap.value[room.id] ?: emptyList()
        assertTrue("Following creator in room context should post USER_FOLLOWED system chat event", chats.any { it.eventType == "USER_FOLLOWED" })
    }

    @Test
    fun testYouTubeUrlAndIdParsing() {
        val stdUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val shortUrl = "https://youtu.be/dQw4w9WgXcQ"
        val shortsUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
        val directId = "dQw4w9WgXcQ"
        val invalidUrl = "https://example.com/video"

        val playlistUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890ABCDEF"
        val directPlaylistId = "PL1234567890ABCDEF"

        assertEquals("dQw4w9WgXcQ", com.synfusion.attendance.live.ui.extractYouTubeVideoId(stdUrl))
        assertEquals("dQw4w9WgXcQ", com.synfusion.attendance.live.ui.extractYouTubeVideoId(shortUrl))
        assertEquals("dQw4w9WgXcQ", com.synfusion.attendance.live.ui.extractYouTubeVideoId(shortsUrl))
        assertEquals("dQw4w9WgXcQ", com.synfusion.attendance.live.ui.extractYouTubeVideoId(directId))
        assertNull(com.synfusion.attendance.live.ui.extractYouTubeVideoId(invalidUrl))

        assertEquals("PL1234567890ABCDEF", com.synfusion.attendance.live.ui.extractYouTubePlaylistId(playlistUrl))
        assertEquals("PL1234567890ABCDEF", com.synfusion.attendance.live.ui.extractYouTubePlaylistId(directPlaylistId))
        assertNull(com.synfusion.attendance.live.ui.extractYouTubePlaylistId(invalidUrl))
    }

    @Test
    fun testStartYouTubeWatchPartyRoomCreation() {
        val videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val videoId = "dQw4w9WgXcQ"

        val room = repository.startLiveStream(
            title = "Site Safety YouTube Session",
            category = "Site Safety",
            type = LiveRoomType.YOUTUBE_WATCH_PARTY,
            tags = listOf("YouTube", "Safety"),
            youtubeVideoUrl = videoUrl,
            youtubeVideoId = videoId
        )

        assertNotNull("YouTube Watch Party room should be created", room)
        assertEquals(LiveRoomType.YOUTUBE_WATCH_PARTY, room.type)
        assertEquals(videoId, room.youtubeVideoId)
        assertEquals(videoUrl, room.youtubeVideoUrl)

        val wpState = room.watchPartyState
        assertNotNull("Initial WatchPartyState should be initialized", wpState)
        assertEquals(videoId, wpState?.videoId)
        assertEquals(1L, wpState?.revision)
        assertEquals(com.synfusion.attendance.live.model.WatchPartyPlaybackStatus.UNSTARTED, wpState?.status)
    }

    @Test
    fun testUpdateWatchPartyStateRevisionHandling() {
        val room = repository.startLiveStream(
            title = "Sync Test Room",
            category = "Site Safety",
            type = LiveRoomType.YOUTUBE_WATCH_PARTY,
            tags = listOf("Sync"),
            youtubeVideoId = "dQw4w9WgXcQ"
        )

        val initialRevision = room.watchPartyState?.revision ?: 0L

        // Host updates playback state to PLAYING at position 12.5 seconds
        repository.updateWatchPartyState(
            roomId = room.id,
            status = com.synfusion.attendance.live.model.WatchPartyPlaybackStatus.PLAYING,
            positionSeconds = 12.5f
        )

        val updatedRoom = repository.liveRooms.value.find { it.id == room.id }
        val updatedState = updatedRoom?.watchPartyState

        assertNotNull(updatedState)
        assertEquals(initialRevision + 1L, updatedState?.revision)
        assertEquals(com.synfusion.attendance.live.model.WatchPartyPlaybackStatus.PLAYING, updatedState?.status)
        assertEquals(12.5f, updatedState?.positionSeconds ?: 0f, 0.01f)
    }
}
