package com.example.realtimepricetracker.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.realtimepricetracker.data.WebSocketRepository
import com.example.realtimepricetracker.domain.ConnectionState
import com.example.realtimepricetracker.domain.PriceUpdate
import com.example.realtimepricetracker.presentation.StockPriceViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun shows_list_and_toggles_and_connection_status() = runTest {
        val connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val repo = mockk<WebSocketRepository>(relaxed = true)
        every { repo.connect() } returns connection
        every { repo.observeMessages() } returns messages
        coEvery { repo.sendMessage(any()) } returns true
        val vm = StockPriceViewModel(repo)

        rule.setContent {
            MainScreen(viewModel = vm)
        }

        // Initially Disconnected, button should show Start
        rule.onNodeWithText("🔴").assertIsDisplayed()
        rule.onNodeWithText("Start").assertIsDisplayed()

        // Toggle to start -> label should change to Stop
        rule.onNodeWithText("Start").performClick()
        rule.onNodeWithText("Stop").assertIsDisplayed()

        // Set connected state -> green dot
        connection.value = ConnectionState.Connecting
        connection.value = ConnectionState.Connected
        rule.onNodeWithText("🟢").assertIsDisplayed()

        // Some known symbols should be present in the list
        rule.onNodeWithText("AAPL").assertIsDisplayed()
        rule.onNodeWithText("GOOG").assertIsDisplayed()
        rule.onNodeWithText("TSLA").assertIsDisplayed()
    }

    @Test
    fun shows_snackbar_on_error_and_dismisses() = runTest {
        val connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
        val repo = mockk<WebSocketRepository>(relaxed = true)
        every { repo.connect() } returns connection
        every { repo.observeMessages() } returns messages
        val vm = StockPriceViewModel(repo)

        rule.setContent { MainScreen(viewModel = vm) }

        // Emit invalid json to trigger error path
        messages.tryEmit("not json")

        // Snackbar should appear with some message
        rule.onNodeWithText("Dismiss").assertIsDisplayed()
        rule.onNodeWithText("Dismiss").performClick()
    }
}
