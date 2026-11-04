package com.idlix.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleLayoutTest {
    @Test fun preservesPreferredPositionWithoutOverlays() {
        assertEquals(0.085f, safeSubtitleBottomPadding(0.085f, false, false), 0.0001f)
        assertEquals(0.16f, safeSubtitleBottomPadding(0.16f, false, false), 0.0001f)
    }

    @Test fun avoidsTransportBarWhileVisible() {
        assertEquals(0.24f, safeSubtitleBottomPadding(0.085f, true, false), 0.0001f)
        assertEquals(0.24f, safeSubtitleBottomPadding(0.085f, true, true), 0.0001f)
    }

    @Test fun avoidsNextEpisodePrompt() {
        assertEquals(0.18f, safeSubtitleBottomPadding(0.085f, false, true), 0.0001f)
    }

    @Test fun clampsInvalidPadding() {
        assertEquals(0.02f, safeSubtitleBottomPadding(-5f, false, false), 0.0001f)
        assertEquals(0.30f, safeSubtitleBottomPadding(10f, false, false), 0.0001f)
    }
}
