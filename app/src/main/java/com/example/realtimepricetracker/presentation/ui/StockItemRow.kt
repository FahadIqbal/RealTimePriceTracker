package com.example.realtimepricetracker.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.realtimepricetracker.domain.StockSymbol
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StockItemRow(
    stockSymbol: StockSymbol,
    modifier: Modifier = Modifier
) {
    val isUp by remember(stockSymbol.isIncreasing) { derivedStateOf { stockSymbol.isIncreasing } }
    val changeColorTarget by remember(isUp) { derivedStateOf { if (isUp) Color(0xFF4CAF50) else Color(0xFFF44336) } }
    val chipColor by animateColorAsState(changeColorTarget, label = "chipColor")

    val flashTarget = remember { mutableStateOf(Color.Transparent) }
    val flashBg by animateColorAsState(
        targetValue = flashTarget.value,
        animationSpec = tween(durationMillis = 250),
        label = "flashBg"
    )

    LaunchedEffect(stockSymbol.currentPrice) {
        if (stockSymbol.priceChange != 0.0) {
            flashTarget.value = changeColorTarget.copy(alpha = 0.15f)
            kotlinx.coroutines.delay(1000)
            flashTarget.value = Color.Transparent
        }
    }

    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    val priceText = currency.format(stockSymbol.currentPrice)
    val pct = if (stockSymbol.previousPrice != 0.0) ((stockSymbol.priceChange / stockSymbol.previousPrice) * 100.0) else 0.0
    val pctText = String.format(Locale.US, "(%.2f%%)", pct)
    val arrow = if (isUp) "↑" else "↓"

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(flashBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stockSymbol.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = priceText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    AnimatedVisibility(visible = stockSymbol.priceChange != 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(chipColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = arrow,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = pctText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = chipColor
                            )
                        }
                    }
                }
            }
        }
    }
}
