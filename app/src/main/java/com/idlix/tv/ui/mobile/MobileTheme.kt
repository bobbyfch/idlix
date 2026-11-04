package com.idlix.tv.ui.mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.idlix.tv.ui.theme.IdlixPalette
import com.idlix.tv.ui.theme.resolveIdlixAppearance

@Composable
fun IdlixMobileTheme(content: @Composable () -> Unit) {
    val appearance = resolveIdlixAppearance()
    val scheme = appearance.scheme
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
    MaterialTheme(colorScheme = scheme, content = content)
}
