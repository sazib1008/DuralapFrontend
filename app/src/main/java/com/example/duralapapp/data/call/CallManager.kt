package com.example.duralapapp.data.call

import android.util.Log
import com.example.duralapapp.data.model.*
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.CallRepository
import com.example.duralapapp.webrtc.AppAudioManager
import com.example.duralapapp.webrtc.WebRtcClient
import com.example.duralapapp.webrtc.WebRtcEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed class CallState {
    data object Idle : CallState()

    data class OutgoingRinging(
        val callId: String,
        val targetUserId: String,
        val contactName: String,
        val contactAvatar: String?,
        val callType: CallType,
        val conversationId: String
    ) : CallState()

    data class IncomingRinging(
        val callId: String,
        val callerId: String,
        val callerName: String,
        val callerAvatar: String?,
        val callType: CallType,
        val conversationId: String
    ) : CallState()

    data class Connecting(
        val callId: String,
        val targetUserId: String,
        val contactName: String,
        val contactAvatar: String?,
        val callType: CallType,
        val isIncoming: Boolean
    ) : CallState()

    data class Connected(
        val callId: String,
        val targetUserId: String,
        val contactName: String,
        val contactAvatar: String?,
        val callType: CallType,
        val startTime: Instant = Instant.now(),
        val durationSeconds: Long = 0L,
        val isAudioMuted: Boolean = false,
        val isVideoEnabled: Boolean = true,
        val isSpeakerphoneOn: Boolean = false,
        val isFrontCamera: Boolean = true
    ) : CallState()

    data class Ended(
        val reason: String,
        val wasMissed: Boolean = false
    ) : CallState()
}

