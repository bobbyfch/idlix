package com.idlix.tv.ui.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.idlix.tv.R
import com.idlix.tv.ui.navigation.Screen
import dev.chrisbanes.haze.HazeState

private data class ChromeTab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    ChromeTab(Screen.Home.route, "Beranda", Icons.Default.Home),
    ChromeTab(Screen.Movies.route, "Film", Icons.Default.Movie),
    ChromeTab(Screen.Series.route, "Serial", Icons.Default.Tv),
    ChromeTab(Screen.Explore.route, "Jelajah", Icons.Default.Explore),
    ChromeTab(Screen.Search.route, "Cari", Icons.Default.Search)
)

/**
 * Mobile layout ALWAYS has a bottom navigation bar, even on tablets, desktop
 * windows and landscape phones. TV navigation is a separate graph.
 */
@Composable
internal fun MobileFloatingChrome(
    currentRoute: String,
    navigateTo: (String) -> Unit,
    tabletLandscape: Boolean,
    hazeState: HazeState? = null
) {
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .mobileGlass(RoundedCornerShape(0.dp), hazeState = hazeState)
                .statusBarsPadding()
                .height(62.dp)
                .padding(start = if (tabletLandscape) 106.dp else 18.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.idlix_logo),
                contentDescription = "IDLIX TV",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(108.dp).height(32.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigateTo(Screen.ContinueWatching.route) }) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Lanjutkan Menonton",
                        tint = if (currentRoute == Screen.ContinueWatching.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { navigateTo(Screen.Settings.route) }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = if (currentRoute == Screen.Settings.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (tabletLandscape) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(88.dp)
                    .mobileGlass(RoundedCornerShape(0.dp), strong = true, hazeState = hazeState)
                    .statusBarsPadding()
                    .padding(top = 62.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                NavigationRail(containerColor = Color.Transparent) {
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = currentRoute == tab.route,
                            onClick = { navigateTo(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        } else if (!keyboardVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .mobileGlass(RoundedCornerShape(0.dp), strong = true, hazeState = hazeState)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navigateTo(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
