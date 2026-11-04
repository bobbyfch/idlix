package com.idlix.tv

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.idlix.tv.data.local.useTabletLayout
import com.idlix.tv.data.local.TvPreferencesStore
import com.idlix.tv.data.local.useTvLayout
import com.idlix.tv.ui.mobile.IdlixMobileTheme
import com.idlix.tv.ui.mobile.MobileNavigation
import com.idlix.tv.ui.navigation.AppNavigation
import com.idlix.tv.ui.theme.IdlixTvTheme

/**
 * Live layout selector. AUTO follows actual Android TV device flags;
 * TV and MOBILE explicitly override it, regardless of touch capability.
 * Switching the graph returns to the matching layout's home screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // One consistent splash implementation from Android 8 through Android 15+.
        // The compat API swaps the starting theme to Theme.IdlixTV at launch.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val deviceIsTelevision =
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            (resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_TELEVISION

        setContent {
            val preferences by TvPreferencesStore.preferences.collectAsState()
            val useTelevisionLayout = preferences.layoutMode.useTvLayout(deviceIsTelevision)
            val tabletLayout = preferences.layoutMode.useTabletLayout(
                deviceIsTelevision,
                LocalConfiguration.current.smallestScreenWidthDp
            )

            // Apply only when switching layouts so a video player's own orientation
            // changes are not overwritten on ordinary recomposition.
            LaunchedEffect(useTelevisionLayout) {
                if (useTelevisionLayout) {
                    if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Different navigation roots are intentionally replaced on mode change:
            // old TV focus state and mobile back stack must never leak across layouts.
            if (useTelevisionLayout) {
                IdlixTvTheme { AppNavigation() }
            } else {
                IdlixMobileTheme { MobileNavigation(tabletMode = tabletLayout) }
            }
        }
    }
}
