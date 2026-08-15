package com.example.duralapapp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.duralapapp.data.model.CallHistoryItemResponse
import com.example.duralapapp.data.model.CallStatus
import com.example.duralapapp.data.model.CallType
import com.example.duralapapp.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CallsScreen(
    onStartCall: (targetUserId: String, targetUserName: String, conversationId: String, callType: String) -> Unit = { _, _, _, _ -> },
    viewModel: CallsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Missed

    Scaffold(
        containerColor = BgDark,
        topBar = {
            CallsTopBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTab = 0 },
                    color = if (selectedTab == 0) PrimaryBlue else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "All",
                            color = if (selectedTab == 0) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTab = 1 },
                    color = if (selectedTab == 1) PrimaryBlue else Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Missed",
                            color = if (selectedTab == 1) Color.White else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (selectedTab == 0) "RECENT CALLS" else "MISSED CALLS",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is CallsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                is CallsUiState.Success -> {
                    val filteredCalls = if (selectedTab == 1) {
                        state.calls.filter { it.status == CallStatus.MISSED }
                    } else {
                        state.calls
                    }

                    if (filteredCalls.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (selectedTab == 1) "No Missed Calls" else "No Recent Calls",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextMain,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your voice and video call logs will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(filteredCalls, key = { it.id }) { callItem ->
                                CallHistoryRowItem(
                                    call = callItem,
                                    onCallBack = {
                                        val otherId = if (callItem.isIncoming) callItem.callerId else callItem.calleeId
                                        val name = callItem.otherUser?.fullName?.takeIf { it.isNotBlank() }
                                            ?: callItem.otherUser?.username ?: otherId
                                        onStartCall(otherId, name, callItem.conversationId, callItem.callType.name)
                                    }
                                )
                            }
                        }
                    }
                }
                is CallsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadCallHistory() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Calls",
            style = MaterialTheme.typography.titleLarge,
            color = TextMain,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
    }
}

@Composable
fun CallHistoryRowItem(
    call: CallHistoryItemResponse,
    onCallBack: () -> Unit
) {
    val isMissed = call.status == CallStatus.MISSED
    val displayName = call.otherUser?.fullName?.takeIf { it.isNotBlank() }
        ?: call.otherUser?.username
        ?: if (call.isIncoming) call.callerId else call.calleeId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Call Type Badge
            Box(modifier = Modifier.size(52.dp)) {
                if (!call.otherUser?.profileImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = call.otherUser!!.profileImageUrl,
                        contentDescription = displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (call.callType == CallType.VIDEO) PrimaryBlue else Color(0xFF10B981))
                        .align(Alignment.BottomEnd)
                        .border(2.dp, CardBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (call.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Contact Name & Status/Timestamp
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isMissed) Color(0xFFF87171) else TextMain,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isMissed -> Icons.Default.CallMissed
                            call.isIncoming -> Icons.Default.CallReceived
                            else -> Icons.Default.CallMade
                        },
                        contentDescription = null,
                        tint = if (isMissed) Color(0xFFF87171) else if (call.isIncoming) Color(0xFF10B981) else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val timeFormatted = formatTimestamp(call.createdAt)
                    val durationText = if (call.duration != null && call.duration > 0) {
                        " • ${call.duration / 60}m ${call.duration % 60}s"
                    } else ""

                    Text(
                        text = "$timeFormatted$durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Call Back Icon Button
            IconButton(
                onClick = onCallBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = if (call.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Call Back",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(instant: Instant): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "Recent"
    }
}
