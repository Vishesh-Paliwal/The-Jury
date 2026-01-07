package com.example.the_jury.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFFBB86FC)
private val Secondary = Color(0xFF03DAC6)
private val Tertiary = Color(0xFF3700B3)
private val Background = Color(0xFF121212)
private val Surface = Color(0xFF1E1E1E)
private val OnPrimary = Color.Black
private val OnSecondary = Color.Black
private val OnBackground = Color.White
private val OnSurface = Color.White

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
