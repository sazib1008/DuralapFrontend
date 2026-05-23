package com.example.duralapapp.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duralapapp.ui.theme.*

data class CallRecord(
    val id: Int,
    val name: String,
    val type: CallType,
    val direction: CallDirection,
    val time: String,
    val avatarColor: Color = Color(0xFF1E293B)
)

enum class CallType {
    VOICE, VIDEO
}

enum class CallDirection {
    INCOMING, OUTGOING, MISSED
}

@Composable
fun CallsScreen() {
    val calls = listOf(
        CallRecord(1, "Alex Rivera", CallType.VIDEO, CallDirection.INCOMING, "12 mins ago, 15:42", Color(0xFF1E293B)),
        CallRecord(2, "Elena Gilbert", CallType.VOICE, CallDirection.MISSED, "2 hours ago, 13:20", Color(0xFF334155)),
        CallRecord(3, "Design Team", CallType.VIDEO, CallDirection.OUTGOING, "Yesterday, 18:05", Color(0xFF0D9488)),
        CallRecord(4, "Michael Chen", CallType.VOICE, CallDirection.INCOMING, "Yesterday, 14:12", Color(0xFF1E293B))
    )

    Scaffold(
        containerColor = BgDark,
        topBar = {
            CallsTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* New Call */ },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "New Call",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            DuralapBottomNavigation(currentTab = "Calls")
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
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = PrimaryBlue,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "All", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "Missed", color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "RECENT CALLS",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(calls) { call ->
                    CallListItem(call)
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* Search */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CallListItem(call: CallRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with type badge
            Box(modifier = Modifier.size(56.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(call.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                        .align(Alignment.BottomEnd)
                        .border(2.dp, CardBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (call.type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = call.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (call.direction == CallDirection.MISSED) Color(0xFFF87171) else TextMain,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(call.direction) {
                            CallDirection.INCOMING -> Icons.Default.CallReceived
                            CallDirection.OUTGOING -> Icons.Default.CallMade
                            CallDirection.MISSED -> Icons.Default.CallMissed
                        },
                        contentDescription = null,
                        tint = if (call.direction == CallDirection.MISSED) Color(0xFFF87171) else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${call.direction.name.lowercase().replaceFirstChar { it.uppercase() }} • ${call.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Action Button
            IconButton(
                onClick = { /* Call Back */ },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = if (call.type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Call",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DuralapBottomNavigation(currentTab: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = BgDark,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.ChatBubble,
                label = "Chats",
                isSelected = currentTab == "Chats"
            )
            BottomNavItem(
                icon = Icons.Default.Call,
                label = "Calls",
                isSelected = currentTab == "Calls"
            )
            BottomNavItem(
                icon = Icons.Default.Contacts,
                label = "Contacts",
                isSelected = currentTab == "Contacts"
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentTab == "Settings"
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun CallsScreenPreview() {
    DuralapAppTheme {
        CallsScreen()
    }
}
