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
    data class Success(val conversations: List<ConversationResponse>, val currentUserId: String = "") : ChatListUiState
    data class Error(val message: String) : ChatListUiState
    data object Empty : ChatListUiState
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    init {
        observeUserId()
        loadConversations()
        initWebSocketConnection()
    }

    private fun observeUserId() {
        viewModelScope.launch {
            tokenManager.userId.collect { id ->
                if (!id.isNullOrBlank()) {
                    currentUserId = id
                    observeRealTimeUserMessages(id)
                }
            }
        }
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = ChatListUiState.Loading
            chatRepository.getMyConversations()
                .onSuccess { conversations ->
                    if (conversations.isEmpty()) {
                        _uiState.value = ChatListUiState.Empty
                    } else {
                        _uiState.value = ChatListUiState.Success(
                            conversations = conversations,
                            currentUserId = currentUserId
                        )
                    }
                }
                .onFailure { error ->
                    val errorMessage = when (error) {
                        is retrofit2.HttpException -> {
                            when (error.code()) {
                                401 -> "Session expired. Please log in again."
                                403 -> "Access denied. Unauthorized request."
                                else -> "Server error (${error.code()}). Please try again."
                            }
                        }
                        is java.io.IOException -> "Network timeout. Please check your connection."
                        else -> error.localizedMessage ?: "Failed to load conversations"
                    }
                    _uiState.value = ChatListUiState.Error(errorMessage)
                }
        }
    }

    private fun observeRealTimeUserMessages(userId: String) {
        viewModelScope.launch {
            chatRepository.observeUserMessages(userId)
                .catch { /* ignore network error in bg flow */ }
                .collect {
                    // Refresh conversation list when a new real-time message arrives
                    loadConversations()
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

