package com.example.duralab.ui.call

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    userId: String,
    viewModel: CallViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    
    // UI Local States
    var timerSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(false) } // Defaulting to Audio Call
    var isSpeakerOn by remember { mutableStateOf(false) }
    
    val userDisplayName = "User $userId" 
    val callerImageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=800&q=80"

    LaunchedEffect(userId) {
        if (callState == CallState.IDLE) {
            viewModel.initiateCall(userId)
        }
    }

    // Timer logic
    LaunchedEffect(callState) {
        if (callState == CallState.CONNECTED) {
            timerSeconds = 0
            while (true) {
                delay(1000L)
                timerSeconds++
            }
        } else {
            timerSeconds = 0
        }
    }
    
    // Auto navigate back when ended
    LaunchedEffect(callState) {
        if (callState == CallState.ENDED) {
            delay(2000L) // Wait 2 seconds before closing the screen to let user see "Ended"
            onNavigateBack()
        }
    }

    val formatTime = { seconds: Int ->
        val mins = seconds / 60
        val secs = seconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Layer
            if (!isVideoEnabled) {
                // Blurred background for audio call feel
                AsyncImage(
                    model = callerImageUrl,
                    contentDescription = "Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.25f)
                        .blur(30.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { alpha = 0.6f },
                    contentScale = ContentScale.Crop
                )
            } else {
                // Video Surface Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF262626)), // neutral-800
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White)
                        }
                        Text(
                            text = "VIDEO STREAM ACTIVE",
                            color = Color.White.copy(alpha = 0.1f),
                            fontSize = 10.sp,
                            letterSpacing = 4.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            }
            
            // Audio Mode Content (Avatar and Pulses)
            if (!isVideoEnabled || callState != CallState.CONNECTED) {
                Box(
                    modifier = Modifier.fillMaxSize(), 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        
                        Box(contentAlignment = Alignment.Center) {
                            // Outgoing/Incoming Pulse
                            if (callState == CallState.OUTGOING || callState == CallState.INCOMING) {
                                val infiniteTransition = rememberInfiniteTransition(label = "outgoing_pulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.5f,
                                    animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Restart),
                                    label = "scale"
                                )
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Restart),
                                    label = "alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(176.dp)
                                        .scale(scale)
                                        .background(Color(0xFF3B82F6).copy(alpha = alpha), CircleShape) // blue-500/30 base
                                )
                            }
                            
                            // Connected Pulse
                            if (callState == CallState.CONNECTED && !isMuted) {
                                val infiniteTransition = rememberInfiniteTransition(label = "connected_pulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.1f,
                                    animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                                    label = "scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(176.dp)
                                        .scale(scale)
                                        .background(Color(0xFF22C55E).copy(alpha = 0.1f), CircleShape) // green-500/10 base
                                )
                            }
                            
                            val isEnded = callState == CallState.ENDED
                            val colorMatrix = if (isEnded) ColorMatrix().apply { setToSaturation(0f) } else null
                            
                            AsyncImage(
                                model = callerImageUrl,
                                contentDescription = "Caller Avatar",
                                modifier = Modifier
                                    .size(176.dp)
                                    .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .clip(CircleShape)
                                    .graphicsLayer { 
                                        alpha = if (isEnded) 0.5f else 1f 
                                        shadowElevation = 60f
                                    },
                                contentScale = ContentScale.Crop,
                                colorFilter = colorMatrix?.let { ColorFilter.colorMatrix(it) }
                            )
                        }
                        
                        // Waveforms
                        if (callState == CallState.CONNECTED) {
                            Spacer(modifier = Modifier.height(48.dp))
                            Row(
                                modifier = Modifier.height(24.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "waveforms")
                                for (i in 0 until 5) {
                                    val heightMultiplier by if (!isMuted) {
                                        infiniteTransition.animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(300 + (i * 100), easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "height_$i"
                                        )
                                    } else {
                                        remember { mutableFloatStateOf(0.15f) }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .fillMaxHeight(heightMultiplier)
                                            .background(
                                                Color(0xFF60A5FA).copy(alpha = if (isMuted) 0.3f else 1f), // blue-400
                                                CircleShape
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isMuted) "MICROPHONE MUTED" else "VOICE CONNECTION ACTIVE",
                                color = Color(0xFF60A5FA).copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }

            // PiP Video View
            if (isVideoEnabled && callState == CallState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 24.dp)
                        .size(width = 112.dp, height = 160.dp)
                        .background(Color(0xFF262626), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF171717)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = "Self View", tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            // UI Overlay (Gradients & Controls)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = 32.dp, vertical = 64.dp)
            ) {
                
                // Header (Status & Timer)
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = userDisplayName,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val statusColor by animateColorAsState(if (callState == CallState.CONNECTED) Color(0xFF4ADE80) else Color(0xFF60A5FA), label = "statusColor")
                    
                    val stateText = when (callState) {
                        CallState.IDLE -> "READY..."
                        CallState.OUTGOING -> "CALLING..."
                        CallState.INCOMING -> "INCOMING CALL..."
                        CallState.CONNECTED -> "CONNECTED"
                        CallState.ENDED -> "CALL ENDED"
                    }
                    Text(
                        text = stateText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    
                    if (callState == CallState.CONNECTED) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Little dot indicators
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp, 12.dp).background(Color.White.copy(alpha=0.4f), CircleShape))
                                Box(modifier = Modifier.size(4.dp, 12.dp).background(Color.White.copy(alpha=0.8f), CircleShape))
                                Box(modifier = Modifier.size(4.dp, 12.dp).background(Color.White.copy(alpha=0.4f), CircleShape))
                            }
                            Text(
                                text = formatTime(timerSeconds),
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Controls Container
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Call Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (callState == CallState.INCOMING) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                                    .clickable { viewModel.acceptIncomingCall() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFEF4444).copy(alpha = if (callState == CallState.ENDED) 0.3f else 1f), CircleShape)
                                .clickable { 
                                    viewModel.endCall()
                                    // Let the LaunchedEffect handle navigation after 2 seconds
                                    if (callState != CallState.CONNECTED && callState != CallState.INCOMING && callState != CallState.OUTGOING) {
                                        onNavigateBack() // Fallback if already ENDED or IDLE
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // In-Call Settings Grid
                    if (callState == CallState.CONNECTED) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(40.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(40.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { isMuted = !isMuted },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, 
                                        contentDescription = "Mute", 
                                        tint = if (isMuted) Color(0xFFF87171) else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Text("MUTE", color = if (isMuted) Color(0xFFF87171) else Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            // Video
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isVideoEnabled) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { isVideoEnabled = !isVideoEnabled },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, 
                                        contentDescription = "Video", 
                                        tint = if (isVideoEnabled) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Text("VIDEO", color = if (isVideoEnabled) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Speaker
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isSpeakerOn) Color(0xFF22C55E).copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { isSpeakerOn = !isSpeakerOn },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown, 
                                        contentDescription = "Speaker", 
                                        tint = if (isSpeakerOn) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Text("SPEAKER", color = if (isSpeakerOn) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            // More
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Transparent, RoundedCornerShape(16.dp))
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White.copy(alpha = 0.8f))
                                }
                                Text("MORE", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
