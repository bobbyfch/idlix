package com.idlix.tv.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixLightGray
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

/**
 * Tonal Material surface by default; optional source-backed Haze blur.
 * Keeps static fallback on devices where blur would be expensive.
 */
internal fun Modifier.mobileGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    strong: Boolean = false,
    hazeState: HazeState? = null
): Modifier {
    val blur = if (hazeState != null) {
        Modifier.hazeChild(state = hazeState, style = HazeStyle(backgroundColor = NetflixDarkCard, tint = null))
    } else Modifier
    // Frosted acrylic: heavier tonal wash, blur remains optional on older devices.
    val alpha = if (hazeState != null) 0.89f else if (strong) 0.97f else 0.95f
    return this.clip(shape)
        .then(blur)
        .background(NetflixDarkCard.copy(alpha = alpha), shape)
        .border(0.7.dp, NetflixLightGray.copy(alpha = 0.18f), shape)
}
