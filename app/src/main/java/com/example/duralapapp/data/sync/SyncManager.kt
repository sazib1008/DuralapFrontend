package com.example.duralapapp.data.sync

import android.util.Log
import com.example.duralapapp.data.local.PendingMessageQueue
import com.example.duralapapp.data.model.MessageResponse
import com.example.duralapapp.data.model.MessageUiStatus
import com.example.duralapapp.data.network.TokenManager
import com.example.duralapapp.data.repository.ChatRepository
import com.example.duralapapp.data.websocket.StompWebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val chatRepository: ChatRepository,
    private val stompClient: StompWebSocketClient,
    private val tokenManager: TokenManager,
    private val pendingQueue: PendingMessageQueue
) {
    private val tag = "SyncManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastSyncTimestamp: Instant? = null

    private val _syncedMessages = MutableSharedFlow<List<MessageResponse>>(extraBufferCapacity = 64)
    val syncedMessages: SharedFlow<List<MessageResponse>> = _syncedMessages.asSharedFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Observe WebSocket connection state for automatic initial sync & pending queue retry
        scope.launch {
            stompClient.connectionState.collect { isConnected ->
                if (isConnected) {
                    Log.d(tag, "WebSocket reconnected. Performing initial REST synchronization...")
                    performInitialSync()
                    flushPendingQueue()
                }
            }
        }
    }

    /**
     * Triggers initial REST synchronization on app startup or reconnect.
     * Calls GET /api/messages/sync with last sync timestamp.
     */
    fun performInitialSync() {
        scope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true

            val token = tokenManager.accessToken.firstOrNull()
            if (!token.isNullOrBlank()) {
                chatRepository.connectWebSocket(token)
            }

            val sinceIso = lastSyncTimestamp?.toString()
            chatRepository.syncMessages(sinceIso)
                .onSuccess { messages ->
                    Log.d(tag, "Initial sync retrieved ${messages.size} missed messages")
                    lastSyncTimestamp = Instant.now()
                    _syncedMessages.emit(messages)
                }
                .onFailure { error ->
                    Log.e(tag, "Initial sync failed", error)
                }

            _isSyncing.value = false
        }
    }

    /**
     * Flushes pending message queue (retries sending unsent messages created while offline)
     */
    private suspend fun flushPendingQueue() {
        val items = pendingQueue.getPendingItems()
        for (item in items) {
            if (item.status == MessageUiStatus.SENDING || item.status == MessageUiStatus.FAILED) {
                Log.d(tag, "Flushing pending message clientMsgId=${item.clientMsgId}")
                chatRepository.sendMessageWithUuid(
                    conversationId = item.request.conversationId,
                    senderId = item.request.senderId,
                    content = item.request.content,
                    clientMsgId = item.clientMsgId
                ).onSuccess { sentMsg ->
                    pendingQueue.markStatus(item.clientMsgId, MessageUiStatus.SENT)
                    pendingQueue.remove(item.clientMsgId)
                }.onFailure {
                    pendingQueue.markStatus(item.clientMsgId, MessageUiStatus.FAILED)
                }
            }
        }
    }
}
