package com.synfusion.attendance.live.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synfusion.attendance.live.data.DemoLiveRepository
import com.synfusion.attendance.live.model.*
import kotlinx.coroutines.flow.StateFlow

class LiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DemoLiveRepository.getInstance(application)

    val liveRooms: StateFlow<List<LiveRoom>> = repository.liveRooms
    val creators: StateFlow<List<LiveCreator>> = repository.creators
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
        repository.sendChatMessage(roomId, text, senderName)
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

class LiveViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LiveViewModel::class.java)) {
            return LiveViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
