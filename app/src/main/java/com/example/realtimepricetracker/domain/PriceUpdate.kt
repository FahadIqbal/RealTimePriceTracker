package com.example.realtimepricetracker.domain

import kotlinx.serialization.Serializable

@Serializable
 data class PriceUpdate(
     val symbol: String,
     val price: Double,
     val timestamp: Long
 )
