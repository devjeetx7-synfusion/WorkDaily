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

    fun toggleFollowCreator(creatorId: String) {
        repository.toggleFollowCreator(creatorId)
    }

    fun topUpDiamonds(amount: Int) {
        repository.topUpDiamonds(amount)
    }

    fun addFreeDiamonds(amount: Int = 1000) {
        repository.addFreeDiamonds(amount)
    }

    fun sendGift(roomId: String, gift: LiveGift): Boolean {
        return repository.sendGift(roomId, gift)
    }

    fun sendChatMessage(roomId: String, text: String, senderName: String = "You") {
        repository.sendChatMessage(roomId, text, senderName)
    }

    fun setActiveRoom(roomId: String?) {
        repository.setActiveRoom(roomId)
    }

    fun startLiveStream(
        title: String,
        category: String,
        type: LiveRoomType,
        tags: List<String>
    ): LiveRoom {
        return repository.startLiveStream(title, category, type, tags)
    }

    fun endLiveStream(roomId: String) {
        repository.endLiveStream(roomId)
    }

    fun cashoutEarnings(diamonds: Int): Boolean {
        return repository.cashoutEarnings(diamonds)
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
