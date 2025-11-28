package com.example.realtimepricetracker.presentation.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.realtimepricetracker.domain.StockSymbol
import org.junit.Rule
import org.junit.Test

class StockItemRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shows_up_arrow_when_increasing() {
        val stock = StockSymbol(symbol = "AAPL", currentPrice = 110.0, previousPrice = 100.0, lastUpdated = 0L)
        composeRule.setContent {
            StockItemRow(stockSymbol = stock)
        }
        composeRule.onNodeWithText("↑").assertIsDisplayed()
    }

    @Test
    fun shows_down_arrow_when_decreasing() {
        val stock = StockSymbol(symbol = "AAPL", currentPrice = 90.0, previousPrice = 100.0, lastUpdated = 0L)
        composeRule.setContent {
            StockItemRow(stockSymbol = stock)
        }
        composeRule.onNodeWithText("↓").assertIsDisplayed()
    }

    @Test
    fun displays_symbol_and_price() {
        val stock = StockSymbol(symbol = "AAPL", currentPrice = 150.25, previousPrice = 150.25, lastUpdated = 0L)
        composeRule.setContent {
            StockItemRow(stockSymbol = stock)
        }
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("$150.25").assertIsDisplayed()
    }
}
