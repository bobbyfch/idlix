package com.idlix.tv.ui.screens.player

/**
 * Keep captions clear of transient TV transport controls, but return them to the viewer's
 * preferred baseline when the controls disappear. All fractions refer to video-view height.
 */
internal fun safeSubtitleBottomPadding(
    preferredPadding: Float,
    controlsVisible: Boolean,
    nextEpisodeVisible: Boolean
): Float = maxOf(
    preferredPadding.coerceIn(0.02f, 0.30f),
    if (controlsVisible) 0.24f else 0f,
    if (nextEpisodeVisible) 0.18f else 0f
)
