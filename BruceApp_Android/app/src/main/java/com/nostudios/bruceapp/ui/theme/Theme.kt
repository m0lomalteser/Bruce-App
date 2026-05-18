package com.nostudios.bruceapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BruceColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Background,
    secondary = WhiteOp70,
    onSecondary = Background,
    background = Background,
    onBackground = White,
    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = CardDark,
    onSurfaceVariant = Gray,
    error = Red,
    onError = White
)

@Composable
fun BruceAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BruceColorScheme,
        typography = BruceTypography,
        content = content
    )
}
