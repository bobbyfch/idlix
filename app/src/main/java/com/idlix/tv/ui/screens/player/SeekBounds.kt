package com.idlix.tv.ui.screens.player

/**
 * Safe forward seek, including streams with an unknown/unset duration.
 * Media3 reports unknown duration as a negative TIME_UNSET value.
 */
internal fun safeForwardSeekPosition(
    currentPositionMs: Long,
    durationMs: Long,
    stepMs: Long = 10_000L
): Long {
    val current = currentPositionMs.coerceAtLeast(0L)
    val step = stepMs.coerceAtLeast(0L)
    val target = if (current > Long.MAX_VALUE - step) Long.MAX_VALUE else current + step
    return if (durationMs >= 0L) target.coerceAtMost(durationMs) else target
}
