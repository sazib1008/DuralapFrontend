package com.example.duralab.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class CallHistoryItem(
    val id: String,
    val name: String,
    val image: String?,
    val type: String, // "outgoing", "missed", "incoming"
    val callType: String, // "video", "audio"
    val time: String,
    val duration: String
)

fun formatCallTime(timeString: String): String {
    return try {
        // Try parsing ISO8601 with or without microseconds
        var format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        var date = format.parse(timeString)
        
        if (date == null) {
             format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
             format.timeZone = TimeZone.getTimeZone("UTC")
             date = format.parse(timeString)
        }
        
        if (date != null) {
            val outFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            outFormat.format(date)
        } else {
            timeString.take(10)
        }
    } catch (e: Exception) {
        timeString.take(10)
    }
}

fun formatCallDuration(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return "00:00"
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCall: (String) -> Unit, // Navigates to a target user
    viewModel: CallHistoryViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    val callHistoryList = remember(uiState) {
        if (uiState is CallHistoryUiState.Success) {
            val history = (uiState as CallHistoryUiState.Success).history
            history.map { response ->
                val type = if (response.status.name == "MISSED") {
                    "missed"
                } else if (response.isIncoming) {
                    "incoming"
                } else {
                    "outgoing"
                }
                
                CallHistoryItem(
                    id = response.id,
                    name = response.otherUser?.username ?: "Unknown",
                    image = response.otherUser?.profilePicture,
                    type = type,
                    callType = response.callType.name.lowercase(),
                    time = formatCallTime(response.createdAt),
                    duration = formatCallDuration(response.duration)
                )
            }
        } else {
            emptyList()
        }
    }

    val filteredHistory = callHistoryList.filter { call ->
        val nameMatch = call.name.contains(searchQuery, ignoreCase = true)
        val tabMatch = if (activeTab == "All") true else call.type == "missed"
        nameMatch && tabMatch
    }

    Scaffold(
        containerColor = Color(0xFF171717) // neutral-900 background for list container feel
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF171717).copy(alpha = 0.8f)) // neutral-900/80
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recents",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { /* More Options */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFFA3A3A3)) // neutral-400
                    }
                }

                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .background(Color(0xFF262626), RoundedCornerShape(12.dp)) // neutral-800
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF737373), modifier = Modifier.size(18.dp)) // neutral-500
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color(0xFF3B82F6)), // blue-500
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search calls...", color = Color(0xFF737373), fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF262626), RoundedCornerShape(12.dp)) // neutral-800
                        .padding(4.dp)
                ) {
                    val tabs = listOf("All", "Missed")
                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF404040) else Color.Transparent) // neutral-700
                                .clickable { activeTab = tab }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color(0xFF737373), // neutral-500
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Call List
            when (uiState) {
                is CallHistoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                }
                is CallHistoryUiState.Error -> {
                    val errorMessage = (uiState as CallHistoryUiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(errorMessage, color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadCallHistory() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is CallHistoryUiState.Success -> {
                    if (filteredHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "No History",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF525252).copy(alpha = 0.5f) // neutral-600 with opacity
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No call history found", color = Color(0xFF525252), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredHistory) { call ->
                                CallHistoryItemView(call = call, onCallClick = {
                                    // Navigate to the user call layout with exact ID if available
                                    onNavigateToCall(call.name)
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryItemView(call: CallHistoryItem, onCallClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* Open Details */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .background(Color(0xFF404040)), // neutral-700
                contentAlignment = Alignment.Center
            ) {
                if (call.image != null) {
                    AsyncImage(
                        model = call.image,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Unknown", tint = Color(0xFFA3A3A3), modifier = Modifier.size(20.dp)) // neutral-400
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column {
                Text(
                    text = call.name,
                    color = if (call.type == "missed") Color(0xFFF87171) else Color.White, // red-400
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconVector = when (call.type) {
                        "incoming" -> Icons.AutoMirrored.Filled.CallReceived
                        "outgoing" -> Icons.AutoMirrored.Filled.CallMade
                        "missed" -> Icons.AutoMirrored.Filled.PhoneMissed
                        else -> Icons.Default.Call
                    }
                    val iconTint = when (call.type) {
                        "incoming" -> Color(0xFF22C55E) // green-500
                        "outgoing" -> Color(0xFF3B82F6) // blue-500
                        "missed" -> Color(0xFFEF4444) // red-500
                        else -> Color(0xFF737373)
                    }
                    
                    Icon(imageVector = iconVector, contentDescription = call.type, tint = iconTint, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = call.time, color = Color(0xFF737373), fontSize = 12.sp) // neutral-500
                    
                    if (call.duration != "00:00") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "•", color = Color(0xFF404040), fontSize = 12.sp) // neutral-700
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = call.duration, color = Color(0xFF737373), fontSize = 12.sp) // neutral-500
                    }
                }
            }
        }

        // Call Action Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF262626)) // background neutral-800
                .clickable { onCallClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (call.callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Call",
                tint = Color(0xFF60A5FA), // text-blue-400
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
