package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = QuantumCyan,
    secondary = LaserBlue,
    tertiary = BioGreen,
    background = MidnightBlack,
    surface = SolarCoal,
    onPrimary = MidnightBlack,
    onSecondary = PureWhite,
    onTertiary = MidnightBlack,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = NebulaDark,
    onSurfaceVariant = CosmicSlate
)

private val LightColorScheme = DarkColorScheme // Default to dark cosmic scheme for premium look

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark theme for immersive lab feel
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce brand identity
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
