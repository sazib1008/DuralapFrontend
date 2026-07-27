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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duralapapp.ui.theme.*

data class ChatItem(
    val id: Int,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isRead: Boolean = false,
    val avatarColor: Color = Color(0xFF1E293B)
)

@Composable
fun HomeScreen(
    onOpenProfile: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    onOpenChat: (conversationId: String, recipientName: String) -> Unit = { _, _ -> },
    viewModel: com.example.duralapapp.ui.chat.ChatListViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val chats = listOf(
        ChatItem(1, "Elena Gilbert", "The final designs are re...", "14:20", unreadCount = 2, avatarColor = Color(0xFF334155)),
        ChatItem(2, "Design Team", "Marcus: Check out the n...", "12:05", isRead = true, avatarColor = Color(0xFF0D9488)),
        ChatItem(3, "Alex Rivera", "See you at the meeting!", "Yesterday", isRead = true, avatarColor = Color(0xFF1E293B))
    )

    Scaffold(
        containerColor = BgDark,
        topBar = {
            DuralapTopBar(onOpenProfile = onOpenProfile)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* New Chat */ },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            DuralapBottomNavigation(onOpenProfile = onOpenProfile)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Recent Chats",
                style = MaterialTheme.typography.titleLarge,
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(chats) { chat ->
                    ChatListItem(chat)
                }
            }
        }
    }
}

@Composable
fun DuralapTopBar(onOpenProfile: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Custom Logo Icon (simplified version of the circles)
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BubbleChart,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Duralap",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF818CF8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

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
            IconButton(onClick = onOpenProfile) {
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
}

@Composable
fun ChatListItem(chat: ChatItem) {
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
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(chat.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = chat.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (chat.unreadCount > 0) Color(0xFF818CF8) else TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF818CF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (chat.isRead) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuralapBottomNavigation(onOpenProfile: () -> Unit = {}) {
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
                isSelected = true
            )
            BottomNavItem(
                icon = Icons.Outlined.Phone,
                label = "Calls",
                isSelected = false
            )
            BottomNavItem(
                icon = Icons.Outlined.Contacts,
                label = "Contacts",
                isSelected = false
            )
            BottomNavItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                isSelected = false,
                onClick = onOpenProfile
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF818CF8) else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF818CF8) else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun HomeScreenPreview() {
    DuralapAppTheme {
        HomeScreen()
    }
}
