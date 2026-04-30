package com.example.pantrypure.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FreshGreenPrimary,
    onPrimary = FreshGreenOnPrimary,
    primaryContainer = FreshGreenPrimaryContainer,
    onPrimaryContainer = FreshGreenOnPrimaryContainer,
    secondary = SunOrangeSecondary,
    onSecondary = SunOrangeOnSecondary,
    secondaryContainer = SunOrangeSecondaryContainer,
    onSecondaryContainer = SunOrangeOnSecondaryContainer,
    tertiary = BerryTertiary,
    onTertiary = BerryOnTertiary,
    tertiaryContainer = BerryTertiaryContainer,
    onTertiaryContainer = BerryOnTertiaryContainer,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFreshGreenPrimary,
    onPrimary = DarkFreshGreenOnPrimary,
    primaryContainer = DarkFreshGreenPrimaryContainer,
    onPrimaryContainer = DarkFreshGreenOnPrimaryContainer,
    secondary = DarkSunOrangeSecondary,
    onSecondary = DarkSunOrangeOnSecondary,
    secondaryContainer = DarkSunOrangeSecondaryContainer,
    onSecondaryContainer = DarkSunOrangeOnSecondaryContainer,
    tertiary = DarkBerryTertiary,
    onTertiary = DarkBerryOnTertiary,
    tertiaryContainer = DarkBerryTertiaryContainer,
    onTertiaryContainer = DarkBerryOnTertiaryContainer,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun PantryPureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar and navigation bar to transparent for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
