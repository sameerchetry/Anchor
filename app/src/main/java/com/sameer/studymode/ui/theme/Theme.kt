package com.sameer.studymode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0C3),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF75F8E8),

    secondary = Color(0xFFB0CCC7),
    onSecondary = Color(0xFF1C3531),

    background = Color(0xFF101414),
    onBackground = Color(0xFFE0E7E5),

    surface = Color(0xFF171D1C),
    onSurface = Color(0xFFE0E7E5),

    surfaceVariant = Color(0xFF273331),
    onSurfaceVariant = Color(0xFFB8C8C4),

    outline = Color(0xFF52615D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006B62),
    onPrimary = Color.White,
    background = Color(0xFFF5F8F7),
    onBackground = Color(0xFF171D1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1C),
    surfaceVariant = Color(0xFFE0E8E5),
    onSurfaceVariant = Color(0xFF414D49)
)

@Composable
fun StudyModeTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Use the app's fixed palette rather than wallpaper-derived colors.
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}