package com.idlix.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekBoundsTest {
    @Test fun forwardSeekWithinDuration() {
        assertEquals(20_000L, safeForwardSeekPosition(10_000L, 100_000L))
    }

    @Test fun forwardSeekClampedToKnownDuration() {
        assertEquals(95_000L, safeForwardSeekPosition(90_000L, 95_000L))
    }

    @Test fun forwardSeekUnknownDurationNeverGoesNegative() {
        assertEquals(15_000L, safeForwardSeekPosition(5_000L, -9223372036854775807L))
        assertEquals(10_000L, safeForwardSeekPosition(-1L, -9223372036854775807L))
    }

    @Test fun forwardSeekDoesNotOverflow() {
        assertEquals(Long.MAX_VALUE, safeForwardSeekPosition(Long.MAX_VALUE - 5L, -1L))
    }

    @Test fun forwardSeekZeroDurationStopsAtZero() {
        assertEquals(0L, safeForwardSeekPosition(0L, 0L))
    }
}
