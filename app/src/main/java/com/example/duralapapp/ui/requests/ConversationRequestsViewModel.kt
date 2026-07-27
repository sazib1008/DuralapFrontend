package com.example.duralapapp.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.ConversationRequestResponse
import com.example.duralapapp.data.repository.ConversationRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PendingRequestsUiState {
    data object Loading : PendingRequestsUiState
    data class Success(val requests: List<ConversationRequestResponse>) : PendingRequestsUiState
    data class Error(val message: String) : PendingRequestsUiState
}

sealed interface RequestActionResult {
    data object Idle : RequestActionResult
    data class Success(val message: String) : RequestActionResult
    data class Error(val message: String) : RequestActionResult
}

@HiltViewModel
class ConversationRequestsViewModel @Inject constructor(
    private val conversationRequestRepository: ConversationRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PendingRequestsUiState>(PendingRequestsUiState.Loading)
    val uiState: StateFlow<PendingRequestsUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableStateFlow<RequestActionResult>(RequestActionResult.Idle)
    val actionResult: StateFlow<RequestActionResult> = _actionResult.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        loadPendingRequests()
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.value = PendingRequestsUiState.Loading
            conversationRequestRepository.getPendingRequests()
                .onSuccess { list ->
                    _uiState.value = PendingRequestsUiState.Success(list)
                    _pendingCount.value = list.size
                }
                .onFailure { error ->
                    _uiState.value = PendingRequestsUiState.Error(
                        error.localizedMessage ?: "Failed to load requests"
                    )
                }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            conversationRequestRepository.acceptRequest(requestId)
                .onSuccess {
                    _actionResult.value = RequestActionResult.Success("Request accepted!")
                    loadPendingRequests()
                }
                .onFailure { error ->
                    _actionResult.value = RequestActionResult.Error(
                        error.localizedMessage ?: "Failed to accept request"
                    )
                }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            conversationRequestRepository.rejectRequest(requestId)
                .onSuccess {
                    _actionResult.value = RequestActionResult.Success("Request rejected.")
                    loadPendingRequests()
                }
                .onFailure { error ->
                    _actionResult.value = RequestActionResult.Error(
                        error.localizedMessage ?: "Failed to reject request"
                    )
                }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            conversationRequestRepository.cancelRequest(requestId)
                .onSuccess {
                    _actionResult.value = RequestActionResult.Success("Request canceled.")
                    loadPendingRequests()
                }
                .onFailure { error ->
                    _actionResult.value = RequestActionResult.Error(
                        error.localizedMessage ?: "Failed to cancel request"
                    )
                }
        }
    }

    fun clearActionResult() {
        _actionResult.value = RequestActionResult.Idle
    }
}
