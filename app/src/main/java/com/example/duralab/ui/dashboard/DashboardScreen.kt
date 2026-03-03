package com.example.duralab.ui.dashboard

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.duralab.data.local.entity.ChatEntity
import com.example.duralab.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val recentChatsState by viewModel.recentChats.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Camera Hole Mockup
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 15.dp)
                .size(14.dp)
                .background(Color.Black, CircleShape)
                .zOrder(50f)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            StickyHeader(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                onProfileClick = onNavigateToProfile
            )

            // Chat List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                ) {
                    when (recentChatsState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is UiState.Success -> {
                            val chats = (recentChatsState as UiState.Success<List<ChatEntity>>).data
                            val filteredChats = if (searchQuery.isBlank()) {
                                chats
                            } else {
                                chats.filter { it.participantId.contains(searchQuery, ignoreCase = true) }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedPadding(4.dp)
                            ) {
                                items(filteredChats) { chat ->
                                    ChatListItem(
                                        chat = chat,
                                        onClick = { onNavigateToChat(chat.id) }
                                    )
                                }
                            }
                        }
                        is UiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Error loading chats", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Android Pill Mockup
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(width = 80.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun StickyHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            .padding(top = 44.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Duralap",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                    )
                )
                Text(
                    text = "CONNECT ANYWHERE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Profile Image with Rotation
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .rotate(3f)
                    .clickable { onProfileClick() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shadowElevation = 2.dp
            ) {
                AsyncImage(
                    model = "https://i.pravatar.cc/100?u=myprofile",
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-3f),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search chats...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatEntity,
    onClick: () -> Unit
) {
    val unreadCount = chat.unreadCount
    val isUnread = unreadCount > 0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = if (isUnread) Color.White else Color.Transparent,
        shadowElevation = if (isUnread) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Active Indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = "https://i.pravatar.cc/100?u=${chat.participantId}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                // Active status ring (Mocked as always active for this UI)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.participantId, // Mocking name with participantId
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "10:30 AM", // Mocked timestamp
                        fontSize = 10.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage ?: "No messages yet",
                        fontSize = 13.sp,
                        color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                    )

                    if (isUnread) {
                        Surface(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Extension to handle Arrangement.spacedBy with padding
fun Arrangement.spacedPadding(space: androidx.compose.ui.unit.Dp): Arrangement.Vertical {
    return Arrangement.spacedBy(space)
}

// Utility for Z-ordering in Box (using zIndex modifier in actual implementation)
@Composable
fun Modifier.zOrder(index: Float): Modifier = this.then(Modifier.zIndex(index))

@Composable
fun Modifier.zIndex(index: Float): Modifier = this.then(androidx.compose.ui.zIndex(index))

// Gradient text helper
fun Modifier.brush(brush: Brush): Modifier = this.then(
    // In actual Compose, you'd use TextStyle brush or a custom Modifier.drawWithContent
    // Since Text composable doesn't directly take a modifier for brush, 
    // we'll use a wrapper or assume experimental API usage here for brevity or just plain text if not possible.
    // Actually, Material 3 Text supports Brush in TextStyle.
    this
)

// Helper for gradient text color
@Composable
fun Text(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit,
    brush: Brush,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = TextStyle(brush = brush),
        letterSpacing = letterSpacing,
        modifier = modifier
    )
}

