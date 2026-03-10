package com.example.duralab.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CallScreen(
    userId: String,
    viewModel: CallViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()

    LaunchedEffect(userId) {
        if (callState == CallState.IDLE) {
            viewModel.initiateCall(userId)
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Placeholder for WebRTC VideoSurfaceView
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Video Surface (WebRTC)",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (Status & Target User)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Text(
                        text = "User $userId",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (callState) {
                            CallState.IDLE -> "Ready..."
                            CallState.OUTGOING -> "Calling..."
                            CallState.INCOMING -> "Incoming Call..."
                            CallState.CONNECTED -> "Connected"
                            CallState.ENDED -> "Call Ended"
                        },
                        color = Color.LightGray,
                        fontSize = 16.sp
                    )
                }

                // Controls (Accept / End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (callState == CallState.INCOMING) {
                        FloatingActionButton(
                            onClick = { viewModel.acceptIncomingCall() },
                            containerColor = Color.Green,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Accept Call", tint = Color.White)
                        }
                    }

                    FloatingActionButton(
                        onClick = { 
                            viewModel.endCall()
                            onNavigateBack()
                        },
                        containerColor = Color.Red,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                    }
                }
            }
        }
    }
}
