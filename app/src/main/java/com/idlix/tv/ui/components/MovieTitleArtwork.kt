package com.idlix.tv.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Official title artwork where available, styled typography everywhere else.
 * During video preparation the logo breathes; the player resolver still runs
 * independently, and animation disappears immediately once playback starts.
 */
@Composable
fun MovieTitleArtwork(
    title: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 30.sp,
    alignment: Alignment = Alignment.CenterStart,
    textAlign: TextAlign = TextAlign.Start,
    breathing: Boolean = false
) {
    val context = LocalContext.current
    var logoLoaded by remember(logoUrl) { mutableStateOf(false) }
    var logoFailed by remember(logoUrl) { mutableStateOf(false) }

    val animation = if (breathing) rememberInfiniteTransition(label = "Title logo breathing") else null
    val animatedScale = animation?.animateFloat(
        initialValue = 0.965f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Title logo scale"
    )?.value ?: 1f
    val animatedAlpha = animation?.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Title logo opacity"
    )?.value ?: 1f

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        },
        contentAlignment = alignment
    ) {
        if (!logoLoaded || logoFailed) {
            Text(
                text = title.ifBlank { "IDLIX" },
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Black,
                lineHeight = textSize * 1.12f,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign
            )
        }
        if (!logoUrl.isNullOrBlank() && !logoFailed) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(220)
                    .build(),
                contentDescription = "Logo " + title,
                contentScale = ContentScale.Fit,
                onSuccess = { logoLoaded = true },
                onError = { logoFailed = true },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (logoLoaded) 1f else 0f
                }
            )
        }
    }
}
