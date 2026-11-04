package com.idlix.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@Composable
fun IdlixTvTheme(content: @Composable () -> Unit) {
    val appearance = resolveIdlixAppearance()
    val scheme = appearance.scheme
    val tvScheme = if (appearance.light) lightColorScheme(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant
    ) else darkColorScheme(
        primary = scheme.primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant
    )
    SideEffect {
        IdlixPalette.isLight = appearance.light
        IdlixPalette.primary = scheme.primary
        IdlixPalette.primaryContainer = scheme.primaryContainer
        IdlixPalette.background = scheme.background
        IdlixPalette.surface = scheme.surface
        IdlixPalette.surfaceVariant = scheme.surfaceVariant
        IdlixPalette.onSurface = scheme.onSurface
        IdlixPalette.onSurfaceVariant = scheme.onSurfaceVariant
    }
    MaterialTheme(colorScheme = tvScheme, content = content)
}
