package com.hybridtempo.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TempoOrange = Color(0xFFEB470A)
private val Carbon = Color(0xFF0D0D0D)
private val SurfaceBlack = Color(0xFF202020)
private val White = Color(0xFFFFFFFF)
private val Muted = Color(0xFF999999)

private val DarkColors = darkColorScheme(
    primary = TempoOrange,
    onPrimary = White,
    secondary = TempoOrange,
    background = Carbon,
    onBackground = White,
    surface = SurfaceBlack,
    onSurface = White,
    surfaceVariant = Color(0xFF2F2F2F),
    onSurfaceVariant = Muted,
    primaryContainer = Color(0xFF3A160C),
    onPrimaryContainer = White,
    secondaryContainer = Color(0xFF202020),
    onSecondaryContainer = White,
)

private val LightColors = lightColorScheme(
    primary = TempoOrange,
    onPrimary = White,
    secondary = TempoOrange,
    background = Carbon,
    onBackground = White,
    surface = SurfaceBlack,
    onSurface = White,
    surfaceVariant = Color(0xFF2F2F2F),
    onSurfaceVariant = Muted,
    primaryContainer = Color(0xFF3A160C),
    onPrimaryContainer = White,
    secondaryContainer = Color(0xFF202020),
    onSecondaryContainer = White,
)

@Composable
fun HybridTempoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
