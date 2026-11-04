package com.idlix.tv.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleArchiveSafetyTest {
    @Test fun normalSubtitlePrefixIsStable() {
        assertEquals("subdl_id_42", subtitleCachePrefix("ID", 42L))
    }

    @Test fun remoteLanguageCannotEscapeCacheDirectory() {
        val name = subtitleCachePrefix("../../\\\\evil", 42L)
        assertFalse(name.contains("/"))
        assertFalse(name.contains("\\\\"))
        assertFalse(name.contains(".."))
        assertTrue(name.startsWith("subdl_"))
    }

    @Test fun excessivelyLongRemoteLanguageIsTruncated() {
        val name = subtitleCachePrefix("a".repeat(120), 100L)
        assertTrue(name.length <= 6 + 12 + 4)
    }
}
