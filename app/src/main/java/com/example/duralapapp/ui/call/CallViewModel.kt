package com.example.duralapapp.ui.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralapapp.data.call.CallManager
import com.example.duralapapp.data.call.CallState
import com.example.duralapapp.data.model.CallType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    val callManager: CallManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetUserId: String = savedStateHandle["targetUserId"] ?: ""
    val targetUserName: String = savedStateHandle["targetUserName"] ?: "Contact"
    val conversationId: String = savedStateHandle["conversationId"] ?: ""
    val initialCallTypeStr: String = savedStateHandle["callType"] ?: "AUDIO"

    val callState: StateFlow<CallState> = callManager.callState

    init {
        // If navigated to start an outgoing call
        if (targetUserId.isNotBlank() && conversationId.isNotBlank()) {
            val callType = if (initialCallTypeStr == "VIDEO") CallType.VIDEO else CallType.AUDIO
            if (callManager.callState.value is CallState.Idle) {
                callManager.startCall(
                    targetUserId = targetUserId,
                    targetUserName = targetUserName,
                    targetUserAvatar = null,
                    conversationId = conversationId,
                    callType = callType
                )
            }
        }
    }

    fun initLocalRenderer(renderer: SurfaceViewRenderer) {
        callManager.webRtcClient.initSurfaceViewRenderer(renderer)
        callManager.webRtcClient.attachLocalRenderer(renderer)
    }

    fun initRemoteRenderer(renderer: SurfaceViewRenderer) {
        callManager.webRtcClient.initSurfaceViewRenderer(renderer)
        callManager.webRtcClient.attachRemoteRenderer(renderer)
    }

    fun acceptCall() {
        callManager.acceptCall()
    }

    fun rejectCall() {
        callManager.rejectCall()
    }

    fun cancelCall() {
        callManager.cancelCall()
    }

    fun endCall() {
        val state = callManager.callState.value
        when (state) {
            is CallState.OutgoingRinging -> callManager.cancelCall()
            is CallState.IncomingRinging -> callManager.rejectCall()
            else -> callManager.hangUp()
        }
    }

    fun toggleMute() = callManager.toggleMicrophone()
    fun toggleVideo() = callManager.toggleCamera()
    fun toggleSpeaker() = callManager.toggleSpeakerphone()
    fun switchCamera() = callManager.switchCamera()
}
