package com.example.duralapapp.ui.main

import com.example.duralapapp.data.model.MessageStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duralapapp.ui.theme.*

data class Message(
    val id: Int,
    val text: String,
    val time: String,
    val isFromMe: Boolean,
    val status: MessageStatus = MessageStatus.SENT
)

@Composable
fun ChatScreen(
    onBack: () -> Unit = {}
) {
    val messages = listOf(
        Message(1, "Hey! Did you get a chance to look at the new design system specs? I think the indigo accents look amazing.", "10:24 AM", false),
        Message(2, "Just finished reviewing them. The glassmorphism effects on the app bar are exactly what we needed.", "10:26 AM", true, MessageStatus.READ),
        Message(3, "Ready to start the implementation? 🚀", "10:26 AM", true, MessageStatus.READ),
        Message(4, "Absolutely. Let's hop on a quick call to sync on the motion tokens first. Can you talk now?", "10:27 AM", false)
    )

    Scaffold(
        containerColor = BgDark,
        topBar = {
            ChatTopBar(onBack = onBack)
        },
        bottomBar = {
            MessageInput()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Today",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
                
                item {
                    IncomingCallCard()
                }
            }
        }
    }
}

@Composable
fun ChatTopBar(isOnline: Boolean = false, onBack: () -> Unit) {
    Surface(
        color = BgDark.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextMain)
            }
            
            // Avatar with online status
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TextMuted)
                }
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .align(Alignment.BottomEnd)
                            .border(2.dp, BgDark, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Alex Rivera", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = if (isOnline) "Online" else "Offline", color = TextMuted, fontSize = 12.sp)
            }
            
            IconButton(onClick = { /* Call */ }) {
                Icon(imageVector = Icons.Default.Phone, contentDescription = "Call", tint = TextMuted)
            }
            IconButton(onClick = { /* Video */ }) {
                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video", tint = TextMuted)
            }
            IconButton(onClick = { /* More */ }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", tint = TextMuted)
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val bubbleColor = if (message.isFromMe) PrimaryBlue else Color(0xFF1E293B)
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val shape = if (message.isFromMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = TextMain,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.time,
                color = TextMuted,
                fontSize = 11.sp
            )
            if (message.isFromMe) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = if (message.status == MessageStatus.READ) Color(0xFF818CF8) else TextMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun IncomingCallCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.PhoneCallback, contentDescription = null, tint = TextMain)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Incoming Audio Call", color = TextMain, fontWeight = FontWeight.Bold)
                Text(text = "Duralap HD Audio", color = TextMuted, fontSize = 13.sp)
            }
            
            // Call Actions
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { /* Decline */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { /* Answer */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryBlue, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Answer", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun MessageInput() {
    Surface(
        color = BgDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = TextMuted)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Message...", color = TextMuted, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.SentimentSatisfiedAlt, contentDescription = "Emoji", tint = TextMuted)
            }
            
            IconButton(
                onClick = { /* Send */ },
                modifier = Modifier
                    .size(52.dp)
                    .background(PrimaryBlue, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun ChatScreenPreview() {
    DuralapAppTheme {
        ChatScreen()
    }
}
