package com.example.duralab.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val onlineUsersState by viewModel.onlineUsers.collectAsState()
    val recentChatsState by viewModel.recentChats.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Duralap", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { onNavigateToProfile() },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Duralap Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // Online Users Section
                Text("Online Users", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                when (onlineUsersState) {
                    is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    is UiState.Success -> {
                        val users = (onlineUsersState as UiState.Success<List<UserEntity>>).data
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(users) { user ->
                                UserStatusItem(user)
                            }
                        }
                    }
                    is UiState.Error -> Text("Error loading users", modifier = Modifier.padding(16.dp))
                }

                // Recent Chats Section
                Text("Recent Conversations", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                when (recentChatsState) {
                    is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    is UiState.Success -> {
                        val chats = (recentChatsState as UiState.Success<List<ChatEntity>>).data
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(chats) { chat ->
                                ChatListItem(chat, onClick = { onNavigateToChat(chat.id) })
                            }
                        }
                    }
                    is UiState.Error -> Text("Error loading chats", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun UserStatusItem(user: UserEntity) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(12.dp))
        }
        Text(user.username, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ChatListItem(chat: ChatEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("Chat with ${chat.participantId}") },
        supportingContent = { Text(chat.lastMessage ?: "No messages yet") },
        trailingContent = { Text(chat.unreadCount.toString().takeIf { it != "0" } ?: "") },
        modifier = Modifier.clickable { onClick() }
    )
}
