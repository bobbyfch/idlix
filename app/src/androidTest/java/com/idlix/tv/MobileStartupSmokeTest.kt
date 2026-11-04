package com.idlix.tv

import android.app.UiModeManager
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on an actual Android emulator display, not a Kotlin/JVM-only test.
 *
 * Catches the v1.1.6 class of failure: Compose builds successfully, but the
 * HazeChildNode draw pass throws when its backgroundColor is unspecified.
 * The Activity must remain alive after several frames and async home loading.
 */
@RunWith(AndroidJUnit4::class)
class MobileStartupSmokeTest {
    @Test
    fun homeDrawsAndRemainsAliveWithFloatingGlass() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        assertFalse(uiMode.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            SystemClock.sleep(2500L)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse("Activity unexpectedly finishing", activity.isFinishing)
                assertTrue("Activity should have a visible root", activity.window.decorView.isShown)
            }
        } finally {
            scenario.close()
        }
    }
}
