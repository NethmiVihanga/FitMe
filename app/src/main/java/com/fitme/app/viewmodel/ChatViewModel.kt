package com.fitme.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitme.app.data.model.Message
import com.fitme.app.data.model.Chat
import com.fitme.app.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _sendResult = MutableLiveData<Result<Unit>>()
    val sendResult: LiveData<Result<Unit>> = _sendResult

    private val _recentChats = MutableLiveData<List<Chat>>()
    val recentChats: LiveData<List<Chat>> = _recentChats

    fun listenToMessages(userId: String, otherUserId: String) {
        chatRepository.listenToMessages(userId, otherUserId) { messages ->
            _messages.postValue(messages)
        }
    }

    fun sendMessage(senderId: String, receiverId: String, text: String) {
        viewModelScope.launch {
            val result = chatRepository.sendMessage(senderId, receiverId, text)
            _sendResult.value = result
        }
    }

    fun sendImageMessage(senderId: String, receiverId: String, imageUri: android.net.Uri) {
        viewModelScope.launch {
            val result = chatRepository.sendImageMessage(senderId, receiverId, imageUri)
            _sendResult.value = result
        }
    }

    fun sendVoiceMessage(senderId: String, receiverId: String, audioUri: android.net.Uri, duration: Int) {
        viewModelScope.launch {
            val result = chatRepository.sendVoiceMessage(senderId, receiverId, audioUri, duration)
            _sendResult.value = result
        }
    }

    fun loadRecentChats(userId: String) {
        chatRepository.listenToRecentChats(userId) { chats ->
            _recentChats.postValue(chats)
        }
    }
    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            chatRepository.markAsRead(chatId, userId)
        }
    }

    fun deleteChat(userId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(userId, otherUserId)
        }
    }
}
