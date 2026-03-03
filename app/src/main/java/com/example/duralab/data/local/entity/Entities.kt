package com.example.duralab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: String, // SENT, DELIVERED, READ
    val type: String // TEXT, IMAGE, DOCUMENT
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val profilePicture: String?,
    val status: String, // online, offline, away, busy
    val lastSeen: Long?
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val participantId: String,
    val unreadCount: Int = 0
)
