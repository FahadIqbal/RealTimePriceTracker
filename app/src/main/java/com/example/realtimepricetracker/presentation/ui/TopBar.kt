package com.example.realtimepricetracker.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.realtimepricetracker.domain.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Top app bar for the Real-Time Price Tracker.
 *
 * Shows a connection status indicator, title, and a Start/Stop action button.
 *
 * @param connectionState current WebSocket connection state
 * @param isPriceUpdateActive whether periodic price generation is active
 * @param onToggleClick invoked when the Start/Stop button is pressed
 * @param modifier optional [Modifier]
 */
fun TopBar(
    connectionState: ConnectionState,
    isPriceUpdateActive: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface
    )

    val statusEmoji = when (connectionState) {
        is ConnectionState.Connected -> "🟢"
        is ConnectionState.Disconnected -> "🔴"
        is ConnectionState.Connecting -> "🟡"
    }

    val buttonText = if (isPriceUpdateActive) "Stop" else "Start"
    val targetColor = if (isPriceUpdateActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val targetOnColor = if (isPriceUpdateActive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val buttonContainerColor by animateColorAsState(targetColor, label = "btnContainer")
    val buttonContentColor by animateColorAsState(targetOnColor, label = "btnContent")

    TopAppBar(
        modifier = modifier,
        colors = appBarColors,
        navigationIcon = {
            Text(text = statusEmoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
        },
        title = {
            Text(
                text = "Real-Time Price Tracker",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            Button(
                onClick = onToggleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(buttonText)
            }
        }
    )
}
