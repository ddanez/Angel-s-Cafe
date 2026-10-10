package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CafeBrown,
    onPrimary = EspressoBlack,
    primaryContainer = CafeDarkBrown,
    onPrimaryContainer = SmoothBeige,
    secondary = GoldenCrema,
    onSecondary = EspressoBlack,
    background = EspressoBlack,
    onBackground = LightText,
    surface = CocoaCard,
    onSurface = LightText,
    surfaceVariant = Color(0xFF2D2522),
    onSurfaceVariant = SmoothBeige,
    error = SoftRed,
    onError = LightText
)

private val LightColorScheme = lightColorScheme(
    primary = CafeDarkBrown,
    onPrimary = LightText,
    primaryContainer = CafeBrown.copy(alpha = 0.25f),
    onPrimaryContainer = DarkText,
    secondary = GoldenCrema,
    onSecondary = DarkText,
    background = SmoothBeige,
    onBackground = DarkText,
    surface = Color(0xFFFAF7F5),
    onSurface = DarkText,
    surfaceVariant = Color(0xFFE4DAD4),
    onSurfaceVariant = DarkText,
    error = SoftRed,
    onError = LightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
