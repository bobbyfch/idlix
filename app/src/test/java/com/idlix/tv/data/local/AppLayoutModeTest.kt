package com.idlix.tv.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLayoutModeTest {
    @Test fun automaticFollowsDeviceType() {
        assertTrue(AppLayoutMode.AUTO.useTvLayout(true))
        assertFalse(AppLayoutMode.AUTO.useTvLayout(false))
    }

    @Test fun tvLayoutCanBeForcedOnAnyDevice() {
        assertTrue(AppLayoutMode.TV.useTvLayout(true))
        assertTrue(AppLayoutMode.TV.useTvLayout(false))
    }

    @Test fun mobileLayoutCanBeForcedOnAnyDevice() {
        assertFalse(AppLayoutMode.MOBILE.useTvLayout(true))
        assertFalse(AppLayoutMode.MOBILE.useTvLayout(false))
    }

    @Test fun tabletModeAndAutoDetection() {
        assertFalse(AppLayoutMode.TABLET.useTvLayout(true))
        assertFalse(AppLayoutMode.TABLET.useTvLayout(false))
        assertTrue(AppLayoutMode.TABLET.useTabletLayout(true, 360))
        assertFalse(AppLayoutMode.MOBILE.useTabletLayout(false, 1200))
        assertTrue(AppLayoutMode.AUTO.useTabletLayout(false, 600))
        assertFalse(AppLayoutMode.AUTO.useTabletLayout(false, 599))
        assertFalse(AppLayoutMode.AUTO.useTabletLayout(true, 800))
        assertFalse(AppLayoutMode.TV.useTabletLayout(false, 1000))
    }

    @Test fun defaultPreferenceRemainsAutomaticForExistingInstalls() {
        assertTrue(TvPreferences().layoutMode == AppLayoutMode.AUTO)
    }
}
