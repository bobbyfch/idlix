package com.idlix.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.idlix.tv.ui.theme.NetflixGold
import com.idlix.tv.ui.theme.NetflixWhite

private val CardShape = RoundedCornerShape(20.dp)
private val BottomScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xEC000000))
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMovieCard(
    title: String,
    posterUrl: String,
    rating: String,
    quality: String? = null,
    modifier: Modifier = Modifier,
    width: Int = 154,
    height: Int = 228,
    onFocus: (() -> Unit)? = null,
    imageReferer: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(posterUrl, imageReferer) {
        ImageRequest.Builder(context)
            .data(posterUrl)
            .size(270, 400)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .apply {
                imageReferer?.takeIf { it.isNotBlank() }?.let { setHeader("Referer", it) }
            }
            .build()
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(width.dp)
            .height(height.dp)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocus?.invoke()
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.05f),
        glow = CardDefaults.glow(focusedGlow = Glow.None),
        shape = CardDefaults.shape(shape = CardShape),
        // TV focus is communicated by the subtle native scale, not a white poster frame.
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient scrim at bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(BottomScrimBrush)
            )

            // TV poster taxonomy follows mobile: quality top-left,
            // real numeric rating top-right. No imaginary HD labels.
            if (!quality.isNullOrBlank() && quality != "N/A") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = quality.uppercase(),
                        color = NetflixWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (rating.isNotBlank() && rating != "N/A") {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = NetflixGold,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        rating,
                        color = NetflixWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Keep the film title readable without overcrowding the poster.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 9.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    color = NetflixWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
