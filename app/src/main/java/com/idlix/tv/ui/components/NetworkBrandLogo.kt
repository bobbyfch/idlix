package com.idlix.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlix.tv.R

/**
 * Offline provider marks for Network filters. The provider name remains beside
 * the mark so dynamic and regional networks are still easy to identify.
 */
@Composable
internal fun NetworkBrandLogo(
    slug: String,
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val resource = when (slug.lowercase()) {
        "netflix" -> R.drawable.network_netflix
        "hbo" -> R.drawable.network_hbo
        "apple-tv-plus" -> R.drawable.network_apple_tv
        else -> null
    }

    if (!compact && resource != null) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            modifier = modifier,
            alignment = Alignment.Center,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        return
    }

    if (!compact && slug.equals("disney-plus", ignoreCase = true)) {
        Text(
            text = "Disney+",
            color = Color(0xFF69B7FF),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Cursive,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = modifier
        )
        return
    }

    if (!compact && slug.equals("prime-video", ignoreCase = true)) {
        Text(
            text = "prime video",
            color = Color(0xFF4CC8FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = modifier
        )
        return
    }

    val mark = when (slug.lowercase()) {
        "netflix" -> "N"
        "disney-plus" -> "D+"
        "prime-video" -> "P"
        "apple-tv-plus" -> "A"
        "hbo" -> "H"
        else -> label.split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .take(2)
            .uppercase()
            .ifBlank { "•" }
    }
    val color = when (slug.lowercase()) {
        "netflix" -> Color(0xFFE50914)
        "disney-plus" -> Color(0xFF69B7FF)
        "prime-video" -> Color(0xFF4CC8FF)
        "apple-tv-plus", "hbo" -> Color.White
        "paramount-plus" -> Color(0xFF6CA7FF)
        "crunchyroll" -> Color(0xFFF47521)
        "youtube" -> Color(0xFFFF3333)
        "bilibili" -> Color(0xFF00A4DC)
        "bbc" -> Color(0xFFE5E5E5)
        else -> Color(0xFFFFB36B)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mark,
            color = color,
            fontSize = if (mark.length > 1) 8.sp else 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}
