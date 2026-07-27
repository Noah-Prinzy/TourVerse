package com.tourverse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TourismColors = lightColorScheme(
    primary = Color(0xFF1F6A4A),
    onPrimary = Color.White,
    secondary = Color(0xFFF4C44E),
    background = Color(0xFFF3F6F4),
    surface = Color.White,
    onSurface = Color(0xFF15231D)
)

@Composable
fun TourismTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TourismColors,
        content = content
    )
}
