package com.example.duralab.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.model.CallHistoryItemResponse
import com.example.duralab.data.repository.CallRepository
import com.example.duralab.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CallHistoryUiState {
    object Loading : CallHistoryUiState()
    data class Success(val history: List<CallHistoryItemResponse>) : CallHistoryUiState()
    data class Error(val message: String) : CallHistoryUiState()
}

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CallHistoryUiState>(CallHistoryUiState.Loading)
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    init {
        loadCallHistory()
    }

    fun loadCallHistory() {
        val userId = tokenManager.getUserId()
        if (userId == null) {
            _uiState.value = CallHistoryUiState.Error("User ID not found. Please login again.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CallHistoryUiState.Loading
            val result = callRepository.getCallHistoryList(userId)
            if (result.isSuccess) {
                _uiState.value = CallHistoryUiState.Success(result.getOrDefault(emptyList()))
            } else {
                _uiState.value = CallHistoryUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}