@Singleton
class CallManager @Inject constructor(
    private val callRepository: CallRepository,
    private val tokenManager: TokenManager,
    val webRtcClient: WebRtcClient,
    val audioManager: AppAudioManager
) {
    companion object {
        private const val TAG = "CallManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var currentUserId: String? = null
    private var signalingJob: Job? = null
    private var durationTimerJob: Job? = null

    init {
        scope.launch {
            tokenManager.userId.collect { userId ->
                currentUserId = userId
                if (!userId.isNullOrBlank()) {
                    startObservingSignaling(userId)
                } else {
                    stopObservingSignaling()
                }
            }
        }

        observeWebRtcEvents()
    }

    private fun startObservingSignaling(userId: String) {
        signalingJob?.cancel()
        signalingJob = scope.launch {
            Log.i(TAG, "Starting signaling observation for user: $userId")
            callRepository.observeUserSignaling(userId).collect { message ->
                handleIncomingSignal(message)
            }
        }
    }

    private fun stopObservingSignaling() {
        signalingJob?.cancel()
        signalingJob = null
    }

    private fun observeWebRtcEvents() {
        scope.launch {
            webRtcClient.events.collect { event ->
                when (event) {
                    is WebRtcEvent.OnIceCandidateGenerated -> {
                        val state = _callState.value
                        val targetUserId = when (state) {
                            is CallState.OutgoingRinging -> state.targetUserId
                            is CallState.Connecting -> state.targetUserId
                            is CallState.Connected -> state.targetUserId
                            else -> null
                        }
                        val callId = when (state) {
                            is CallState.OutgoingRinging -> state.callId
                            is CallState.Connecting -> state.callId
                            is CallState.Connected -> state.callId
                            else -> null
                        }

                        if (targetUserId != null && callId != null && currentUserId != null) {
                            val candidateSignal = CallSignalingMessage(
                                callId = callId,
                                senderId = currentUserId!!,
                                targetUserId = targetUserId,
                                signalType = SignalType.ICE_CANDIDATE,
                                iceCandidate = event.candidate.sdp,
                                sdpMid = event.candidate.sdpMid,
                                sdpMLineIndex = event.candidate.sdpMLineIndex
                            )
                            Log.i(TAG, "[ICE candidate sent] to $targetUserId | callId=$callId | mid=${event.candidate.sdpMid} | line=${event.candidate.sdpMLineIndex}")
                            callRepository.sendSignalingMessage(candidateSignal)
                        }
                    }

                    is WebRtcEvent.OnConnectionStateChanged -> {
                        Log.i(TAG, "[PeerConnectionState] -> ${event.state}")
                        if (event.state == PeerConnection.PeerConnectionState.CONNECTED) {
                            transitionToConnected()
                        } else if (event.state == PeerConnection.PeerConnectionState.FAILED) {
                            Log.e(TAG, "[PeerConnectionState] FAILED")
                            if (_callState.value is CallState.Connected || _callState.value is CallState.Connecting) {
                                endCallInternal("CONNECTION_FAILED")
                            }
                        }
                    }

                    is WebRtcEvent.OnIceConnectionChanged -> {
                        Log.i(TAG, "[IceConnectionState] -> ${event.state}")
                        if (event.state == PeerConnection.IceConnectionState.CONNECTED ||
                            event.state == PeerConnection.IceConnectionState.COMPLETED) {
                            transitionToConnected()
                        } else if (event.state == PeerConnection.IceConnectionState.FAILED) {
                            Log.e(TAG, "[IceConnectionState] FAILED")
                            if (_callState.value is CallState.Connected || _callState.value is CallState.Connecting) {
                                endCallInternal("ICE_FAILED")
                            }
                        }
                    }

                    is WebRtcEvent.OnIceGatheringChanged -> {
                        Log.i(TAG, "[IceGatheringState] -> ${event.state}")
                    }

                    is WebRtcEvent.OnSignalingStateChanged -> {
                        Log.i(TAG, "[SignalingState] -> ${event.state}")
                    }

                    is WebRtcEvent.OnRemoteVideoTrackAdded -> {
                        Log.i(TAG, "[onTrack] Remote VideoTrack added and attached to renderer")
                    }

                    is WebRtcEvent.OnRemoteAudioTrackAdded -> {
                        Log.i(TAG, "[onTrack] Remote AudioTrack added and enabled")
                    }
                }
            }
        }
    }

    private suspend fun handleIncomingSignal(message: CallSignalingMessage) {
        val role = when (_callState.value) {
            is CallState.OutgoingRinging -> "CALLER"
            is CallState.IncomingRinging -> "CALLEE"
            is CallState.Connecting -> if ((_callState.value as CallState.Connecting).isIncoming) "CALLEE" else "CALLER"
            is CallState.Connected -> "PEER"
            else -> "UNKNOWN"
        }

        val stateName = _callState.value::class.simpleName ?: "UnknownState"
        Log.i(TAG, "[SIGNALING_RECV] callId=${message.callId} | userId=$currentUserId | role=$role | callState=$stateName | signalType=${message.signalType} | from=${message.senderId} | to=${message.targetUserId}")

        when (message.signalType) {
            SignalType.CALL_INITIATE -> {
                val currentState = _callState.value
                if (currentState !is CallState.Idle && currentState !is CallState.Ended) {
                    Log.w(TAG, "[CALL_INITIATE] Callee busy, rejecting call ${message.callId}")
                    val busySignal = CallSignalingMessage(
                        callId = message.callId,
                        senderId = currentUserId ?: "",
                        targetUserId = message.senderId,
                        signalType = SignalType.CALL_BUSY,
                        callType = message.callType,
                        conversationId = message.conversationId,
                        reason = "CALLEE_BUSY"
                    )
                    callRepository.sendSignalingMessage(busySignal)
                    return
                }

                _callState.value = CallState.IncomingRinging(
                    callId = message.callId,
                    callerId = message.senderId,
                    callerName = message.callerName ?: "Incoming Call",
                    callerAvatar = message.callerAvatar,
                    callType = message.callType,
                    conversationId = message.conversationId ?: ""
                )
            }

            SignalType.CALL_ACCEPT -> {
                val currentState = _callState.value
                if (currentState is CallState.OutgoingRinging && currentState.callId == message.callId) {
                    Log.i(TAG, "[CALL_ACCEPT] received on Caller. Transitioning to Connecting and creating Offer...")
                    _callState.value = CallState.Connecting(
                        callId = currentState.callId,
                        targetUserId = currentState.targetUserId,
                        contactName = currentState.contactName,
                        contactAvatar = currentState.contactAvatar,
                        callType = currentState.callType,
                        isIncoming = false
                    )

                    // Caller creates WebRTC Offer
                    webRtcClient.createOffer(currentState.callType) { sessionDescription ->
                        val offerSignal = CallSignalingMessage(
                            callId = message.callId,
                            senderId = currentUserId ?: "",
                            targetUserId = message.senderId,
                            signalType = SignalType.WEBRTC_OFFER,
                            callType = currentState.callType,
                            sdp = sessionDescription.description
                        )
                        Log.i(TAG, "[offer sent] to ${message.senderId} | callId=${message.callId} | sdpLength=${sessionDescription.description.length}")
                        callRepository.sendSignalingMessage(offerSignal)
                    }
                }
            }

            SignalType.WEBRTC_OFFER -> {
                val currentState = _callState.value
                val callId = when (currentState) {
                    is CallState.Connecting -> currentState.callId
                    is CallState.IncomingRinging -> currentState.callId
                    is CallState.Connected -> currentState.callId
                    else -> null
                }
                val callType = when (currentState) {
                    is CallState.Connecting -> currentState.callType
                    is CallState.IncomingRinging -> currentState.callType
                    is CallState.Connected -> currentState.callType
                    else -> message.callType
                }

                if (callId == message.callId && message.sdp != null) {
                    Log.i(TAG, "[offer received] from ${message.senderId} | callId=${message.callId}")
                    val sdp = SessionDescription(SessionDescription.Type.OFFER, message.sdp)
                    webRtcClient.setRemoteDescription(sdp) {
                        webRtcClient.createAnswer(callType) { answerDescription ->
                            val answerSignal = CallSignalingMessage(
                                callId = message.callId,
                                senderId = currentUserId ?: "",
                                targetUserId = message.senderId,
                                signalType = SignalType.WEBRTC_ANSWER,
                                callType = callType,
                                sdp = answerDescription.description
                            )
                            Log.i(TAG, "[answer sent] to ${message.senderId} | callId=${message.callId} | sdpLength=${answerDescription.description.length}")
                            callRepository.sendSignalingMessage(answerSignal)
                        }
                    }
                } else {
                    Log.w(TAG, "[offer received] Dropped WEBRTC_OFFER: callId mismatch ($callId vs ${message.callId}) or null SDP")
                }
            }

            SignalType.WEBRTC_ANSWER -> {
                val currentState = _callState.value
                val callId = when (currentState) {
                    is CallState.Connecting -> currentState.callId
                    is CallState.OutgoingRinging -> currentState.callId
                    is CallState.Connected -> currentState.callId
                    else -> null
                }

                if (callId == message.callId && message.sdp != null) {
                    Log.i(TAG, "[answer received] from ${message.senderId} | callId=${message.callId}")
                    val sdp = SessionDescription(SessionDescription.Type.ANSWER, message.sdp)
                    webRtcClient.setRemoteDescription(sdp) {
                        Log.i(TAG, "[setRemoteDescription(answer)] Remote description applied on Caller for callId=${message.callId}")
                    }
                } else {
                    Log.w(TAG, "[answer received] Dropped WEBRTC_ANSWER: callId mismatch ($callId vs ${message.callId}) or null SDP")
                }
            }

            SignalType.ICE_CANDIDATE -> {
                if (message.iceCandidate != null && message.sdpMid != null && message.sdpMLineIndex != null) {
                    Log.i(TAG, "[ICE candidate received] from ${message.senderId} | callId=${message.callId} | mid=${message.sdpMid} | line=${message.sdpMLineIndex}")
                    val candidate = IceCandidate(message.sdpMid, message.sdpMLineIndex, message.iceCandidate)
                    webRtcClient.addIceCandidate(candidate)
                }
            }

            SignalType.CALL_REJECT -> {
                Log.i(TAG, "[CALL_REJECT] Call rejected by peer: ${message.reason}")
                endCallInternal(message.reason ?: "CALL_REJECTED")
            }

            SignalType.CALL_BUSY -> {
                Log.i(TAG, "[CALL_BUSY] Callee busy")
                endCallInternal("USER_BUSY")
            }

            SignalType.CALL_CANCEL -> {
                Log.i(TAG, "[SIGNALING] Call cancelled by caller")
                endCallInternal("CANCELLED_BY_CALLER")
            }

            SignalType.CALL_END -> {
                Log.i(TAG, "[SIGNALING] Call ended by peer")
                endCallInternal("CALL_ENDED")
            }

            SignalType.CALL_TIMEOUT -> {
                Log.i(TAG, "[SIGNALING] Call timed out (no answer)")
                endCallInternal("NO_ANSWER", wasMissed = true)
            }

            SignalType.CALL_RINGING -> {
                Log.d(TAG, "[SIGNALING] Callee is ringing for call ${message.callId}")
            }

            else -> {
                Log.d(TAG, "[SIGNALING] Unhandled signal type: ${message.signalType}")
            }
        }
    }

    fun startCall(
        targetUserId: String,
        targetUserName: String,
        targetUserAvatar: String?,
        conversationId: String,
        callType: CallType
    ) {
        val callerId = currentUserId ?: return

        Log.i(TAG, "======================================================")
        Log.i(TAG, "[CALL_START] Initiating $callType call to $targetUserId ($targetUserName)")
        Log.i(TAG, "======================================================")

        scope.launch {
            val defaultIce = listOf(
                IceServerConfig(urls = listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))
            )

            val response = callRepository.initiateCall(
                conversationId = conversationId,
                callerId = callerId,
                calleeId = targetUserId,
                callType = callType
            )

            response.onSuccess { call ->
                Log.i(TAG, "[CALL_START] Backend initiate succeeded. CallId=${call.id}")

                val serverIce = callRepository.getIceServers().getOrNull()?.iceServers ?: defaultIce
                webRtcClient.startSession(callType, serverIce, role = "CALLER", callId = call.id)
                audioManager.start(isSpeakerphoneDefault = callType == CallType.VIDEO)

                _callState.value = CallState.OutgoingRinging(
                    callId = call.id,
                    targetUserId = targetUserId,
                    contactName = targetUserName,
                    contactAvatar = targetUserAvatar,
                    callType = callType,
                    conversationId = conversationId
                )
            }.onFailure { error ->
                Log.e(TAG, "[CALL_START] Backend initiate failed", error)
                _callState.value = CallState.Ended(error.message ?: "Failed to start call")
            }
        }
    }

    fun acceptCall() {
        val currentState = _callState.value
        if (currentState !is CallState.IncomingRinging) return

        val myUserId = currentUserId ?: return
        val callId = currentState.callId
        val callerId = currentState.callerId
        val callerName = currentState.callerName
        val callerAvatar = currentState.callerAvatar
        val callType = currentState.callType
        val conversationId = currentState.conversationId

        Log.i(TAG, "======================================================")
        Log.i(TAG, "[CALL_ACCEPT] Accepting call $callId from $callerId")
        Log.i(TAG, "======================================================")

        // 1. Immediately transition state to Connecting so UI updates without waiting
        _callState.value = CallState.Connecting(
            callId = callId,
            targetUserId = callerId,
            contactName = callerName,
            contactAvatar = callerAvatar,
            callType = callType,
            isIncoming = true
        )

        // 2. Start WebRTC session & audio immediately so peerConnection is ready before Offer arrives
        val defaultIce = listOf(
            IceServerConfig(urls = listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))
        )
        webRtcClient.startSession(callType, defaultIce, role = "CALLEE", callId = callId)
        audioManager.start(isSpeakerphoneDefault = callType == CallType.VIDEO)

        // 3. In background: fetch dynamic ICE servers, send STOMP CALL_ACCEPT, and call REST accept
        scope.launch {
            callRepository.getIceServers().onSuccess { iceResp ->
                if (iceResp.iceServers.isNotEmpty()) {
                    Log.d(TAG, "[ICE_CONFIG] Loaded ${iceResp.iceServers.size} ICE servers from backend")
                }
            }

            val acceptSignal = CallSignalingMessage(
                callId = callId,
                senderId = myUserId,
                targetUserId = callerId,
                signalType = SignalType.CALL_ACCEPT,
                callType = callType,
                conversationId = conversationId
            )
            Log.i(TAG, "[SIGNALING] Dispatched CALL_ACCEPT -> $callerId for call $callId")
            callRepository.sendSignalingMessage(acceptSignal)

            callRepository.acceptCall(callId, myUserId)
        }
    }

    fun rejectCall(reason: String = "DECLINED") {
        val currentState = _callState.value
        if (currentState !is CallState.IncomingRinging) return

        val myUserId = currentUserId ?: return

        Log.i(TAG, "[CALL_REJECT] Rejecting call ${currentState.callId}")
        scope.launch {
            val rejectSignal = CallSignalingMessage(
                callId = currentState.callId,
                senderId = myUserId,
                targetUserId = currentState.callerId,
                signalType = SignalType.CALL_REJECT,
                callType = currentState.callType,
                conversationId = currentState.conversationId,
                reason = reason
            )
            callRepository.sendSignalingMessage(rejectSignal)
            callRepository.rejectCall(currentState.callId, myUserId)
            endCallInternal("DECLINED")
        }
    }

    fun cancelCall() {
        val currentState = _callState.value
        if (currentState !is CallState.OutgoingRinging) return

        val myUserId = currentUserId ?: return

        Log.i(TAG, "[CALL_CANCEL] Cancelling call ${currentState.callId}")
        scope.launch {
            val cancelSignal = CallSignalingMessage(
                callId = currentState.callId,
                senderId = myUserId,
                targetUserId = currentState.targetUserId,
                signalType = SignalType.CALL_CANCEL,
                callType = currentState.callType,
                conversationId = currentState.conversationId
            )
            callRepository.sendSignalingMessage(cancelSignal)
            callRepository.cancelCall(currentState.callId, myUserId)
            endCallInternal("CANCELLED_BY_CALLER")
        }
    }

    fun hangUp() {
        val currentState = _callState.value
        val (callId, targetUserId) = when (currentState) {
            is CallState.OutgoingRinging -> Pair(currentState.callId, currentState.targetUserId)
            is CallState.Connecting -> Pair(currentState.callId, currentState.targetUserId)
            is CallState.Connected -> Pair(currentState.callId, currentState.targetUserId)
            else -> return
        }

        val myUserId = currentUserId ?: return

        Log.i(TAG, "[CALL_END] Hanging up call $callId to $targetUserId")
        scope.launch {
            val endSignal = CallSignalingMessage(
                callId = callId,
                senderId = myUserId,
                targetUserId = targetUserId,
                signalType = SignalType.CALL_END
            )
            callRepository.sendSignalingMessage(endSignal)
            callRepository.endCall(callId, myUserId)
            endCallInternal("HUNG_UP")
        }
    }

    fun toggleMicrophone(): Boolean {
        val currentState = _callState.value
        if (currentState is CallState.Connected) {
            val newMute = !currentState.isAudioMuted
            webRtcClient.setMicrophoneMute(newMute)
            _callState.value = currentState.copy(isAudioMuted = newMute)
            return newMute
        }
        return false
    }

    fun toggleCamera(): Boolean {
        val currentState = _callState.value
        if (currentState is CallState.Connected) {
            val newVideo = !currentState.isVideoEnabled
            webRtcClient.setCameraEnabled(newVideo)
            _callState.value = currentState.copy(isVideoEnabled = newVideo)
            return newVideo
        }
        return false
    }

    fun switchCamera() {
        val currentState = _callState.value
        if (currentState is CallState.Connected) {
            webRtcClient.switchCamera()
            _callState.value = currentState.copy(isFrontCamera = !currentState.isFrontCamera)
        }
    }

    fun toggleSpeakerphone(): Boolean {
        val currentState = _callState.value
        if (currentState is CallState.Connected) {
            val newSpeaker = !currentState.isSpeakerphoneOn
            audioManager.setSpeakerphone(newSpeaker)
            _callState.value = currentState.copy(isSpeakerphoneOn = newSpeaker)
            return newSpeaker
        }
        return false
    }

    private fun transitionToConnected() {
        val currentState = _callState.value
        if (currentState is CallState.Connected) return // Already connected

        val (callId, targetUserId, name, avatar, callType) = when (currentState) {
            is CallState.Connecting -> Triple5(currentState.callId, currentState.targetUserId, currentState.contactName, currentState.contactAvatar, currentState.callType)
            is CallState.OutgoingRinging -> Triple5(currentState.callId, currentState.targetUserId, currentState.contactName, currentState.contactAvatar, currentState.callType)
            is CallState.IncomingRinging -> Triple5(currentState.callId, currentState.callerId, currentState.callerName, currentState.callerAvatar, currentState.callType)
            else -> return
        }

        Log.i(TAG, "======================================================")
        Log.i(TAG, "[CALL_CONNECTED] Real WebRTC media channel established! CallId: $callId, Target: $targetUserId")
        Log.i(TAG, "======================================================")

        _callState.value = CallState.Connected(
            callId = callId,
            targetUserId = targetUserId,
            contactName = name,
            contactAvatar = avatar,
            callType = callType,
            startTime = Instant.now(),
            isSpeakerphoneOn = audioManager.isSpeakerphoneOn()
        )

        startDurationTimer()
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            var seconds = 0L
            while (isActive) {
                delay(1000)
                seconds++
                val state = _callState.value
                if (state is CallState.Connected) {
                    _callState.value = state.copy(durationSeconds = seconds)
                } else {
                    break
                }
            }
        }
    }

    private fun endCallInternal(reason: String, wasMissed: Boolean = false) {
        Log.i(TAG, "[CALL_CLEANUP] Ending call session (reason=$reason, wasMissed=$wasMissed)")
        durationTimerJob?.cancel()
        durationTimerJob = null

        webRtcClient.endSession()
        audioManager.stop()

        _callState.value = CallState.Ended(reason = reason, wasMissed = wasMissed)

        scope.launch {
            delay(2000)
            if (_callState.value is CallState.Ended) {
                _callState.value = CallState.Idle
            }
        }
    }

    private data class Triple5(
        val callId: String,
        val targetUserId: String,
        val contactName: String,
        val contactAvatar: String?,
        val callType: CallType
    )
}
