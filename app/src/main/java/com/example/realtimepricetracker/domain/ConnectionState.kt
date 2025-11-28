package com.example.realtimepricetracker.domain

 sealed class ConnectionState {
     data object Connected : ConnectionState()
     data object Disconnected : ConnectionState()
     data object Connecting : ConnectionState()
 }
