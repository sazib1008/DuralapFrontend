package com.example.duralab.data.remote

import android.util.Log
import com.example.duralab.util.TokenManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StompClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun connect(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("StompClient", "WebSocket Opened")
                // Send CONNECT frame
                val token = tokenManager.getToken() ?: ""
                val connectFrame = """
                    CONNECT
                    accept-version:1.1,1.0
                    heart-beat:10000,10000
                    Authorization:Bearer $token
                    
                    ${'\u0000'}
                """.trimIndent()
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("StompClient", "Message Received: ${text.take(100)}")
                _messages.tryEmit(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("StompClient", "WebSocket Closing: $reason")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("StompClient", "WebSocket Error", t)
            }
        })
    }

    fun subscribe(destination: String, id: String) {
        val subscribeFrame = """
            SUBSCRIBE
            id:$id
            destination:$destination
            
            ${'\u0000'}
        """.trimIndent()
        webSocket?.send(subscribeFrame)
    }

    fun send(destination: String, body: String) {
        val sendFrame = """
            SEND
            destination:$destination
            content-length:${body.length}
            
            $body${'\u0000'}
        """.trimIndent()
        webSocket?.send(sendFrame)
    }

    fun disconnect() {
        val disconnectFrame = """
            DISCONNECT
            
            ${'\u0000'}
        """.trimIndent()
        webSocket?.send(disconnectFrame)
        webSocket?.close(1000, "App Disconnected")
        webSocket = null
    }
}
