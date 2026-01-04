package com.babymonitor.ui.theme

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
    primary = Color(0xFF6B5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E5FF),
    onPrimaryContainer = Color(0xFF1D1148),
    secondary = Color(0xFF7DD3C0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F5ED),
    onSecondaryContainer = Color(0xFF00382C),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFE4BC),
    onTertiaryContainer = Color(0xFF3D2600),
    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFCFC3FF),
    onPrimary = Color(0xFF33277A),
    primaryContainer = Color(0xFF4A3D91),
    onPrimaryContainer = Color(0xFFE8E5FF),
    secondary = Color(0xFF9CF5DE),
    onSecondary = Color(0xFF00513E),
    secondaryContainer = Color(0xFF007358),
    onSecondaryContainer = Color(0xFFD4F5ED),
    tertiary = Color(0xFFFFD38A),
    onTertiary = Color(0xFF4E3A00),
    tertiaryContainer = Color(0xFF6F5200),
    onTertiaryContainer = Color(0xFFFFE4BC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun BabyMonitorTheme(
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
