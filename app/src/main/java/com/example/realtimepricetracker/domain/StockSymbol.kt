package com.example.realtimepricetracker.domain

 data class StockSymbol(
     val symbol: String,
     val currentPrice: Double,
     val previousPrice: Double,
     val lastUpdated: Long
 ) {
     val priceChange: Double
         get() = currentPrice - previousPrice

     val isIncreasing: Boolean
         get() = priceChange > 0
 }
