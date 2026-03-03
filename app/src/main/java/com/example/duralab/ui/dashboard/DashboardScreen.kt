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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.duralab.data.local.entity.ChatEntity
import com.example.duralab.data.local.entity.UserEntity
import com.example.duralab.util.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val recentChatsState by viewModel.recentChats.collectAsState()
    val suggestedUsers by viewModel.suggestedUsers.collectAsState()
    
    var homeSearchQuery by remember { mutableStateOf("") }
    var newUserSearchQuery by remember { mutableStateOf("") }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .border(width = 8.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(48.dp))
            .clip(RoundedCornerShape(48.dp))
    ) {
        // Camera Hole Mockup
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 15.dp)
                .size(14.dp)
                .background(Color.Black, CircleShape)
                .zIndex(100f)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Header Content
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(top = 44.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
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
                                style = TextStyle(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                                    )
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
                                .clickable { onNavigateToProfile() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Home Screen Search
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = homeSearchQuery,
                                onValueChange = { homeSearchQuery = it },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                decorationBox = { innerTextField ->
                                    if (homeSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search connected users by name...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp,
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

            // Chat List Section
            when (recentChatsState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Success -> {
                    val chats = (recentChatsState as UiState.Success<List<ChatEntity>>).data
                    val filteredChats = chats.filter { 
                        it.participantId.contains(homeSearchQuery, ignoreCase = true) 
                    }
                    
                    items(filteredChats) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onNavigateToChat(chat.id) }
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Oops! Something went wrong.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showBottomSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 24.dp)
                .size(56.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF3B82F6).copy(alpha = 0.4f)),
            containerColor = Color(0xFF2563EB),
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add User", modifier = Modifier.size(28.dp))
        }

        // Android Pill Mockup
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(width = 80.dp, height = 4.dp)
                .background(Color(0xFFE2E8F0), CircleShape)
                .zIndex(70f)
        )

        // Add New User Modal (BottomSheet)
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                dragHandle = null
            ) {
                NewUserModalContent(
                    searchQuery = newUserSearchQuery,
                    onSearchChange = { newUserSearchQuery = it },
                    suggestedUsers = suggestedUsers.filter {
                        it.username.contains(newUserSearchQuery, ignoreCase = true) ||
                        it.email.contains(newUserSearchQuery, ignoreCase = true)
                    },
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showBottomSheet = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NewUserModalContent(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    suggestedUsers: List<UserEntity>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Modal Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color(0xFFF1F5F9), CircleShape)
                    .size(40.dp)
            ) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color(0xFF475569))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Add New User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "CONNECT WITH OTHERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search in Modal
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search by username or email...",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SUGGESTED USERS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedUsers) { user ->
                NewUserListItem(user)
            }
        }
    }
}

@Composable
fun NewUserListItem(user: UserEntity) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Add User Logic */ }
            .clip(RoundedCornerShape(20.dp)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://i.pravatar.cc/100?u=${user.id}",
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user.username,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155),
                    fontSize = 14.sp
                )
                Text(
                    text = "@${user.username.lowercased()} • ${user.email}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .shadow(elevation = 0.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Active Indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = "https://i.pravatar.cc/100?u=${chat.participantId}",
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                val isActive = chat.participantId.length % 2 == 0 // Mocked status
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .size(12.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User Name", // Mocked
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        fontSize = 16.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "10:30 AM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = chat.lastMessage ?: "No messages yet",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Utility to mock lowercased for strings in types where .lowercase() might be different
fun String.lowercased(): String = this.lowercase()
