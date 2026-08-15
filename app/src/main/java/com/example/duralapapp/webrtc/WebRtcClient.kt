package com.example.duralapapp.webrtc

import android.content.Context
import android.util.Log
import com.example.duralapapp.data.model.CallType
import com.example.duralapapp.data.model.IceServerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import javax.inject.Inject
import javax.inject.Singleton

sealed class WebRtcEvent {
    data class OnIceCandidateGenerated(val candidate: IceCandidate) : WebRtcEvent()
    data class OnConnectionStateChanged(val state: PeerConnection.PeerConnectionState) : WebRtcEvent()
    data class OnIceConnectionChanged(val state: PeerConnection.IceConnectionState) : WebRtcEvent()
    data class OnIceGatheringChanged(val state: PeerConnection.IceGatheringState) : WebRtcEvent()
    data class OnSignalingStateChanged(val state: PeerConnection.SignalingState) : WebRtcEvent()
    data class OnRemoteVideoTrackAdded(val track: VideoTrack) : WebRtcEvent()
    data class OnRemoteAudioTrackAdded(val track: AudioTrack) : WebRtcEvent()
}

@Singleton
class WebRtcClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WebRtcClient"
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FPS = 30
        private const val AUDIO_TRACK_ID = "ARDAMSa0"
        private const val VIDEO_TRACK_ID = "ARDAMSv0"
    }

    val eglBase: EglBase = EglBase.create()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var remoteVideoTrack: VideoTrack? = null
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    private val _events = MutableSharedFlow<WebRtcEvent>(extraBufferCapacity = 128)
    val events = _events.asSharedFlow()

    private val queuedRemoteCandidates = mutableListOf<IceCandidate>()
    @Volatile
    private var isRemoteDescriptionSet = false

    private var activeCallId: String = ""
    private var activeRole: String = ""

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true, // enableIntelVp8Encoder
            true  // enableH264HighProfile
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()

        Log.i(TAG, "PeerConnectionFactory initialized with Hardware Codecs & JavaAudioDeviceModule")
    }

    fun initSurfaceViewRenderer(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
    }

    fun startSession(
        callType: CallType,
        iceServers: List<IceServerConfig>,
        role: String = "PEER",
        callId: String = ""
    ) {
        activeCallId = callId
        activeRole = role

        Log.i(TAG, "======================================================")
        Log.i(TAG, "[WEBRTC_SESSION_START] Call: $callId | Role: $role | Type: $callType")
        Log.i(TAG, "ICE Servers count: ${iceServers.size}")
        iceServers.forEach { config ->
            Log.d(TAG, "  ICE URL: ${config.urls} (username=${config.username})")
        }
        Log.i(TAG, "======================================================")

        synchronized(queuedRemoteCandidates) {
            isRemoteDescriptionSet = false
            queuedRemoteCandidates.clear()
        }

        // Close any existing peer connection before starting a new session
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        val rtcIceServers = iceServers.map { config ->
            val builder = PeerConnection.IceServer.builder(config.urls)
            if (!config.username.isNullOrBlank()) {
                builder.setUsername(config.username)
            }
            if (!config.credential.isNullOrBlank()) {
                builder.setPassword(config.credential)
            }
            builder.createIceServer()
        }.ifEmpty {
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
            )
        }

        val rtcConfig = PeerConnection.RTCConfiguration(rtcIceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 2
        }

        fun parseCandidateType(sdp: String): String {
            val typIndex = sdp.indexOf("typ ")
            if (typIndex != -1) {
                val remainder = sdp.substring(typIndex + 4)
                return remainder.substringBefore(" ").trim()
            }
            return "unknown"
        }

        val logState = {
            val pc = peerConnection
            val sig = pc?.signalingState()?.name ?: "NULL"
            val ice = pc?.iceConnectionState()?.name ?: "NULL"
            val gathering = pc?.iceGatheringState()?.name ?: "NULL"
            val conn = pc?.connectionState()?.name ?: "NULL"
            Log.i(TAG, "[WEBRTC_STATE] callId=$activeCallId | role=$activeRole | SignalingState=$sig | IceGatheringState=$gathering | IceConnectionState=$ice | PeerConnectionState=$conn")
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val candType = parseCandidateType(candidate.sdp)
                Log.i(TAG, "[onIceCandidate] Generated local candidate: type=$candType | mid=${candidate.sdpMid} | line=${candidate.sdpMLineIndex} | sdp=${candidate.sdp.take(50)}...")
                _events.tryEmit(WebRtcEvent.OnIceCandidateGenerated(candidate))
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Log.d(TAG, "[onIceCandidatesRemoved] Removed ${candidates?.size ?: 0} local ICE Candidates")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.i(TAG, "[PeerConnectionState] Changed -> $newState")
                logState()
                _events.tryEmit(WebRtcEvent.OnConnectionStateChanged(newState))
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.i(TAG, "[IceConnectionState] Changed -> $newState")
                logState()
                _events.tryEmit(WebRtcEvent.OnIceConnectionChanged(newState))
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                Log.i(TAG, "[IceGatheringState] Changed -> $newState")
                logState()
                _events.tryEmit(WebRtcEvent.OnIceGatheringChanged(newState))
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState) {
                Log.i(TAG, "[SignalingState] Changed -> $newState")
                logState()
                _events.tryEmit(WebRtcEvent.OnSignalingStateChanged(newState))
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "[IceConnectionReceiving] receiving=$receiving")
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                Log.i(TAG, "[onTrack] received: id=${track?.id()}, kind=${track?.kind()}")
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                    remoteRenderer?.let { track.addSink(it) }
                    _events.tryEmit(WebRtcEvent.OnRemoteVideoTrackAdded(track))
                } else if (track is AudioTrack) {
                    track.setEnabled(true)
                    _events.tryEmit(WebRtcEvent.OnRemoteAudioTrackAdded(track))
                }
            }

            override fun onAddStream(stream: MediaStream) {
                Log.i(TAG, "[onAddStream] audioTracks=${stream.audioTracks.size}, videoTracks=${stream.videoTracks.size}")
                if (stream.videoTracks.isNotEmpty()) {
                    val track = stream.videoTracks[0]
                    remoteVideoTrack = track
                    remoteRenderer?.let { track.addSink(it) }
                    _events.tryEmit(WebRtcEvent.OnRemoteVideoTrackAdded(track))
                }
                if (stream.audioTracks.isNotEmpty()) {
                    val track = stream.audioTracks[0]
                    track.setEnabled(true)
                    _events.tryEmit(WebRtcEvent.OnRemoteAudioTrackAdded(track))
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "[onRemoveStream]")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "[onRenegotiationNeeded]")
            }
        })

        // Setup local audio track
        val audioConstraints = MediaConstraints()
        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, localAudioSource)
        localAudioTrack?.setEnabled(true)
        localAudioTrack?.let { peerConnection?.addTrack(it) }
        Log.i(TAG, "[MEDIA] Local AudioTrack added to PeerConnection")

        // Setup local video track if VIDEO call
        if (callType == CallType.VIDEO) {
            setupLocalVideo()
        }
    }

    private fun setupLocalVideo() {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames

        // Prefer front-facing camera
        val frontDevice = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: deviceNames.firstOrNull()

        if (frontDevice != null) {
            videoCapturer = enumerator.createCapturer(frontDevice, null)
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            localVideoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)

            videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
            videoCapturer?.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)

            localVideoTrack = peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, localVideoSource)
            localVideoTrack?.setEnabled(true)
            localVideoTrack?.let { peerConnection?.addTrack(it) }

            localRenderer?.let { localVideoTrack?.addSink(it) }
            Log.i(TAG, "[MEDIA] Local VideoTrack added to PeerConnection (device=$frontDevice)")
        } else {
            Log.w(TAG, "[MEDIA] No camera device found on hardware")
        }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        localRenderer = renderer
        localVideoTrack?.addSink(renderer)
        Log.d(TAG, "[RENDERER] Attached local renderer")
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        remoteRenderer = renderer
        remoteVideoTrack?.addSink(renderer)
        Log.d(TAG, "[RENDERER] Attached remote renderer")
    }

    fun setMicrophoneMute(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
        Log.d(TAG, "[MEDIA] Microphone muted: $muted")
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        Log.d(TAG, "[MEDIA] Camera enabled: $enabled")
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
        Log.d(TAG, "[MEDIA] Switched camera front/back")
    }

    fun createOffer(callType: CallType, onOfferCreated: (SessionDescription) -> Unit) {
        Log.i(TAG, "[createOffer] started for callType=$callType")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (callType == CallType.VIDEO) "true" else "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.i(TAG, "[createOffer] SUCCESS | type=${sdp.type} | length=${sdp.description.length}")
                Log.i(TAG, "[setLocalDescription(offer)] setting local offer SDP...")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.i(TAG, "[setLocalDescription(offer)] SUCCESS")
                        scope.launch(Dispatchers.Main) {
                            onOfferCreated(sdp)
                        }
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "[setLocalDescription(offer)] FAILED: $error")
                    }
                }, sdp) ?: Log.e(TAG, "[setLocalDescription(offer)] peerConnection is null!")
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[createOffer] FAILED: $error")
            }
            override fun onSetFailure(p0: String?) {}
        }, constraints) ?: Log.e(TAG, "[createOffer] peerConnection is null!")
    }

    fun createAnswer(callType: CallType, onAnswerCreated: (SessionDescription) -> Unit) {
        Log.i(TAG, "[createAnswer] started for callType=$callType")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (callType == CallType.VIDEO) "true" else "false"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                Log.i(TAG, "[createAnswer] SUCCESS | type=${sdp.type} | length=${sdp.description.length}")
                Log.i(TAG, "[setLocalDescription(answer)] setting local answer SDP...")
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.i(TAG, "[setLocalDescription(answer)] SUCCESS")
                        scope.launch(Dispatchers.Main) {
                            onAnswerCreated(sdp)
                        }
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "[setLocalDescription(answer)] FAILED: $error")
                    }
                }, sdp) ?: Log.e(TAG, "[setLocalDescription(answer)] peerConnection is null!")
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "[createAnswer] FAILED: $error")
            }
            override fun onSetFailure(p0: String?) {}
        }, constraints) ?: Log.e(TAG, "[createAnswer] peerConnection is null!")
    }

    fun setRemoteDescription(sdp: SessionDescription, onSetSuccess: (() -> Unit)? = null) {
        val tag = if (sdp.type == SessionDescription.Type.OFFER) "[setRemoteDescription(offer)]" else "[setRemoteDescription(answer)]"
        Log.i(TAG, "$tag started | type=${sdp.type} | length=${sdp.description.length}")
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.i(TAG, "$tag SUCCESS")
                synchronized(queuedRemoteCandidates) {
                    isRemoteDescriptionSet = true
                    drainQueuedCandidates()
                }
                scope.launch(Dispatchers.Main) {
                    onSetSuccess?.invoke()
                }
            }

            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "$tag FAILED: error=$error")
            }
        }, sdp) ?: Log.e(TAG, "$tag peerConnection is null!")
    }

    private fun parseCandidateType(sdp: String): String {
        val typIndex = sdp.indexOf("typ ")
        if (typIndex != -1) {
            val remainder = sdp.substring(typIndex + 4)
            return remainder.substringBefore(" ").trim()
        }
        return "unknown"
    }

    fun addIceCandidate(candidate: IceCandidate) {
        val candType = parseCandidateType(candidate.sdp)
        synchronized(queuedRemoteCandidates) {
            if (isRemoteDescriptionSet) {
                Log.i(TAG, "[addIceCandidate] type=$candType | mid=${candidate.sdpMid} | line=${candidate.sdpMLineIndex}")
                peerConnection?.addIceCandidate(candidate)
            } else {
                Log.i(TAG, "[addIceCandidate] Remote description not set yet. Queuing candidate: type=$candType | mid=${candidate.sdpMid} (queueSize=${queuedRemoteCandidates.size + 1})")
                queuedRemoteCandidates.add(candidate)
            }
        }
    }

    private fun drainQueuedCandidates() {
        synchronized(queuedRemoteCandidates) {
            Log.i(TAG, "[addIceCandidate] Draining ${queuedRemoteCandidates.size} queued ICE candidates")
            for (candidate in queuedRemoteCandidates) {
                val candType = parseCandidateType(candidate.sdp)
                Log.i(TAG, "[addIceCandidate] (drained) type=$candType | mid=${candidate.sdpMid} | line=${candidate.sdpMLineIndex}")
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteCandidates.clear()
        }
    }

    fun endSession() {
        Log.i(TAG, "[WEBRTC_SESSION_END] Cleaning up WebRTC session for call $activeCallId")
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping capturer", e)
        }

        videoCapturer?.dispose()
        videoCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        localVideoSource?.dispose()
        localVideoSource = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        localAudioSource?.dispose()
        localAudioSource = null

        remoteVideoTrack = null
        localRenderer = null
        remoteRenderer = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        synchronized(queuedRemoteCandidates) {
            isRemoteDescriptionSet = false
            queuedRemoteCandidates.clear()
        }

        activeCallId = ""
        activeRole = ""
    }
}
