package com.example.duralapapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.UserResponse
import com.example.duralapapp.data.repository.ConversationRequestRepository
import com.example.duralapapp.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Success(val users: List<UserResponse>, val nextCursor: String? = null) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

sealed interface RequestActionState {
    data object Idle : RequestActionState
    data class Loading(val targetUserId: String) : RequestActionState
    data class Success(val targetUserId: String, val message: String) : RequestActionState
    data class Error(val message: String) : RequestActionState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val conversationRequestRepository: ConversationRequestRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<RequestActionState>(RequestActionState.Idle)
    val actionState: StateFlow<RequestActionState> = _actionState.asStateFlow()

    init {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    _uiState.value = SearchUiState.Idle
                } else {
                    performSearch(query)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Searching
            searchRepository.searchUsers(query)
                .onSuccess { response ->
                    _uiState.value = SearchUiState.Success(
                        users = response.items,
                        nextCursor = response.nextCursor
                    )
                }
                .onFailure { error ->
                    _uiState.value = SearchUiState.Error(error.localizedMessage ?: "Failed to search users")
                }
        }
    }

    fun sendConversationRequest(targetUserId: String, initialMessage: String? = null) {
        viewModelScope.launch {
            _actionState.value = RequestActionState.Loading(targetUserId)
            conversationRequestRepository.startConversationWithUser(targetUserId, initialMessage)
                .onSuccess {
                    _actionState.value = RequestActionState.Success(
                        targetUserId = targetUserId,
                        message = "Conversation request sent successfully!"
                    )
                }
                .onFailure { error ->
                    _actionState.value = RequestActionState.Error(
                        error.localizedMessage ?: "Failed to send request"
                    )
                }
        }
    }

    fun resetActionState() {
        _actionState.value = RequestActionState.Idle
    }
}
