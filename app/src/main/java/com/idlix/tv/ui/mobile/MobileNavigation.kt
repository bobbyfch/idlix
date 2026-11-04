package com.idlix.tv.ui.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.app.Activity
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.idlix.tv.data.local.TvPreferencesStore
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.idlix.tv.ui.navigation.Screen
import com.idlix.tv.ui.theme.NetflixRed
import com.idlix.tv.ui.theme.NetflixBlack
import com.idlix.tv.data.local.AppColorMode
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private data class MobileTab(val route: String, val label: String, val icon: ImageVector)

private val mobileTabs = listOf(
    MobileTab(Screen.Home.route, "Beranda", Icons.Default.Home),
    MobileTab(Screen.Movies.route, "Film", Icons.Default.Movie),
    MobileTab(Screen.Series.route, "Serial", Icons.Default.Tv),
    MobileTab(Screen.Explore.route, "Jelajah", Icons.Default.Explore),
    MobileTab(Screen.Search.route, "Cari", Icons.Default.Search)
)

/** A separate mobile graph keeps TV D-pad routes/layouts untouched. */
@Composable
fun MobileNavigation(tabletMode: Boolean = false) {
    val navController = rememberNavController()
    val preferences by TvPreferencesStore.preferences.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val navigationIsLight = if (preferences.materialYouEnabled) !systemDark
        else preferences.colorMode == AppColorMode.LIGHT
    val context = LocalContext.current
    val tabletLandscape = tabletMode &&
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val hazeState = remember { HazeState() }

    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Screen.Home.route
    val hideChrome = route.startsWith("detail/") || route.startsWith("player/")

    // Phone only: make status bar visible by default. Toggle survives restarts.
    DisposableEffect(context, preferences.mobileStatusBarEnabled, navigationIsLight, hideChrome) {
        val activity = context as? Activity
        activity?.let {
            val controller = WindowCompat.getInsetsController(it.window, it.window.decorView)
            controller.isAppearanceLightStatusBars =
                navigationIsLight && !hideChrome
            controller.isAppearanceLightNavigationBars =
                navigationIsLight && !hideChrome
            if (preferences.mobileStatusBarEnabled) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose { }
    }
    // Preserve search/filter/scroll in other tabs when switching the root.
    fun openTab(destination: String) {
        if (route == destination) return
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openDetails: (String, String) -> Unit = { type, slug ->
        if (slug.isNotBlank()) navController.navigate(Screen.Detail.createRoute(type, slug))
    }
    val openBrowse: OpenBrowse = { section, value, filterType ->
        navController.navigate(
            if (section == "collections") Screen.Search.route
            else Screen.Browse.createRoute(section, value, filterType)
        )
    }

    val openPlayer: MobilePlay = { type, id, title, poster, backdrop, slug, season, episode ->
        if (id.isNotBlank()) {
            navController.navigate(
                Screen.Player.createRoute(type, id, title, poster, backdrop, slug, season, episode)
            )
        }
    }

    // Content remains edge-to-edge beneath floating controls, rather than being
    // resized by a Material3 Scaffold bottomBar on every page.
    Box(Modifier.fillMaxSize().background(NetflixBlack)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
                .padding(start = if (tabletLandscape && !hideChrome) 88.dp else 0.dp)
                .then(
                if (preferences.mobileGlassEnabled) Modifier.haze(hazeState) else Modifier
            )
        ) {
            composable(Screen.Home.route) {
                MobileHomeScreen(onBrowse = openBrowse, onDetail = openDetails, onPlay = openPlayer)
            }
            composable(Screen.Movies.route) {
                MobileMoviesScreen(onBrowse = openBrowse, onDetail = openDetails, onPlay = openPlayer)
            }
            composable(Screen.Series.route) {
                MobileSeriesScreen(onBrowse = openBrowse, onDetail = openDetails)
            }
            composable(Screen.Explore.route) {
                MobileExploreScreen(onDetail = openDetails)
            }
            composable(
                Screen.Browse.route,
                arguments = listOf(
                    navArgument("section") { type = NavType.StringType },
                    navArgument("value") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") { type = NavType.StringType; defaultValue = "all" }
                )
            ) { entry ->
                MobileExploreScreen(
                    onDetail = openDetails,
                    initialSection = entry.arguments?.getString("section") ?: "genre",
                    initialValue = entry.arguments?.getString("value").orEmpty(),
                    initialType = entry.arguments?.getString("type") ?: "all"
                )
            }
            composable(Screen.Search.route) {
                MobileSearchScreen(onDetail = openDetails, onCollection = { slug -> navController.navigate(Screen.Browse.createRoute("collections", slug)) })
            }
            composable(Screen.Collections.route) {
                MobileSearchScreen(onDetail = openDetails, onCollection = { slug -> navController.navigate(Screen.Browse.createRoute("collections", slug)) })
            }
            composable(
                Screen.ActorSearch.route,
                arguments = listOf(navArgument("name") { type = NavType.StringType })
            ) { backStack ->
                val rawName = backStack.arguments?.getString("name").orEmpty()
                val actorName = runCatching {
                    URLDecoder.decode(rawName, StandardCharsets.UTF_8.toString())
                }.getOrDefault(rawName)
                MobileSearchScreen(onDetail = openDetails, onCollection = { slug -> navController.navigate(Screen.Browse.createRoute("collections", slug)) }, initialQuery = actorName)
            }
            composable(Screen.ContinueWatching.route) {
                MobileHistoryScreen(onPlay = openPlayer)
            }
            composable(Screen.Settings.route) {
                MobileSettingsScreen()
            }
            composable(
                Screen.Detail.route,
                arguments = listOf(
                    navArgument("contentType") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType }
                )
            ) { backStack ->
                MobileDetailScreen(
                    contentType = backStack.arguments?.getString("contentType") ?: "movie",
                    slug = backStack.arguments?.getString("slug").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onCastClick = { name ->
                        navController.navigate(Screen.ActorSearch.createRoute(name))
                    },
                    onPlay = openPlayer
                )
            }
            composable(
                Screen.Player.route,
                arguments = listOf(
                    navArgument("contentType") { type = NavType.StringType },
                    navArgument("contentId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("poster") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("backdrop") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("slug") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("season") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("episode") { type = NavType.StringType; nullable = true; defaultValue = "" }
                )
            ) { backStack ->
                fun decoded(name: String): String = backStack.arguments?.getString(name)
                    ?.let { raw -> runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8.toString()) }.getOrDefault(raw) }
                    .orEmpty()

                MobilePlayerScreen(
                    contentType = decoded("contentType"),
                    contentId = decoded("contentId"),
                    title = decoded("title"),
                    posterUrl = decoded("poster").ifBlank { null },
                    backdropUrl = decoded("backdrop").ifBlank { null },
                    slug = decoded("slug").ifBlank { null },
                    seasonNumber = decoded("season").toIntOrNull(),
                    episodeNumber = decoded("episode").toIntOrNull(),
                    onBack = { navController.popBackStack() },
                    tabletMode = tabletMode
                )
            }
        }
        if (!hideChrome) {
            MobileFloatingChrome(
                currentRoute = when {
                    route.startsWith("browse/") -> Screen.Explore.route
                    route.startsWith("actor/") -> Screen.Search.route
                    else -> route
                },
                navigateTo = { openTab(it) },
                tabletLandscape = tabletLandscape,
                hazeState = if (preferences.mobileGlassEnabled) hazeState else null
            )
        }
    }
}
