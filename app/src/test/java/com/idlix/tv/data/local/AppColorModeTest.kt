package com.idlix.tv.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppColorModeTest {
    @Test fun onlyThreeBuiltInThemesRemain() {
        assertEquals(listOf(AppColorMode.LIGHT, AppColorMode.DARK, AppColorMode.OLED),
            AppColorMode.values().toList())
    }

    @Test fun existingModesHaveStablePersistenceNames() {
        assertEquals("LIGHT", AppColorMode.LIGHT.name)
        assertEquals("DARK", AppColorMode.DARK.name)
        assertEquals("OLED", AppColorMode.OLED.name)
        assertFalse(AppColorMode.values().any { it.name == "IDLIX" || it.name == "MATERIAL" })
    }
}
