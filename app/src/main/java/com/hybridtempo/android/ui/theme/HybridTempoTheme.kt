package com.hybridtempo.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RecoveryGreen = Color(0xFFD9F26B)
private val Carbon = Color(0xFF090B0B)
private val Slate = Color(0xFF151A18)
private val Bone = Color(0xFFF4F0E8)
private val Clay = Color(0xFFB77B55)

private val DarkColors = darkColorScheme(
    primary = RecoveryGreen,
    onPrimary = Carbon,
    secondary = Clay,
    background = Carbon,
    onBackground = Bone,
    surface = Slate,
    onSurface = Bone,
    surfaceVariant = Color(0xFF202723),
    onSurfaceVariant = Color(0xFFCBD4C8),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF526600),
    onPrimary = Color.White,
    secondary = Color(0xFF7B4F34),
    background = Bone,
    onBackground = Carbon,
    surface = Color.White,
    onSurface = Carbon,
    surfaceVariant = Color(0xFFE1E7D8),
    onSurfaceVariant = Color(0xFF40483F),
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
