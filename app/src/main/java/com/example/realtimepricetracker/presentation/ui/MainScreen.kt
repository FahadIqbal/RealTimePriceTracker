package com.example.realtimepricetracker.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.realtimepricetracker.data.WebSocketRepository
import com.example.realtimepricetracker.domain.ConnectionState
import com.example.realtimepricetracker.presentation.StockPriceViewModel

@Composable
fun MainScreen(
    viewModel: StockPriceViewModel = viewModel(factory = stockPriceViewModelFactory())
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val err = uiState.error
        if (err != null) {
            val result = snackbarHostState.showSnackbar(
                message = err,
                actionLabel = "Dismiss"
            )
            viewModel.onEvent(StockPriceViewModel.UiEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                connectionState = uiState.connectionState,
                isPriceUpdateActive = uiState.isPriceUpdateActive,
                onToggleClick = { viewModel.onEvent(StockPriceViewModel.UiEvent.TogglePriceUpdates) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        if (uiState.stocks.isEmpty()) {
            when (uiState.connectionState) {
                is ConnectionState.Connecting -> {
                    Box(contentModifier, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Box(contentModifier, contentAlignment = Alignment.Center) {
                        Text("No stocks to display", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.stocks, key = { _, item -> item.symbol }) { index, stock ->
                    StockItemRow(stockSymbol = stock, modifier = Modifier.padding(horizontal = 12.dp))
                    if (index < uiState.stocks.lastIndex) {
                        Divider(modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

private fun stockPriceViewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockPriceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StockPriceViewModel(WebSocketRepository()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
