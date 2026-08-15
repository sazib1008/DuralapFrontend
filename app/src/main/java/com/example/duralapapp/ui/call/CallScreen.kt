package com.example.duralapapp.ui.call

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.duralapapp.data.call.CallState
import com.example.duralapapp.data.model.CallType
import com.example.duralapapp.ui.theme.*
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    onBackClick: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()

    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    val haptics = LocalHapticFeedback.current

    // Request permissions on entry
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions result handled by system */ }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (viewModel.initialCallTypeStr == "VIDEO") {
            permissions.add(Manifest.permission.CAMERA)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(callState) {
        if (callState is CallState.Ended) {
            kotlinx.coroutines.delay(1500)
            onBackClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        when (val state = callState) {
            is CallState.OutgoingRinging -> {
                CallRingingView(name = state.contactName, status = "Ringing...")
            }
            is CallState.IncomingRinging -> {
                CallRingingView(name = state.callerName, status = "Incoming call...")
            }
            is CallState.Connecting -> {
                CallRingingView(name = state.contactName, status = "Connecting WebRTC...")
            }
            is CallState.Connected -> {
                if (state.callType == CallType.VIDEO && state.isVideoEnabled) {
                    // Remote Fullscreen Video
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).apply {
                                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                setMirror(false)
                                viewModel.initRemoteRenderer(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Local PIP Draggable Video
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 20.dp)
                            .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    pipOffsetX += dragAmount.x
                                    pipOffsetY += dragAmount.y
                                }
                            }
                            .size(width = 110.dp, height = 160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).apply {
                                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                    setMirror(state.isFrontCamera)
                                    viewModel.initLocalRenderer(this)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Duration overlay in top left
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 48.dp, start = 20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = formatDuration(state.durationSeconds),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    CallRingingView(
                        name = state.contactName,
                        status = formatDuration(state.durationSeconds)
                    )
                }
            }
            is CallState.Ended -> {
                CallRingingView(name = viewModel.targetUserName, status = state.reason)
            }
            is CallState.Idle -> {
                CallRingingView(name = viewModel.targetUserName, status = "Connecting...")
            }
        }

        // Control Bar at Bottom
        if (callState !is CallState.Ended) {
            val isMuted = (callState as? CallState.Connected)?.isAudioMuted ?: false
            val isVideoOn = (callState as? CallState.Connected)?.isVideoEnabled ?: (viewModel.initialCallTypeStr == "VIDEO")
            val isSpeakerOn = (callState as? CallState.Connected)?.isSpeakerphoneOn ?: (viewModel.initialCallTypeStr == "VIDEO")

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = CardBg.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        isActive = isMuted,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleMute()
                        }
                    )

                    CallControlButton(
                        icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        isActive = !isVideoOn,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleVideo()
                        }
                    )

                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        isActive = isSpeakerOn,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleSpeaker()
                        }
                    )

                    CallControlButton(
                        icon = Icons.Default.Cameraswitch,
                        isActive = false,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.switchCamera()
                        }
                    )

                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.endCall()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallRingingView(name: String, status: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(CardBg)
                .border(2.dp, PrimaryBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = TextMain,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = status,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (isActive) PrimaryBlue else Color(0xFF1E293B))
            .border(1.dp, BorderColor, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.White else TextMuted,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
