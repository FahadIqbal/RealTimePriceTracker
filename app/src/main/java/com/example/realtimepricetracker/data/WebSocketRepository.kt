package com.example.realtimepricetracker.data

import android.util.Log
import com.example.realtimepricetracker.domain.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Repository that manages a WebSocket connection using OkHttp.
 *
 * Exposes a [StateFlow] for connection state and a [SharedFlow] for incoming messages.
 * Thread-safe via a [Mutex] and scoped with [SupervisorJob] on [Dispatchers.IO].
 */
class WebSocketRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
    private val url: String = "wss://ws.postman-echo.com/raw"
) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val mutex = Mutex()
    private var webSocket: WebSocket? = null
    private var connectJob: Job? = null
    private var retryAttempt: Int = 0
    private var isManuallyClosed: Boolean = false

    /**
     * Connect (or ensure connection) to the WebSocket and return a [Flow] of [ConnectionState].
     */
    fun connect(): Flow<ConnectionState> {
        scope.launch { ensureConnected() }
        return connectionState
    }

    /**
     * Observe incoming text messages from the WebSocket as a cold [Flow].
     */
    fun observeMessages(): Flow<String> = messages

    /**
     * Send a text [message] through the WebSocket.
     * @return true if the message was enqueued successfully.
     */
    suspend fun sendMessage(message: String): Boolean {
        return mutex.withLock {
            webSocket?.send(message) == true
        }
    }

    /**
     * Gracefully close the WebSocket and stop reconnection attempts.
     */
    suspend fun disconnect() {
        mutex.withLock {
            isManuallyClosed = true
            retryAttempt = 0
            webSocket?.close(1000, null)
            webSocket = null
            _connectionState.value = ConnectionState.Disconnected
        }
        connectJob?.cancel()
    }

    private suspend fun ensureConnected() {
        mutex.withLock {
            if (_connectionState.value == ConnectionState.Connected || _connectionState.value == ConnectionState.Connecting) return
            isManuallyClosed = false
            _connectionState.value = ConnectionState.Connecting
            startSocket()
        }
    }

    private fun startSocket() {
        connectJob?.cancel()
        connectJob = scope.launch {
            val request = Request.Builder().url(url).build()
            val listener = createListener()
            mutex.withLock {
                webSocket = client.newWebSocket(request, listener)
            }
        }
    }

    private fun scheduleReconnect() {
        if (isManuallyClosed) return
        val delayMs = (1000L * (1 shl retryAttempt).coerceAtMost(32))
        scope.launch {
            delay(delayMs)
            if (!isManuallyClosed && scope.isActive) {
                _connectionState.value = ConnectionState.Connecting
                startSocket()
            }
        }
        if (retryAttempt < 6) retryAttempt++
    }

    private fun resetBackoff() {
        retryAttempt = 0
    }

    private fun createListener(): WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            resetBackoff()
            _connectionState.value = ConnectionState.Connected
            Log.d(TAG, "WebSocket connected: ${'$'}response")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            _messages.tryEmit(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "WebSocket closing: ${'$'}code ${'$'}reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "WebSocket closed: ${'$'}code ${'$'}reason")
            if (!isManuallyClosed) scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = ConnectionState.Disconnected
            Log.e(TAG, "WebSocket failure", t)
            if (!isManuallyClosed) scheduleReconnect()
        }
    }

    companion object {
        private const val TAG = "WebSocketRepo"
    }
}
