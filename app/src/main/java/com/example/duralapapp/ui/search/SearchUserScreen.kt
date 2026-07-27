package com.example.duralapapp.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.duralapapp.data.model.UserResponse
import com.example.duralapapp.ui.common.rememberShimmerBrush
import com.example.duralapapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUserScreen(
    onBackClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedUserForRequest by remember { mutableStateOf<UserResponse?>(null) }
    var initialMessageText by remember { mutableStateOf("") }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is RequestActionState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetActionState()
                selectedUserForRequest = null
                initialMessageText = ""
            }
            is RequestActionState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                selectedUserForRequest = null
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = {
                            Text(
                                "Search by username or email...",
                                color = PlaceholderColor,
                                fontSize = 16.sp
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Search for people to start chatting",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMuted
                        )
                    }
                }
                is SearchUiState.Searching -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(5) {
                            SearchUserShimmerItem()
                        }
                    }
                }
                is SearchUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No users found for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                        ) {
                            items(state.users, key = { it.id }) { user ->
                                UserSearchResultItem(
                                    user = user,
                                    isRequestPending = actionState is RequestActionState.Loading && (actionState as RequestActionState.Loading).targetUserId == user.id,
                                    onConnectClick = { selectedUserForRequest = user }
                                )
                            }
                        }
                    }
                }
                is SearchUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Dialog for sending conversation request with initial message
    selectedUserForRequest?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUserForRequest = null },
            containerColor = CardBg,
            title = {
                Text(
                    text = "Connect with @${user.username}",
                    color = TextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Send a conversation request to start messaging.",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = initialMessageText,
                        onValueChange = { initialMessageText = it },
                        placeholder = { Text("Add an optional message...", color = PlaceholderColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = InputBorderColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                val isSending = actionState is RequestActionState.Loading
                Button(
                    onClick = {
                        viewModel.sendConversationRequest(
                            targetUserId = user.id,
                            initialMessage = initialMessageText.ifBlank { null }
                        )
                    },
                    enabled = !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Request", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserForRequest = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun UserSearchResultItem(
    user: UserResponse,
    isRequestPending: Boolean,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName ?: user.username,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMain,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isRequestPending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryBlue,
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Connect",
                        tint = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun SearchUserShimmerItem() {
    val shimmerBrush = rememberShimmerBrush()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}
