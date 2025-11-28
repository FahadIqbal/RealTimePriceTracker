package com.example.realtimepricetracker.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.realtimepricetracker.domain.ConnectionState
import org.junit.Rule
import org.junit.Test

class TopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shows_correct_status_emoji_and_title() {
        composeRule.setContent {
            TopBar(connectionState = ConnectionState.Connected, isPriceUpdateActive = false, onToggleClick = {})
        }
        composeRule.onNodeWithText("🟢").assertIsDisplayed()
        composeRule.onNodeWithText("Real-Time Price Tracker").assertIsDisplayed()
    }

    @Test
    fun toggle_button_label_reflects_state() {
        composeRule.setContent {
            TopBar(connectionState = ConnectionState.Disconnected, isPriceUpdateActive = false, onToggleClick = {})
        }
        composeRule.onNodeWithText("Start").assertIsDisplayed()

        composeRule.setContent {
            TopBar(connectionState = ConnectionState.Disconnected, isPriceUpdateActive = true, onToggleClick = {})
        }
        composeRule.onNodeWithText("Stop").assertIsDisplayed()
    }
}
