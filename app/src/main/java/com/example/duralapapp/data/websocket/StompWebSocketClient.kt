package com.example.duralapapp.data.websocket

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class StompMessage(
    val command: String,
    val headers: Map<String, String>,
    val payload: String
)

@Singleton
class StompWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val tag = "StompWebSocketClient"
    private var webSocket: WebSocket? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<StompMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<StompMessage> = _messages.asSharedFlow()

    private val activeSubscriptions = ConcurrentHashMap<String, String>() // destination -> subId
    private val subIdCounter = AtomicInteger(0)

    private var lastWsUrl: String = "ws://192.168.0.179:8080/websocket"
    private var lastAuthToken: String? = null
    private var isManualDisconnect = false
    private var reconnectJob: Job? = null

    fun connect(wsUrl: String = "ws://192.168.0.179:8080/websocket", authToken: String? = null) {
        if (webSocket != null) return

        isManualDisconnect = false
        lastWsUrl = wsUrl
        lastAuthToken = authToken
        reconnectJob?.cancel()

        val requestBuilder = Request.Builder().url(wsUrl)
        if (!authToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }
        val request = requestBuilder.build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened. Sending STOMP CONNECT...")
                val connectFrame = StringBuilder()
                    .append("CONNECT\n")
                    .append("accept-version:1.1,1.2\n")
                    .append("host:192.168.0.179\n")
                    .append("heart-beat:15000,15000\n")
                if (!authToken.isNullOrBlank()) {
                    val token = if (authToken.startsWith("Bearer ")) authToken else "Bearer $authToken"
                    connectFrame.append("Authorization:").append(token).append("\n")
                }
                connectFrame.append("\n\u0000")

                webSocket.send(connectFrame.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseStompFrame(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket Closing: $reason")
                stopHeartbeat()
                _connectionState.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket Closed: $reason")
                stopHeartbeat()
                _connectionState.value = false
                this@StompWebSocketClient.webSocket = null
                if (!isManualDisconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket Failure", t)
                stopHeartbeat()
                _connectionState.value = false
                this@StompWebSocketClient.webSocket = null
                if (!isManualDisconnect) {
                    scheduleReconnect()
                }
            }
        }

        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(2000)
            if (!isManualDisconnect && webSocket == null) {
                Log.i(tag, "Attempting STOMP reconnect...")
                connect(lastWsUrl, lastAuthToken)
            }
        }
    }

    fun subscribe(destination: String): String {
        val existingSub = activeSubscriptions[destination]
        if (existingSub != null) return existingSub

        val subId = "sub-${subIdCounter.getAndIncrement()}"
        activeSubscriptions[destination] = subId

        if (_connectionState.value && webSocket != null) {
            sendSubscribeFrame(subId, destination)
        }
        return subId
    }

    fun unsubscribe(destination: String) {
        val subId = activeSubscriptions.remove(destination) ?: return
        if (_connectionState.value && webSocket != null) {
            val frame = "UNSUBSCRIBE\nid:$subId\n\n\u0000"
            webSocket?.send(frame)
        }
    }

    fun send(destination: String, jsonBody: String) {
        val frame = "SEND\ndestination:$destination\ncontent-type:application/json\n\n$jsonBody\u0000"
        webSocket?.send(frame)
    }

    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        stopHeartbeat()
        webSocket?.close(1000, "Disconnect requested")
        webSocket = null
        _connectionState.value = false
        activeSubscriptions.clear()
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value && webSocket != null) {
                delay(15_000)
                try {
                    val heartbeatPayload = "{\"clientType\":\"ANDROID\"}"
                    send("/app/presence.heartbeat", heartbeatPayload)
                    Log.d(tag, "Sent STOMP presence heartbeat to /app/presence.heartbeat")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to send presence heartbeat", e)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendSubscribeFrame(subId: String, destination: String) {
        val frame = "SUBSCRIBE\nid:$subId\ndestination:$destination\n\n\u0000"
        webSocket?.send(frame)
        Log.d(tag, "Subscribed to $destination with id $subId")
    }

    private fun parseStompFrame(raw: String) {
        val lines = raw.split("\n")
        if (lines.isEmpty()) return

        val command = lines[0].trim()
        if (command.isEmpty()) return // Heartbeat ping frame

        if (command == "CONNECTED") {
            Log.d(tag, "STOMP Handshake CONNECTED")
            _connectionState.value = true
            startHeartbeat()
            // Re-subscribe any pending active subscriptions
            activeSubscriptions.forEach { (dest, subId) ->
                sendSubscribeFrame(subId, dest)
            }
            return
        }

        val headers = mutableMapOf<String, String>()
        var bodyStartIndex = 1

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.trim().isEmpty()) {
                bodyStartIndex = i + 1
                break
            }
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                headers[parts[0].trim()] = parts[1].trim()
            }
        }

        val payload = lines.drop(bodyStartIndex).joinToString("\n").replace("\u0000", "")

        val stompMessage = StompMessage(command, headers, payload)
        android.util.Log.d("DEBUG_STOMP", "[1] WebSocket frame received | Command: $command | Destination: ${headers["destination"]} | Payload: $payload")
        scope.launch {
            _messages.emit(stompMessage)
        }
    }
}
