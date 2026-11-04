package com.idlix.tv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.idlix.tv.ui.components.NavItem
import com.idlix.tv.ui.components.TvNavDrawer
import com.idlix.tv.ui.screens.browse.CatalogBrowseScreen
import com.idlix.tv.ui.screens.detail.DetailScreen
import com.idlix.tv.ui.screens.history.ContinueWatchingScreen
import com.idlix.tv.ui.screens.home.HomeScreen
import com.idlix.tv.ui.screens.movies.MoviesScreen
import com.idlix.tv.ui.screens.player.PlayerScreen
import com.idlix.tv.ui.screens.search.SearchScreen
import com.idlix.tv.ui.screens.series.SeriesScreen
import com.idlix.tv.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val selectedNavItem = when {
        currentRoute.startsWith("explore") || currentRoute.startsWith("browse/") -> NavItem.EXPLORE
        currentRoute.startsWith("shorts") -> NavItem.SHORTS
        currentRoute.startsWith("collections") -> NavItem.SEARCH
        currentRoute.startsWith("search") || currentRoute.startsWith("actor/") -> NavItem.SEARCH
        currentRoute.startsWith("continue_watching") -> NavItem.CONTINUE_WATCHING
        currentRoute.startsWith("movies") -> NavItem.MOVIES
        currentRoute.startsWith("series") -> NavItem.SERIES
        currentRoute.startsWith("settings") -> NavItem.SETTINGS
        else -> NavItem.HOME
    }

    val isFullScreenRoute = currentRoute.startsWith("player") || currentRoute.startsWith("detail")

    // Keep one NavHost instance so fullscreen routes preserve navigation state.
    TvNavDrawer(
            selectedItem = selectedNavItem,
            onItemSelected = { item ->
                val destination = when (item) {
                    NavItem.HOME -> Screen.Home.route
                    NavItem.SHORTS -> Screen.Shorts.route
                    NavItem.EXPLORE -> Screen.Explore.route
                    NavItem.SEARCH -> Screen.Search.route
                    NavItem.CONTINUE_WATCHING -> Screen.ContinueWatching.route
                    NavItem.MOVIES -> Screen.Movies.route
                    NavItem.SERIES -> Screen.Series.route
                    NavItem.SETTINGS -> Screen.Settings.route
                }
                // Same item must not rebuild the screen or reset TV focus.
                if (currentRoute != destination) {
                    navController.navigate(destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = modifier,
            visible = !isFullScreenRoute
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                setupNavRoutes(navController)
            }
        }
}

private fun androidx.navigation.NavGraphBuilder.setupNavRoutes(
    navController: androidx.navigation.NavHostController
) {
    composable(Screen.Home.route) {
        HomeScreen(
            onNavigateToBrowse = { section, value, filterType ->
                navController.navigate(
                    if (section == "collections") Screen.Search.route
                    else Screen.Browse.createRoute(section, value, filterType)
                )
            },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            },
            onNavigateToPlayer = { type, id, title, poster, backdrop, slug ->
                navController.navigate(Screen.Player.createRoute(type, id, title, poster, backdrop, slug))
            }
        )
    }

    composable(Screen.ContinueWatching.route) {
        ContinueWatchingScreen(
            onNavigateToPlayer = { type, id, title, poster, backdrop, slug, season, episode ->
                navController.navigate(Screen.Player.createRoute(type, id, title, poster, backdrop, slug, season, episode))
            },
            onNavigateToHome = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }

    composable(Screen.Movies.route) {
        MoviesScreen(
            onNavigateToBrowse = { section, value, filterType ->
                navController.navigate(
                    if (section == "collections") Screen.Search.route
                    else Screen.Browse.createRoute(section, value, filterType)
                )
            },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            },
            onNavigateToPlayer = { type, id, title, poster, backdrop, slug ->
                navController.navigate(Screen.Player.createRoute(type, id, title, poster, backdrop, slug))
            }
        )
    }

    composable(Screen.Series.route) {
        SeriesScreen(
            onNavigateToBrowse = { section, value, filterType ->
                navController.navigate(
                    if (section == "collections") Screen.Search.route
                    else Screen.Browse.createRoute(section, value, filterType)
                )
            },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            },
            onNavigateToPlayer = { type, id, title, poster, backdrop, slug ->
                navController.navigate(Screen.Player.createRoute(type, id, title, poster, backdrop, slug))
            }
        )
    }

    composable(
        Screen.ActorSearch.route,
        arguments = listOf(navArgument("name") { type = NavType.StringType })
    ) { entry ->
        val rawName = entry.arguments?.getString("name").orEmpty()
        val actorName = rawName
        SearchScreen(
            onNavigateToCollection = { slug -> navController.navigate(Screen.Browse.createRoute("collections", slug)) },
            initialQuery = actorName,
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            }
        )
    }

    composable(Screen.Search.route) {
        SearchScreen(
            onNavigateToCollection = { slug -> navController.navigate(Screen.Browse.createRoute("collections", slug)) },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            }
        )
    }

    composable(Screen.Explore.route) {
        CatalogBrowseScreen(
            section = "explore",
            onSelectSection = { section ->
                navController.navigate(
                    if (section == "collections") Screen.Search.route
                    else Screen.Browse.createRoute(section)
                )
            },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            }
        )
    }

    composable(Screen.Shorts.route) {
        CatalogBrowseScreen(
            section = "shorts",
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            }
        )
    }

    // Legacy route remains reachable from old links, but Collections lives in Search.
    composable(Screen.Collections.route) {
        SearchScreen(
            onNavigateToCollection = { slug -> navController.navigate(Screen.Browse.createRoute("collections", slug)) },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            }
        )
    }

    composable(
        route = Screen.Browse.route,
        arguments = listOf(
            navArgument("section") { type = NavType.StringType },
            navArgument("value") { type = NavType.StringType; defaultValue = "" },
            navArgument("type") { type = NavType.StringType; defaultValue = "all" }
        )
    ) { entry ->
        CatalogBrowseScreen(
            section = entry.arguments?.getString("section") ?: "genre",
            initialValue = entry.arguments?.getString("value").orEmpty(),
            initialType = entry.arguments?.getString("type") ?: "all",
            onSelectSection = { section ->
                navController.navigate(Screen.Browse.createRoute(section)) {
                    launchSingleTop = true
                }
            },
            onNavigateToDetail = { type, slug ->
                navController.navigate(Screen.Detail.createRoute(type, slug))
            }
        )
    }

    composable(Screen.Settings.route) {
        SettingsScreen()
    }

    composable(
        route = Screen.Detail.route,
        arguments = listOf(
            navArgument("contentType") { type = NavType.StringType },
            navArgument("slug") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val contentType = backStackEntry.arguments?.getString("contentType") ?: "movie"
        val slug = backStackEntry.arguments?.getString("slug") ?: ""

        DetailScreen(
            contentType = contentType,
            slug = slug,
            onCastClick = { name ->
                navController.navigate(Screen.ActorSearch.createRoute(name))
            },
            onNavigateToPlayer = { type, id, title, poster, backdrop, slugParam, season, episode ->
                navController.navigate(Screen.Player.createRoute(type, id, title, poster, backdrop, slugParam, season, episode))
            }
        )
    }

    composable(
        route = Screen.Player.route,
        arguments = listOf(
            navArgument("contentType") { type = NavType.StringType },
            navArgument("contentId") { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType },
            navArgument("poster") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            },
            navArgument("backdrop") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            },
            navArgument("slug") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            },
            navArgument("season") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            },
            navArgument("episode") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            }
        )
    ) { backStackEntry ->
        val contentType = backStackEntry.arguments?.getString("contentType") ?: "movie"
        val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
        val title = backStackEntry.arguments?.getString("title").orEmpty()
        val poster = backStackEntry.arguments?.getString("poster")?.takeIf { it.isNotBlank() }
        val backdrop = backStackEntry.arguments?.getString("backdrop")?.takeIf { it.isNotBlank() }
        val slug = backStackEntry.arguments?.getString("slug")?.takeIf { it.isNotBlank() }
        val season = backStackEntry.arguments?.getString("season")?.toIntOrNull()
        val episode = backStackEntry.arguments?.getString("episode")?.toIntOrNull()

        PlayerScreen(
            contentType = contentType,
            contentId = contentId,
            title = title,
            posterUrl = poster,
            backdropUrl = backdrop,
            slug = slug,
            seasonNumber = season,
            episodeNumber = episode,
            onBack = { navController.popBackStack() }
        )
    }
}
