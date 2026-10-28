package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CDPrimaryLight,
    onPrimary = Color(0xFF00354E),
    primaryContainer = CDPrimaryDark,
    onPrimaryContainer = CDPrimaryContainer,
    secondary = CDSecondaryContainer,
    onSecondary = Color(0xFF00354E),
    background = CDBackgroundDark,
    surface = CDSurfaceDark,
    onBackground = CDTextPrimaryDark,
    onSurface = CDTextPrimaryDark,
    surfaceVariant = CDSurfaceVariantDark,
    onSurfaceVariant = CDTextSecondaryDark,
    outline = CDOutlineDark,
    error = CDDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CDPrimary,
    onPrimary = Color.White,
    primaryContainer = CDPrimaryContainer,
    onPrimaryContainer = CDOnPrimaryContainer,
    secondary = CDPrimaryDark,
    onSecondary = Color.White,
    background = CDBackgroundLight,
    surface = CDSurfaceLight,
    onBackground = CDTextPrimaryLight,
    onSurface = CDTextPrimaryLight,
    surfaceVariant = CDSurfaceVariantLight,
    onSurfaceVariant = CDTextSecondaryLight,
    outline = CDOutlineLight,
    error = CDDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false by default to retain the Cobranzas CD brand colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
