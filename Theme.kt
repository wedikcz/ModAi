package com.automod.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF00C853),
    tertiary = Color(0xFFD500F9),
    background = Color(0xFF0D0D0D),
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF2D2D2D),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00B8D4),
    secondary = Color(0xFF00A86B),
    tertiary = Color(0xFFAA00FF),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E8E8),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    error = Color(0xFFB00020)
)

@Composable
fun AutoModTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
