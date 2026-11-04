package com.idlix.tv.ui.screens.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.idlix.tv.data.local.VideoResizeMode

/** Affects only rendering, never modifies the source file or media stream. */
@OptIn(UnstableApi::class)
internal fun VideoResizeMode.toMedia3ResizeMode(): Int = when (this) {
    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    VideoResizeMode.FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
    VideoResizeMode.FIXED_HEIGHT -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
}
