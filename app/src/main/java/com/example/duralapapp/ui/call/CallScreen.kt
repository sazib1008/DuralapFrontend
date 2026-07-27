package com.example.duralapapp.ui.call

import androidx.compose.foundation.BorderStroke
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.duralapapp.data.model.CallType
import com.example.duralapapp.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    onBackClick: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()

    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current

    LaunchedEffect(uiState) {
        if (uiState is CallUiState.Ended) {
            kotlinx.coroutines.delay(1500)
            onBackClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Main Viewport (Video preview or Avatar)
        when (val state = uiState) {
            is CallUiState.OutgoingRinging -> {
                CallRingingView(name = state.recipientName, status = "Ringing...")
            }
            is CallUiState.IncomingRinging -> {
                CallRingingView(name = state.callerName, status = "Incoming call...")
            }
            is CallUiState.Connected -> {
                if (isVideoEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Remote Video Stream", color = TextMuted, fontSize = 16.sp)
                    }
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
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Self Video", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    CallRingingView(
                        name = state.recipientName,
                        status = formatDuration(state.durationSeconds)
                    )
                }
            }
            is CallUiState.Ended -> {
                CallRingingView(name = viewModel.targetUserName, status = state.reason)
            }
            is CallUiState.Idle -> {
                CallRingingView(name = viewModel.targetUserName, status = "Connecting...")
            }
        }

        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                title = { Text("Call Permissions Needed", color = TextMain, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Duralap requires Camera and Microphone permissions to establish real-time audio and video calls with your contact.",
                        color = TextMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showPermissionRationale = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Grant", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationale = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = CardBg,
                shape = RoundedCornerShape(24.dp)
            )
        }

        // Control Bar at Bottom
        if (uiState !is CallUiState.Ended) {
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
                        icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        isActive = !isVideoEnabled,
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
                            viewModel.toggleCamera()
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
