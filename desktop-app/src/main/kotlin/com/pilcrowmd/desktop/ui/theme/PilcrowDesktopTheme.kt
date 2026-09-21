package com.pilcrowmd.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun PilcrowDesktopTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val mdColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    // We map a few basic Material colors just in case standard components are used
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            background = mdColorScheme.primaryBackground,
            surface = mdColorScheme.secondarySurface,
            onBackground = mdColorScheme.primaryText,
            onSurface = mdColorScheme.primaryText,
            primary = mdColorScheme.accent,
            onPrimary = mdColorScheme.onAccent,
            error = mdColorScheme.error,
            onError = mdColorScheme.onError,
        )
    } else {
        lightColorScheme(
            background = mdColorScheme.primaryBackground,
            surface = mdColorScheme.secondarySurface,
            onBackground = mdColorScheme.primaryText,
            onSurface = mdColorScheme.primaryText,
            primary = mdColorScheme.accent,
            onPrimary = mdColorScheme.onAccent,
            error = mdColorScheme.error,
            onError = mdColorScheme.onError,
        )
    }


    CompositionLocalProvider(
        LocalMDColors provides mdColorScheme
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            
            content = content
        )
    }
}
