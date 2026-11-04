package com.idlix.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.idlix.tv.ui.theme.NetflixDarkCard
import com.idlix.tv.ui.theme.NetflixLightGray

/** Frosted visual weight without expensive shader blur on Android TV 11. */
fun Modifier.glassSurface(intensity: Int, shape: Shape = RoundedCornerShape(16.dp)): Modifier {
    val opacity = when (intensity.coerceIn(1, 3)) {
        1 -> 0.93f
        2 -> 0.975f
        else -> 0.995f
    }
    return this.clip(shape)
        .background(NetflixDarkCard.copy(alpha = opacity), shape)
        .border(0.7.dp, NetflixLightGray.copy(alpha = 0.17f), shape)
}
