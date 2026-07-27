package com.example.duralapapp.ui.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.model.CallResponse
import com.example.duralapapp.data.model.CallStatus
import com.example.duralapapp.data.model.CallType
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CallUiState {
    data object Idle : CallUiState
    data class OutgoingRinging(val recipientName: String, val callType: CallType) : CallUiState
    data class IncomingRinging(val callerName: String, val callId: String, val callType: CallType) : CallUiState
    data class Connected(val callId: String, val recipientName: String, val durationSeconds: Long, val callType: CallType) : CallUiState
    data class Ended(val reason: String) : CallUiState
}

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetUserId: String = savedStateHandle["targetUserId"] ?: ""
    val targetUserName: String = savedStateHandle["targetUserName"] ?: "Contact"
    val conversationId: String = savedStateHandle["conversationId"] ?: ""
    val initialCallTypeStr: String = savedStateHandle["callType"] ?: "AUDIO"

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(initialCallTypeStr == "VIDEO")
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(initialCallTypeStr == "VIDEO")
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private var activeCallId: String? = null
    private var currentUserId: String = ""
    private var timerJob: Job? = null
    private var callDurationSeconds: Long = 0

    init {
        viewModelScope.launch {
            tokenManager.userId.collect { id ->
                if (!id.isNullOrBlank()) {
                    currentUserId = id
                    observeSignaling()
                    if (targetUserId.isNotBlank() && conversationId.isNotBlank()) {
                        val callType = if (initialCallTypeStr == "VIDEO") CallType.VIDEO else CallType.AUDIO
                        startCall(conversationId, targetUserId, callType)
                    }
                }
            }
        }
    }

    private fun startCall(conversationId: String, calleeId: String, callType: CallType) {
        viewModelScope.launch {
            _uiState.value = CallUiState.OutgoingRinging(targetUserName, callType)
            callRepository.initiateCall(
                conversationId = conversationId,
                callerId = currentUserId,
                calleeId = calleeId,
                callType = callType
            ).onSuccess { response ->
                activeCallId = response.id
            }.onFailure { error ->
                _uiState.value = CallUiState.Ended(error.localizedMessage ?: "Failed to start call")
            }
        }
    }

    fun acceptCall(callId: String) {
        viewModelScope.launch {
            callRepository.acceptCall(callId, currentUserId)
                .onSuccess { response ->
                    activeCallId = response.id
                    startCallTimer(response.id, targetUserName, response.callType)
                }
                .onFailure { error ->
                    _uiState.value = CallUiState.Ended(error.localizedMessage ?: "Failed to accept call")
                }
        }
    }

    fun rejectCall(callId: String) {
        viewModelScope.launch {
            callRepository.rejectCall(callId, currentUserId)
            _uiState.value = CallUiState.Ended("Call declined")
        }
    }

    fun endCall() {
        val callId = activeCallId
        viewModelScope.launch {
            if (callId != null) {
                callRepository.endCall(callId, currentUserId)
            }
            stopCallTimer()
            _uiState.value = CallUiState.Ended("Call ended")
        }
    }

    private fun observeSignaling() {
        viewModelScope.launch {
            callRepository.observeUserSignaling(currentUserId)
                .collect { signal ->
                    // Handle incoming signaling offer/answer/ICE candidate
                }
        }
    }

    private fun startCallTimer(callId: String, recipientName: String, callType: CallType) {
        timerJob?.cancel()
        callDurationSeconds = 0
        timerJob = viewModelScope.launch {
            while (true) {
                _uiState.value = CallUiState.Connected(
                    callId = callId,
                    recipientName = recipientName,
                    durationSeconds = callDurationSeconds,
                    callType = callType
                )
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    private fun stopCallTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleMute() { _isMuted.value = !_isMuted.value }
    fun toggleVideo() { _isVideoEnabled.value = !_isVideoEnabled.value }
    fun toggleSpeaker() { _isSpeakerOn.value = !_isSpeakerOn.value }
    fun toggleCamera() { _isFrontCamera.value = !_isFrontCamera.value }
}
