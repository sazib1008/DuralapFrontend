package com.example.duralab.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.local.dao.ChatDao
import com.example.duralab.data.local.entity.ChatEntity
import com.example.duralab.data.local.entity.UserEntity
import com.example.duralab.data.remote.ChatApi
import com.example.duralab.data.repository.UserRepository
import com.example.duralab.util.TokenManager
import com.example.duralab.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao,
    private val chatApi: ChatApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _onlineUsers = MutableStateFlow<UiState<List<UserEntity>>>(UiState.Loading)
    val onlineUsers: StateFlow<UiState<List<UserEntity>>> = _onlineUsers

    private val _recentChats = MutableStateFlow<UiState<List<ChatEntity>>>(UiState.Loading)
    val recentChats: StateFlow<UiState<List<ChatEntity>>> = _recentChats

    private val _suggestedUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val suggestedUsers: StateFlow<List<UserEntity>> = _suggestedUsers

    init {
        loadData()
        loadSuggestedUsers()
    }

    private fun loadData() {
        viewModelScope.launch {
            userRepository.syncUsers()
            userRepository.getOnlineUsers().collect { users ->
                _onlineUsers.value = UiState.Success(users.filter { it.status == "ONLINE" || it.status == "online" })
            }
        }
        viewModelScope.launch {
            chatDao.getAllChats().collect { chats ->
                _recentChats.value = UiState.Success(chats)
            }
        }
        
        // Sync API logic updating conversations using the new ConversationResponse schema mapping
        viewModelScope.launch {
            try {
                val currentUserId = tokenManager.getUserId() ?: ""
                if (currentUserId.isNotEmpty()) {
                    val response = chatApi.getConversationsForUser(currentUserId)
                    if (response.isSuccessful) {
                        response.body()?.forEach { conv ->
                            // Identify the other participant's ID for 1-on-1 chats
                            val otherParticipantId = conv.participantIds.firstOrNull { it != currentUserId } ?: "group"
                            
                            val lastMsgTimestamp = try {
                                if (conv.lastMessage?.createdAt != null) Instant.parse(conv.lastMessage.createdAt).toEpochMilli() else 0L
                            } catch (e: Exception) {
                                0L
                            }
                            
                            val chatEntity = ChatEntity(
                                id = conv.id,
                                lastMessage = conv.lastMessage?.content,
                                lastMessageTimestamp = lastMsgTimestamp,
                                participantId = otherParticipantId,
                                unreadCount = conv.unreadCount
                            )
                            chatDao.insertChat(chatEntity)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // DB acts as Safe local cache fallback if API crashes
            }
        }
    }

    private fun loadSuggestedUsers() {
        // Mocking suggested users for the "Add New User" modal
        _suggestedUsers.value = listOf(
            UserEntity("a", "Alice Freeman", "alice@example.com", null, "offline", null),
            UserEntity("b", "Bob Marley", "bob@marley.com", null, "offline", null),
            UserEntity("c", "Charlie Brown", "charlie@fb.com", null, "online", null)
        )
    }

    fun searchUsers(query: String) {
        // Logic to filter suggested users or search from API
    }
}
