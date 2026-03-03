package com.example.duralab.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.local.dao.MessageDao
import com.example.duralab.data.local.entity.MessageEntity
import com.example.duralab.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageDao: MessageDao
) : ViewModel() {

    private val _messages = MutableStateFlow<UiState<List<MessageEntity>>>(UiState.Loading)
    val messages: StateFlow<UiState<List<MessageEntity>>> = _messages

    fun setChatId(chatId: String) {
        viewModelScope.launch {
            messageDao.getMessagesForChat(chatId).collect {
                _messages.value = UiState.Success(it)
            }
        }
    }

    fun sendMessage(chatId: String, content: String) {
        viewModelScope.launch {
            val newMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = "me", // Placeholder
                content = content,
                timestamp = System.currentTimeMillis(),
                status = "SENT",
                type = "TEXT"
            )
            messageDao.insertMessage(newMessage)
            // In a real app, this would also be sent via WebSocket
        }
    }
}
