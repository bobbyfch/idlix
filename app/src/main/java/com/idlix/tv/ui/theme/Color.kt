package com.idlix.tv.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Compatibility color tokens shared by TV and touch composables during Material 3 migration. */
object IdlixPalette {
    var isLight by mutableStateOf(false)
    var primary by mutableStateOf(Color(0xFFD0BCFF))
    var primaryContainer by mutableStateOf(Color(0xFF4F378B))
    var background by mutableStateOf(Color.Black)
    var surface by mutableStateOf(Color(0xFF1C1B1F))
    var surfaceVariant by mutableStateOf(Color(0xFF2B2930))
    var onSurface by mutableStateOf(Color.White)
    var onSurfaceVariant by mutableStateOf(Color(0xFFCAC4D0))
}
val NetflixRed: Color get() = IdlixPalette.primary
val NetflixRedDark: Color get() = IdlixPalette.primaryContainer
val NetflixBlack: Color get() = IdlixPalette.background
val NetflixDarkCard: Color get() = IdlixPalette.surface
val NetflixDarkGray: Color get() = IdlixPalette.surfaceVariant
val NetflixLightGray: Color get() = IdlixPalette.onSurfaceVariant
val NetflixWhite: Color get() = IdlixPalette.onSurface
val NetflixGold = Color(0xFFFFD166)
val NetflixOverlay = Color(0x99000000)
