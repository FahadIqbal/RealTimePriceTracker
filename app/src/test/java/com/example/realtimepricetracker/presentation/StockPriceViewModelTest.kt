package com.example.realtimepricetracker.presentation

import app.cash.turbine.test
import com.example.realtimepricetracker.data.WebSocketRepository
import com.example.realtimepricetracker.domain.ConnectionState
import com.example.realtimepricetracker.domain.PriceUpdate
import com.example.realtimepricetracker.domain.StockSymbols
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockPriceViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: WebSocketRepository
    private lateinit var viewModel: StockPriceViewModel

    private val connectionFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val messagesFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.connect() } returns connectionFlow
        every { repository.observeMessages() } returns messagesFlow
        coEvery { repository.sendMessage(any()) } returns true
        viewModel = StockPriceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_has_25_symbols_sorted_desc() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals(25, state.stocks.size)
        val sorted = state.stocks.sortedByDescending { it.currentPrice }
        assertEquals(sorted.map { it.symbol }, state.stocks.map { it.symbol })
        assertEquals(ConnectionState.Disconnected, state.connectionState)
        assertFalse(state.isPriceUpdateActive)
        assertEquals(null, state.error)
    }

    @Test
    fun start_event_sets_active_and_connects() = runTest(testDispatcher) {
        viewModel.onEvent(StockPriceViewModel.UiEvent.StartPriceUpdates)
        // Connection state flow is collected; simulate connected
        connectionFlow.value = ConnectionState.Connecting
        connectionFlow.value = ConnectionState.Connected
        assertTrue(viewModel.uiState.value.isPriceUpdateActive)
        assertEquals(ConnectionState.Connected, viewModel.uiState.value.connectionState)
    }

    @Test
    fun stop_event_cancels_updates_and_disconnects() = runTest(testDispatcher) {
        viewModel.onEvent(StockPriceViewModel.UiEvent.StartPriceUpdates)
        viewModel.onEvent(StockPriceViewModel.UiEvent.StopPriceUpdates)
        assertFalse(viewModel.uiState.value.isPriceUpdateActive)
        coVerify { repository.disconnect() }
    }

    @Test
    fun applies_price_updates_and_keeps_sorted() = runTest(testDispatcher) {
        viewModel.onEvent(StockPriceViewModel.UiEvent.StartPriceUpdates)
        connectionFlow.value = ConnectionState.Connected
        val first = viewModel.uiState.value.stocks.first()
        val higher = first.currentPrice + 10.0
        val update = PriceUpdate(symbol = first.symbol, price = higher, timestamp = System.currentTimeMillis())
        messagesFlow.emit(kotlinx.serialization.json.Json.encodeToString(PriceUpdate.serializer(), update))
        val updated = viewModel.uiState.value.stocks.first()
        assertEquals(first.symbol, updated.symbol)
        assertEquals(higher, updated.currentPrice, 0.0001)
        val sorted = viewModel.uiState.value.stocks.sortedByDescending { it.currentPrice }
        assertEquals(sorted.map { it.symbol }, viewModel.uiState.value.stocks.map { it.symbol })
    }

    @Test
    fun error_is_set_and_can_be_cleared() = runTest(testDispatcher) {
        val errorMsg = "boom"
        // Emit invalid JSON to trigger error
        messagesFlow.emit("not json")
        // Give chance to collect
        assertTrue(viewModel.uiState.value.error != null)
        viewModel.onEvent(StockPriceViewModel.UiEvent.ClearError)
        assertEquals(null, viewModel.uiState.value.error)
    }
}
