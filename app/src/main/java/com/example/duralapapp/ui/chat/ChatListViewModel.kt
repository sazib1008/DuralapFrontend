package com.example.duralapapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.ConversationResponse
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatListUiState {
    data object Loading : ChatListUiState
    data class Success(val conversations: List<ConversationResponse>) : ChatListUiState
    data class Error(val message: String) : ChatListUiState
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
        initWebSocketConnection()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = ChatListUiState.Loading
            chatRepository.getMyConversations()
                .onSuccess { conversations ->
                    _uiState.value = ChatListUiState.Success(conversations)
                }
                .onFailure { error ->
                    _uiState.value = ChatListUiState.Error(
                        error.localizedMessage ?: "Failed to load chats"
                    )
                }
        }
    }

    private fun initWebSocketConnection() {
        viewModelScope.launch {
            tokenManager.accessToken.collect { token ->
                if (!token.isNullOrBlank()) {
                    chatRepository.connectWebSocket(token)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.disconnectWebSocket()
    }
}
