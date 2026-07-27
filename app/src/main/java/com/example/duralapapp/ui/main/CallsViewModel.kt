package com.example.duralapapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.CallHistoryItemResponse
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CallsUiState {
    data object Loading : CallsUiState
    data class Success(val calls: List<CallHistoryItemResponse>) : CallsUiState
    data class Error(val message: String) : CallsUiState
}

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CallsUiState>(CallsUiState.Loading)
    val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

    init {
        loadCallHistory()
    }

    fun loadCallHistory() {
        viewModelScope.launch {
            _uiState.value = CallsUiState.Loading
            tokenManager.userId.collect { userId ->
                if (!userId.isNullOrBlank()) {
                    callRepository.getCallHistory(userId)
                        .onSuccess { list ->
                            _uiState.value = CallsUiState.Success(list)
                        }
                        .onFailure { error ->
                            _uiState.value = CallsUiState.Error(
                                error.localizedMessage ?: "Failed to load calls"
                            )
                        }
                }
            }
        }
    }
}
