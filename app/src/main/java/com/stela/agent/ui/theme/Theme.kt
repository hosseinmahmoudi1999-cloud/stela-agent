package com.stela.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0B6EFD),
    secondary = androidx.compose.ui.graphics.Color(0xFF00BFA5),
    tertiary = androidx.compose.ui.graphics.Color(0xFF5C6BC0)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF81B4FF),
    secondary = androidx.compose.ui.graphics.Color(0xFF50E3C2),
    tertiary = androidx.compose.ui.graphics.Color(0xFFB39DDB)
)

@Composable
fun StelaTheme(content: @Composable () -> Unit) {
    val useDark = isSystemInDarkTheme()
    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
