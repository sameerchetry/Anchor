package com.sameer.studymode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF36D6C3),
    onPrimary = Color(0xFF102522),
    primaryContainer = Color(0xFF174D48),
    onPrimaryContainer = Color(0xFF9AF5E9),

    secondary = Color(0xFF9CBDB7),
    onSecondary = Color(0xFF202327),

    background = Color(0xFF202327),
    onBackground = Color(0xFFF1F3F5),

    surface = Color(0xFF292E34),
    onSurface = Color(0xFFF1F3F5),

    surfaceVariant = Color(0xFF343A42),
    onSurfaceVariant = Color(0xFFB8C0CA),

    outline = Color(0xFF454C55),
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