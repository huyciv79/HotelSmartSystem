package com.example.hotelsmartbooking.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = NavyPrimary,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = TextPrimaryDark,
    secondary = NavySecondary,
    onSecondary = TextPrimaryDark,
    background = NavyBackground,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkCardBackground,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = GoldAccent,
    onPrimary = NavyPrimary,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = TextPrimaryDark,
    secondary = NavySecondary,
    onSecondary = LightSurface,
    background = LightSurface,
    onBackground = TextPrimaryLight,
    surface = LightCardBackground,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed
)

@Composable
fun HotelSmartBookingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
