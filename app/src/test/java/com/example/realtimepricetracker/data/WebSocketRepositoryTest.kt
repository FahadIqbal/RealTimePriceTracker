package com.example.realtimepricetracker.data

import app.cash.turbine.test
import com.example.realtimepricetracker.domain.ConnectionState
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketRepositoryTest {

    @MockK(relaxed = true)
    lateinit var client: OkHttpClient

    private lateinit var repository: WebSocketRepository

    private lateinit var listenerSlot: CapturingSlot<WebSocketListener>
    private lateinit var mockSocket: WebSocket

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        listenerSlot = slot()
        mockSocket = mockk(relaxed = true)

        every { client.newWebSocket(any<Request>(), capture(listenerSlot)) } answers {
            mockSocket
        }

        repository = WebSocketRepository(client = client, url = "wss://example.com/test")
    }

    @Test
    fun connection_state_emits_on_open_and_close() = runTest {
        repository.connect() // start connect
        val listener = listenerSlot.captured
        val response = mockk<Response>(relaxed = true)

        repository.connectionState.test {
            // initial Disconnected, then Connecting when ensureConnected invoked
            awaitItem() // Disconnected
            // simulate open
            listener.onOpen(mockSocket, response)
            assertEquals(ConnectionState.Connected, awaitItem())
            // simulate close
            listener.onClosed(mockSocket, 1000, "bye")
            assertEquals(ConnectionState.Disconnected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun messages_flow_emits_on_message() = runTest {
        repository.connect()
        val listener = listenerSlot.captured

        repository.observeMessages().test {
            val msg = "hello"
            listener.onMessage(mockSocket, msg)
            assertEquals(msg, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendMessage_uses_socket() = runTest {
        repository.connect()
        val listener = listenerSlot.captured
        val response = mockk<Response>(relaxed = true)
        listener.onOpen(mockSocket, response)
        every { mockSocket.send("ping") } returns true
        val ok = repository.sendMessage("ping")
        assertEquals(true, ok)
    }
}
