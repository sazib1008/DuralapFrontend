package com.example.duralapapp.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.duralapapp.data.model.MessageResponse
import com.example.duralapapp.data.model.MessageStatus
import com.example.duralapapp.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBackClick: () -> Unit,
    onStartCall: (targetUserId: String, targetUserName: String, conversationId: String, callType: String) -> Unit = { _, _, _, _ -> },
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isUserOnline.collectAsStateWithLifecycle()
    val lastSeen by viewModel.lastSeen.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                        .align(Alignment.BottomEnd)
                                        .border(1.5.dp, Color(0xFF1E293B), CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = viewModel.recipientName,
                                color = TextMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (isOnline) {
                                Text(
                                    text = "Online",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "Offline",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
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
                actions = {
                    IconButton(onClick = {
                        onStartCall(viewModel.targetUserId, viewModel.recipientName, viewModel.conversationId, "AUDIO")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Audio Call",
                            tint = PrimaryBlue
                        )
                    }
                    IconButton(onClick = {
                        onStartCall(viewModel.targetUserId, viewModel.recipientName, viewModel.conversationId, "VIDEO")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        },
        bottomBar = {
            Surface(
                color = CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...", color = PlaceholderColor) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputBg,
                            unfocusedContainerColor = InputBg,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = InputBorderColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    val haptics = LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ChatDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                is ChatDetailUiState.Success -> {
                    SideEffect {
                        android.util.Log.d("DEBUG_STOMP", "[5] Compose recomposed | ConversationId: ${viewModel.conversationId} | messages.size: ${state.messages.size}")
                    }
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(state.messages.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            val isFromMe = msg.senderId == state.currentUserId
                            ChatMessageBubble(message = msg, isFromMe = isFromMe)
                        }
                    }
                }
                is ChatDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: MessageResponse,
    isFromMe: Boolean
) {
    val timeFormatted = remember(message.createdAt) {
        try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
            formatter.format(message.createdAt)
        } catch (e: Exception) {
            ""
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isFromMe) PrimaryBlue else CardBg
            ),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isFromMe) 18.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 18.dp
            ),
            border = if (!isFromMe) BorderStroke(1.dp, BorderColor) else null,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    color = TextMain,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        color = if (isFromMe) Color.White.copy(alpha = 0.7f) else TextMuted,
                        fontSize = 11.sp
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (icon, tint) = when {
                            message.isRead || message.status == MessageStatus.READ -> Pair(Icons.Default.DoneAll, Color(0xFF38BDF8))
                            message.status == MessageStatus.DELIVERED -> Pair(Icons.Default.DoneAll, Color.White.copy(alpha = 0.7f))
                            else -> Pair(Icons.Default.Done, Color.White.copy(alpha = 0.7f))
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

