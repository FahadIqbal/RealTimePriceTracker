package com.example.realtimepricetracker.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimepricetracker.data.WebSocketRepository
import com.example.realtimepricetracker.domain.ConnectionState
import com.example.realtimepricetracker.domain.PriceUpdate
import com.example.realtimepricetracker.domain.StockSymbol
import com.example.realtimepricetracker.domain.StockSymbols
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * ViewModel implementing MVI for real-time stock prices.
 *
 * Exposes immutable [UiState] via [uiState] and processes [UiEvent] via [onEvent].
 */
class StockPriceViewModel(
    private val repository: WebSocketRepository
) : ViewModel() {

    /** UI state of the stock price screen. */
    data class UiState(
        val stocks: List<StockSymbol> = emptyList(),
        val connectionState: ConnectionState = ConnectionState.Disconnected,
        val isPriceUpdateActive: Boolean = false,
        val error: String? = null
    )

    /** UI events that drive state changes and side effects. */
    sealed class UiEvent {
        data object StartPriceUpdates : UiEvent()
        data object StopPriceUpdates : UiEvent()
        data object TogglePriceUpdates : UiEvent()
        data object ClearError : UiEvent()
    }

    private val _uiState = MutableStateFlow(UiState())
    /** Public immutable stream of [UiState]. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var priceJob: Job? = null
    private val collectorsStarted = AtomicBoolean(false)

    private val json = Json { ignoreUnknownKeys = true }

    init {
        val now = System.currentTimeMillis()
        val initial = StockSymbols.DEFAULT_SYMBOLS.map { symbol ->
            val price = Random.nextDouble(50.0, 500.0)
            StockSymbol(
                symbol = symbol,
                currentPrice = price,
                previousPrice = price,
                lastUpdated = now
            )
        }.sortedByDescending { it.currentPrice }
        _uiState.value = _uiState.value.copy(stocks = initial)
    }

    /** Handle a [UiEvent] to mutate state or trigger side-effects. */
    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.StartPriceUpdates -> startPriceUpdates()
            is UiEvent.StopPriceUpdates -> stopPriceUpdates()
            is UiEvent.TogglePriceUpdates -> if (_uiState.value.isPriceUpdateActive) stopPriceUpdates() else startPriceUpdates()
            is UiEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    fun startPriceUpdates() {
        if (_uiState.value.isPriceUpdateActive) return
        if (collectorsStarted.compareAndSet(false, true)) {
            viewModelScope.launch {
                repository.connect().collectLatest { state ->
                    _uiState.update { it.copy(connectionState = state) }
                }
            }
            viewModelScope.launch {
                repository.observeMessages().collect { msg ->
                    try {
                        val update = json.decodeFromString<PriceUpdate>(msg)
                        applyPriceUpdate(update)
                    } catch (t: Throwable) {
                        _uiState.update { it.copy(error = t.message) }
                    }
                }
            }
        }
        priceJob?.cancel()
        priceJob = viewModelScope.launch {
            _uiState.update { it.copy(isPriceUpdateActive = true) }
            while (true) {
                val now = System.currentTimeMillis()
                val currentStocks = _uiState.value.stocks
                for (stock in currentStocks) {
                    val deltaPct = Random.nextDouble(0.005, 0.03) * if (Random.nextBoolean()) 1 else -1
                    val newPrice = (stock.currentPrice * (1.0 + deltaPct)).coerceAtLeast(0.01)
                    val update = PriceUpdate(stock.symbol, newPrice, now)
                    try {
                        repository.sendMessage(json.encodeToString(update))
                    } catch (t: Throwable) {
                        _uiState.update { it.copy(error = t.message) }
                    }
                }
                delay(2000)
            }
        }
    }

    private fun stopPriceUpdates() {
        priceJob?.cancel()
        priceJob = null
        _uiState.update { it.copy(isPriceUpdateActive = false) }
        viewModelScope.launch {
            try {
                repository.disconnect()
            } catch (_: Throwable) {
            }
        }
    }

    private fun applyPriceUpdate(update: PriceUpdate) {
        val updated = _uiState.value.stocks.map { stock ->
            if (stock.symbol == update.symbol) {
                stock.copy(
                    previousPrice = stock.currentPrice,
                    currentPrice = update.price,
                    lastUpdated = update.timestamp
                )
            } else stock
        }.sortedByDescending { it.currentPrice }
        _uiState.update { it.copy(stocks = updated) }
    }

    override fun onCleared() {
        super.onCleared()
        stopPriceUpdates()
    }
}
