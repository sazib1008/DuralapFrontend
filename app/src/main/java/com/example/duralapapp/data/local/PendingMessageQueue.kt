package com.example.duralapapp.data.local

import com.example.duralapapp.data.model.MessageCreateRequest
import com.example.duralapapp.data.model.MessageUiStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class PendingMessageItem(
    val clientMsgId: String,
    val request: MessageCreateRequest,
    var status: MessageUiStatus = MessageUiStatus.SENDING,
    var retryCount: Int = 0
)

@Singleton
class PendingMessageQueue @Inject constructor() {

    private val queue = ConcurrentHashMap<String, PendingMessageItem>()
    private val _pendingState = MutableStateFlow<List<PendingMessageItem>>(emptyList())
    val pendingState: StateFlow<List<PendingMessageItem>> = _pendingState.asStateFlow()

    fun enqueue(item: PendingMessageItem) {
        queue[item.clientMsgId] = item
        updateState()
    }

    fun markStatus(clientMsgId: String, status: MessageUiStatus) {
        queue[clientMsgId]?.let {
            it.status = status
            if (status == MessageUiStatus.FAILED) {
                it.retryCount++
            }
            updateState()
        }
    }

    fun remove(clientMsgId: String) {
        queue.remove(clientMsgId)
        updateState()
    }

    fun getPendingItems(): List<PendingMessageItem> {
        return queue.values.toList()
    }

    private fun updateState() {
        _pendingState.value = queue.values.toList()
    }
}
