package com.idlix.tv.ui.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlix.tv.ui.theme.NetflixRed

private fun compactPlayerTime(ms: Long): String {
    val sec = (ms.coerceAtLeast(0L) / 1000L)
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

@Composable
internal fun MobilePlayerControls(
    title: String,
    visible: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isLandscape: Boolean,
    onToggleVisibility: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenQuality: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onInteracted: () -> Unit
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleVisibility
            )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.56f),
                            0.22f to Color.Black.copy(alpha = 0.06f),
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.64f)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(start = 5.dp, end = 6.dp, top = if (isLandscape) 8.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onInteracted(); onBack() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xA8191A20), CircleShape)
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                    Text(
                        title,
                        modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = if (isLandscape) 15.sp else 13.sp
                    )
                    IconButton(
                        onClick = { onInteracted(); onOpenSubtitles() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xA8191A20), CircleShape)
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    ) {
                        Icon(Icons.Default.ClosedCaption, contentDescription = "Pilih subtitle",
                            tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { onInteracted(); onOpenQuality() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xA8191A20), CircleShape)
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    ) {
                        Icon(Icons.Default.HighQuality, contentDescription = "Pilih kualitas video",
                            tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = { onInteracted(); onOpenSettings() },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xA8191A20), CircleShape)
                            .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan video",
                            tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 38.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onInteracted(); onSeekBack() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x99171920), CircleShape)
                    ) {
                        Icon(Icons.Default.Replay10, "Mundur 10 detik", modifier = Modifier.size(30.dp),
                            tint = Color.White)
                    }
                    IconButton(
                        onClick = { onInteracted(); onPlayPause() },
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xD21B1C22), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            modifier = Modifier.size(36.dp),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { onInteracted(); onSeekForward() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x99171920), CircleShape)
                    ) {
                        Icon(Icons.Default.Forward10, "Maju 10 detik", modifier = Modifier.size(30.dp),
                            tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = if (isLandscape) 34.dp else 16.dp)
                        .padding(bottom = if (isLandscape) 16.dp else 9.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Slider(
                        value = if (scrubbing) scrubProgress else progress,
                        onValueChange = {
                            scrubbing = true
                            scrubProgress = it.coerceIn(0f, 1f)
                            onInteracted()
                        },
                        onValueChangeFinished = {
                            if (durationMs > 0L) {
                                onSeekTo((durationMs * scrubProgress).toLong())
                            }
                            scrubbing = false
                            onInteracted()
                        },
                        enabled = durationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = NetflixRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f),
                            disabledThumbColor = Color.White.copy(alpha = 0.55f),
                            disabledActiveTrackColor = Color.White.copy(alpha = 0.28f),
                            disabledInactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(30.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            compactPlayerTime(if (scrubbing) (scrubProgress * durationMs).toLong() else currentPositionMs),
                            fontSize = 11.sp, color = Color.White
                        )
                        Text(
                            compactPlayerTime(durationMs),
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
