package com.example.duralab.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.local.dao.ChatDao
import com.example.duralab.data.local.entity.ChatEntity
import com.example.duralab.data.local.entity.UserEntity
import com.example.duralab.data.repository.UserRepository
import com.example.duralab.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val chatDao: ChatDao
) : ViewModel() {

    private val _onlineUsers = MutableStateFlow<UiState<List<UserEntity>>>(UiState.Loading)
    val onlineUsers: StateFlow<UiState<List<UserEntity>>> = _onlineUsers

    private val _recentChats = MutableStateFlow<UiState<List<ChatEntity>>>(UiState.Loading)
    val recentChats: StateFlow<UiState<List<ChatEntity>>> = _recentChats

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            userRepository.syncUsers()
            userRepository.getOnlineUsers().collect { users ->
                _onlineUsers.value = UiState.Success(users.filter { it.status == "online" })
            }
        }
        viewModelScope.launch {
            chatDao.getAllChats().collect { chats ->
                _recentChats.value = UiState.Success(chats)
            }
        }
    }
}
