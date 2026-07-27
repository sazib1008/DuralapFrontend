package com.example.duralapapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.MessageResponse
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatDetailUiState {
    data object Loading : ChatDetailUiState
    data class Success(val messages: List<MessageResponse>, val currentUserId: String) : ChatDetailUiState
    data class Error(val message: String) : ChatDetailUiState
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    val recipientName: String = savedStateHandle["recipientName"] ?: "Chat"

    val targetUserId: String
        get() {
            val state = _uiState.value
            if (state is ChatDetailUiState.Success) {
                val otherMsg = state.messages.firstOrNull { it.senderId != state.currentUserId }
                if (otherMsg != null) return otherMsg.senderId
            }
            return ""
        }

    private val _uiState = MutableStateFlow<ChatDetailUiState>(ChatDetailUiState.Loading)
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            tokenManager.userId.collect { id ->
                if (!id.isNullOrBlank()) {
                    currentUserId = id
                    loadMessages()
                    observeRealTimeMessages()
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = ChatDetailUiState.Loading
            chatRepository.getMessages(conversationId)
                .onSuccess { msgList ->
                    _uiState.value = ChatDetailUiState.Success(
                        messages = msgList.reversed(), // Latest at bottom
                        currentUserId = currentUserId
                    )
                    chatRepository.markAllAsRead(conversationId)
                }
                .onFailure { error ->
                    _uiState.value = ChatDetailUiState.Error(
                        error.localizedMessage ?: "Failed to load messages"
                    )
                }
        }
    }

    private fun observeRealTimeMessages() {
        viewModelScope.launch {
            chatRepository.observeConversationMessages(conversationId)
                .collect { newMsg ->
                    val currentState = _uiState.value
                    if (currentState is ChatDetailUiState.Success) {
                        if (currentState.messages.none { it.id == newMsg.id }) {
                            _uiState.value = currentState.copy(
                                messages = currentState.messages + newMsg
                            )
                        }
                    }
                }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || currentUserId.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(
                conversationId = conversationId,
                senderId = currentUserId,
                content = content
            ).onSuccess { sentMsg ->
                val currentState = _uiState.value
                if (currentState is ChatDetailUiState.Success) {
                    if (currentState.messages.none { it.id == sentMsg.id }) {
                        _uiState.value = currentState.copy(
                            messages = currentState.messages + sentMsg
                        )
                    }
                }
            }
        }
    }
}
