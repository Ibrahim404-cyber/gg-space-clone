package com.ibrahim404.ggspace.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val NeonPurple = Color(0xFF7C5CFF)
val NeonCyan = Color(0xFF00E5FF)
val NeonPink = Color(0xFFFF4D9D)
val AppBackground = Color(0xFF090910)
val CardSurface = Color(0xFF14141F)
val CardSurfaceAlt = Color(0xFF1C1C2B)
val TextMuted = Color(0xFF8E8EA6)

private val DarkColors = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = NeonPink,
    background = AppBackground,
    onBackground = Color(0xFFF2F2FA),
    surface = CardSurface,
    onSurface = Color(0xFFF2F2FA),
    surfaceVariant = CardSurfaceAlt,
    onSurfaceVariant = Color(0xFFA9A9BE),
    error = Color(0xFFFF5A6E)
)

@Composable
fun GGSpaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp)
        ),
        content = content
    )
}
