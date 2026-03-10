package com.example.duralab.ui.call

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.duralab.data.remote.CallApi
import com.example.duralab.data.model.CallInitiateRequest
import com.example.duralab.data.model.CallType
import com.example.duralab.data.remote.StompClient
import com.example.duralab.data.remote.WebRTCManager
import com.example.duralab.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import javax.inject.Inject

enum class CallState {
    IDLE, INCOMING, OUTGOING, CONNECTED, ENDED
}

@HiltViewModel
class CallViewModel @Inject constructor(
    private val webRTCManager: WebRTCManager,
    private val callApi: CallApi,
    private val stompClient: StompClient,
    private val tokenManager: TokenManager
) : ViewModel(), PeerConnection.Observer {

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private var currentCallId: String? = null
    private var targetUserId: String? = null

    init {
        // Initialize the WebRTC peer connection early so we are ready
        webRTCManager.createPeerConnection(this)
        
        viewModelScope.launch {
            stompClient.messages.collect { message ->
                handleSignalingMessage(message)
            }
        }
    }

    private fun handleSignalingMessage(message: String) {
        // Here you would parse the incoming STOMP message
    }

    fun initiateCall(calleeId: String, conversationId: String = "conv_123", callType: CallType = CallType.VIDEO) {
        targetUserId = calleeId
        _callState.value = CallState.OUTGOING
        val callerId = tokenManager.getUserId() ?: ""

        viewModelScope.launch {
            try {
                val response = callApi.initiateCall(
                    CallInitiateRequest(conversationId, callerId, calleeId, callType)
                )
                if (response.isSuccessful) {
                    currentCallId = response.body()?.id
                    Log.d("CallViewModel", "Call initiated: ${currentCallId}")

                    webRTCManager.initiateCall(calleeId, object : SdpObserver {
                        override fun onCreateSuccess(desc: SessionDescription?) {
                            desc?.let {
                                viewModelScope.launch {
                                    try {
                                        callApi.updateCallWithSignaling(callId = currentCallId!!, offer = it.description)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        override fun onSetSuccess() {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptIncomingCall() {
        _callState.value = CallState.CONNECTED
        val currentUserId = tokenManager.getUserId() ?: ""
        viewModelScope.launch {
            currentCallId?.let { callId ->
                try {
                    callApi.acceptCall(callId, currentUserId)
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun endCall() {
        val previousState = _callState.value
        _callState.value = CallState.ENDED
        val currentUserId = tokenManager.getUserId() ?: ""
        
        viewModelScope.launch {
            if (previousState == CallState.INCOMING && currentCallId != null) {
                try { callApi.rejectCall(currentCallId!!, currentUserId) } catch (e: Exception) {}
            } else if (currentCallId != null) {
                try { callApi.endCall(currentCallId!!, currentUserId) } catch (e: Exception) {}
            }
            webRTCManager.endCall()
            currentCallId = null
            _callState.value = CallState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager.destroy()
    }

    // PeerConnection.Observer implementation
    override fun onIceCandidate(candidate: IceCandidate?) {
        candidate?.let { ice ->
            viewModelScope.launch {
                currentCallId?.let { callId ->
                    try {
                        val iceObj = """{"candidate":"${ice.sdp}","sdpMid":"${ice.sdpMid}","sdpMLineIndex":${ice.sdpMLineIndex}}"""
                        callApi.updateCallWithSignaling(
                            callId = callId,
                            iceCandidates = listOf(iceObj)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    // Stub out other PeerConnection.Observer methods
    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
    override fun onAddStream(stream: MediaStream?) {}
    override fun onRemoveStream(stream: MediaStream?) {}
    override fun onDataChannel(dataChannel: org.webrtc.DataChannel?) {}
    override fun onRenegotiationNeeded() {}
}
