package com.idlix.tv.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdaterVersionTest {
    @Test fun comparesSemanticVersionComponentsNotLexicographicStrings() {
        assertEquals(1, AppUpdater.compareVersions("1.0.12", "1.0.9"))
        assertEquals(-1, AppUpdater.compareVersions("1.0.9", "1.0.10"))
        assertEquals(0, AppUpdater.compareVersions("v1.0.12", "1.0.12"))
        assertEquals(1, AppUpdater.compareVersions("2.0.0", "1.99.99"))
    }

    @Test fun rejectsMalformedVersions() {
        assertThrows(IllegalArgumentException::class.java) {
            AppUpdater.compareVersions("1.0.12-beta", "1.0.11")
        }
    }
    @Test fun migrationReleaseUsesIncreasingVersionCodeAcrossVersionNameReset() {
        val release = GithubRelease("1.0.0", "https://example.test/IDLIX-1.0.0.apk", 1L, null, 1_200_054L)
        assertTrue(AppUpdater.isNewer(release, 1_100_053, "1.0.17"))
        assertFalse(AppUpdater.isNewer(release, 1_200_054, "1.0.0"))
    }

    @Test fun olderReleaseMetadataFallsBackToSemanticVersion() {
        val release = GithubRelease("1.0.18", "https://example.test/IDLIX-1.0.18.apk", 1L, null)
        assertTrue(AppUpdater.isNewer(release, 1_100_053, "1.0.17"))
    }

}
