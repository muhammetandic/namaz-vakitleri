package com.anttech.prayertimes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anttech.prayertimes.navigation.DefaultAppRoute
import com.anttech.prayertimes.navigation.NavDestination
import com.anttech.prayertimes.navigation.TopBarController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    routeContent: @Composable (
        route: String,
        navController: NavHostController,
        topBarController: TopBarController
    ) -> Unit = { route, nav, topBar -> DefaultAppRoute(route, nav, topBar) }
) {
    val topBarController = remember { TopBarController() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Times.route
    val publishedConfig by topBarController.config
    val topBarConfig = publishedConfig?.takeIf { it.ownerRoute == currentRoute }
    val isPrimaryTab = NavDestination.primaryTabs.any { it.route == currentRoute }
    val scale = topBarConfig?.fontScale ?: 1f

    Scaffold(
        topBar = {
            if (isPrimaryTab) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = topBarConfig?.title.orEmpty(),
                                fontSize = (20 * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = topBarConfig?.subtitle.orEmpty(),
                                fontSize = (12 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        topBarConfig?.actions?.invoke()
                        IconButton(
                            onClick = { navController.navigate("settings") },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            if (isPrimaryTab) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavDestination.primaryTabs.forEach { destination ->
                        val isSelected = currentRoute == destination.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(NavDestination.Times.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontSize = (12 * scale).sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Times.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavDestination.Times.route) {
                routeContent(NavDestination.Times.route, navController, topBarController)
            }
            composable(NavDestination.Imsakiye.route) {
                routeContent(NavDestination.Imsakiye.route, navController, topBarController)
            }
            composable(NavDestination.Qibla.route) {
                routeContent(NavDestination.Qibla.route, navController, topBarController)
            }
            composable(NavDestination.Tracker.route) {
                routeContent(NavDestination.Tracker.route, navController, topBarController)
            }
            composable("sound_settings") {
                routeContent("sound_settings", navController, topBarController)
            }
            composable("settings") {
                routeContent("settings", navController, topBarController)
            }
            composable("notifications_settings") {
                routeContent("notifications_settings", navController, topBarController)
            }
            composable("permissions") {
                routeContent("permissions", navController, topBarController)
            }
            composable("city_selection") {
                routeContent("city_selection", navController, topBarController)
            }
            composable("about") {
                routeContent("about", navController, topBarController)
            }
        }
    }
}
