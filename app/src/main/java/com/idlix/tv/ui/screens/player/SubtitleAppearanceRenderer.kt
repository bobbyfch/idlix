package com.idlix.tv.ui.screens.player

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.idlix.tv.data.local.SubtitleAppearance
import com.idlix.tv.data.local.TvPreferences

/**
 * Media3 Canvas subtitle renderer: no WebView, no replacement of player or stream resolver.
 * Keep user-selected positioning and scale synchronized with settings and the player modal.
 */
@OptIn(UnstableApi::class)
internal fun SubtitleView.applyTvSubtitleAppearance(
    preferences: TvPreferences,
    controlsVisible: Boolean,
    nextEpisodeVisible: Boolean
) {
    // Embedded font sizes can ignore the viewer's preference. Respect formatting only
    // in System mode; custom cinema presets must have predictable contrast.
    setApplyEmbeddedStyles(preferences.subtitleAppearance == SubtitleAppearance.SYSTEM)
    setApplyEmbeddedFontSizes(false)
    when (preferences.subtitleAppearance) {
        SubtitleAppearance.SYSTEM -> setUserDefaultStyle()
        SubtitleAppearance.OUTLINE -> setStyle(
            CaptionStyleCompat(
                Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, Typeface.DEFAULT
            )
        )
        SubtitleAppearance.SHADOW -> setStyle(
            CaptionStyleCompat(
                Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, Color.BLACK, Typeface.DEFAULT
            )
        )
        SubtitleAppearance.SOFT_BOX -> setStyle(
            CaptionStyleCompat(
                Color.WHITE, Color.argb(155, 0, 0, 0), Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_NONE, Color.BLACK, Typeface.DEFAULT
            )
        )
    }

    // Slightly smaller than the Media3 default 0.0533f to keep two-line subtitles elegant.
    setFractionalTextSize(0.048f * preferences.subtitleSize.scale)
    setBottomPaddingFraction(
        safeSubtitleBottomPadding(
            preferences.subtitlePosition.bottomPaddingFraction,
            controlsVisible,
            nextEpisodeVisible
        )
    )
}
