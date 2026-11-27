package com.anttech.prayertimes.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeIntent
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeScreen
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeViewModel
import com.anttech.prayertimes.features.qibla.ui.QiblaIntent
import com.anttech.prayertimes.features.qibla.ui.QiblaScreen
import com.anttech.prayertimes.features.qibla.ui.QiblaViewModel
import com.anttech.prayertimes.features.settings.ui.CitySelectionScreen
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsScreen
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsViewModel
import com.anttech.prayertimes.features.settings.ui.PermissionsRoute
import com.anttech.prayertimes.features.settings.ui.SettingsScreen
import com.anttech.prayertimes.features.settings.ui.SoundSettingsScreen
import com.anttech.prayertimes.features.settings.ui.SoundSettingsViewModel
import com.anttech.prayertimes.features.settings.ui.rememberNotificationPermission
import com.anttech.prayertimes.features.times.ui.TimesIntent
import com.anttech.prayertimes.features.times.ui.TimesScreen
import com.anttech.prayertimes.features.times.ui.TimesViewModel
import com.anttech.prayertimes.features.tracker.ui.TrackerScreen
import com.anttech.prayertimes.features.tracker.ui.TrackerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun sharedTimesViewModel(navController: NavHostController): TimesViewModel {
    return hiltViewModel(navController.getBackStackEntry(NavDestination.Times.route))
}

@Composable
private fun sharedFontScale(navController: NavHostController): Float {
    val timesViewModel = sharedTimesViewModel(navController)
    val scale by timesViewModel.uiState
        .map { it.fontSizeMultiplier }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = timesViewModel.uiState.value.fontSizeMultiplier)
    return scale
}

@Composable
internal fun DefaultAppRoute(
    route: String,
    navController: NavHostController,
    topBarController: TopBarController
) {
    when (route) {
        NavDestination.Times.route -> TimesRoute(navController, topBarController)
        NavDestination.Imsakiye.route -> ImsakiyeRoute(navController, topBarController)
        NavDestination.Qibla.route -> QiblaRoute(navController, topBarController)
        NavDestination.Tracker.route -> TrackerRoute(navController, topBarController)
        NavDestination.Sounds.route -> SoundSettingsRoute(navController)
        "settings" -> SettingsRoute(navController)
        "notifications_settings" -> NotificationSettingsRoute(navController)
        "permissions" -> PermissionsRoute(
            onBackClick = { navController.popBackStack() },
            fontSizeMultiplier = sharedFontScale(navController)
        )
        "city_selection" -> CitySelectionRoute(navController)
        "about" -> com.anttech.prayertimes.features.settings.ui.AboutScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}

@Composable
internal fun TimesRoute(
    navController: NavHostController,
    topBarController: TopBarController
) {
    val viewModel: TimesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.refreshLocationOnce()
    }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.sendIntent(TimesIntent.TickSecond)
            while (true) {
                delay(1_000L)
                viewModel.sendIntent(TimesIntent.TickSecond)
            }
        }
    }

    SideEffect {
        topBarController.update(
            ownerRoute = NavDestination.Times.route,
            title = state.city,
            subtitle = "Namaz Vakitleri",
            fontScale = state.fontSizeMultiplier,
            actionsKey = state.isSimpleMode
        ) {
            IconButton(
                onClick = { viewModel.sendIntent(TimesIntent.ToggleSimpleMode) },
                modifier = Modifier.testTag("toggle_simple_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = if (state.isSimpleMode) {
                        "Standart Moda Geç"
                    } else {
                        "Sade Moda Geç"
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { viewModel.sendIntent(TimesIntent.UseLocation) },
                modifier = Modifier.testTag("refresh_location_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Mevcut Konumu Bul",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { topBarController.clear(NavDestination.Times.route) }
    }

    TimesScreen(
        state = state,
        onIntent = viewModel::sendIntent,
        wrapInScaffold = false
    )
}

@Composable
internal fun ImsakiyeRoute(
    navController: NavHostController,
    topBarController: TopBarController
) {
    val viewModel: ImsakiyeViewModel = hiltViewModel()
    val timesViewModel = sharedTimesViewModel(navController)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scale = sharedFontScale(navController)
    val city by timesViewModel.uiState
        .map { it.city }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(initialValue = timesViewModel.uiState.value.city)

    LaunchedEffect(city) {
        val storedMonth = state.yearMonth.takeIf { it.isNotBlank() && city == state.city }
        val yearMonth = storedMonth ?: currentYearMonth()
        viewModel.sendIntent(ImsakiyeIntent.LoadImsakiye(city, yearMonth))
    }

    SideEffect {
        topBarController.update(
            ownerRoute = NavDestination.Imsakiye.route,
            title = "$city İmsakiyesi",
            subtitle = turkishMonthLabel(state.yearMonth),
            fontScale = scale,
            actionsKey = null
        ) {
            IconButton(
                onClick = { viewModel.sendIntent(ImsakiyeIntent.ShareTodayImsakiye) },
                modifier = Modifier.testTag("share_imsakiye_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Bugünün imsakiyesini paylaş",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = {
                    viewModel.sendIntent(
                        ImsakiyeIntent.LoadImsakiye(city, state.yearMonth, force = true)
                    )
                },
                modifier = Modifier.testTag("refresh_imsakiye_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "İmsakiyeyi yenile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { topBarController.clear(NavDestination.Imsakiye.route) }
    }

    ImsakiyeScreen(
        state = state.copy(fontSizeMultiplier = scale),
        onIntent = viewModel::sendIntent,
        wrapInScaffold = false
    )
}


@Composable
internal fun CompassLifecycle(
    latitude: Double,
    longitude: Double,
    cityName: String,
    onIntent: (QiblaIntent) -> Unit
) {
    LaunchedEffect(latitude, longitude, cityName) {
        onIntent(
            QiblaIntent.StartCompass(
                latitude = latitude,
                longitude = longitude,
                cityName = cityName
            )
        )
    }
    DisposableEffect(Unit) {
        onDispose { onIntent(QiblaIntent.StopCompass) }
    }
}

@Composable
internal fun QiblaRoute(
    navController: NavHostController,
    topBarController: TopBarController
) {
    val viewModel: QiblaViewModel = hiltViewModel()
    val timesViewModel = sharedTimesViewModel(navController)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timesState by timesViewModel.uiState.collectAsStateWithLifecycle()

    CompassLifecycle(
        latitude = timesState.latitude,
        longitude = timesState.longitude,
        cityName = timesState.city,
        onIntent = viewModel::sendIntent
    )

    SideEffect {
        topBarController.update(
            ownerRoute = NavDestination.Qibla.route,
            title = "Kıble Pusulası",
            subtitle = "Kıble Yönü",
            fontScale = timesState.fontSizeMultiplier
        ) {}
    }
    DisposableEffect(Unit) {
        onDispose { topBarController.clear(NavDestination.Qibla.route) }
    }

    QiblaScreen(
        state = state,
        onIntent = viewModel::sendIntent,
        fontScale = timesState.fontSizeMultiplier,
        wrapInScaffold = false
    )
}

@Composable
internal fun TrackerRoute(
    navController: NavHostController,
    topBarController: TopBarController
) {
    val viewModel: TrackerViewModel = hiltViewModel()
    val scale = sharedFontScale(navController)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SideEffect {
        topBarController.update(
            ownerRoute = NavDestination.Tracker.route,
            title = "Zikirmatik",
            subtitle = "Zikir",
            fontScale = scale
        ) {}
    }
    DisposableEffect(Unit) {
        onDispose { topBarController.clear(NavDestination.Tracker.route) }
    }

    TrackerScreen(
        state = state.copy(fontSizeMultiplier = scale),
        onIntent = viewModel::sendIntent,
        wrapInScaffold = false
    )
}

@Composable
internal fun SoundSettingsRoute(navController: NavHostController) {
    val viewModel: SoundSettingsViewModel = hiltViewModel()
    val scale = sharedFontScale(navController)
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SoundSettingsScreen(
        state = state.copy(fontSizeMultiplier = scale),
        onIntent = viewModel::sendIntent,
        onBackClick = { navController.popBackStack() }
    )
}

@Composable
internal fun SettingsRoute(navController: NavHostController) {
    val timesViewModel = sharedTimesViewModel(navController)
    val state by timesViewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        isSimpleMode = state.isSimpleMode,
        fontScale = state.fontSizeMultiplier,
        onSimpleModeToggle = {
            timesViewModel.sendIntent(TimesIntent.ToggleSimpleMode)
        },
        onFontScaleChange = { scale ->
            timesViewModel.sendIntent(TimesIntent.SetFontScale(scale))
        },
        onBackClick = { navController.popBackStack() },
        onSoundSettingsClick = { navController.navigate(NavDestination.Sounds.route) },
        onNotificationsClick = { navController.navigate("notifications_settings") },
        onCitySelectionClick = { navController.navigate("city_selection") },
        onAboutClick = { navController.navigate("about") },
        onPermissionsClick = { navController.navigate("permissions") }
    )
}

@Composable
internal fun NotificationSettingsRoute(navController: NavHostController) {
    val viewModel: NotificationSettingsViewModel = hiltViewModel()
    val scale = sharedFontScale(navController)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val (hasPermission, requestPermission) = rememberNotificationPermission()
    val settingsContext = LocalContext.current

    NotificationSettingsScreen(
        state = state.copy(fontSizeMultiplier = scale),
        hasNotificationPermission = hasPermission,
        onIntent = viewModel::sendIntent,
        onBackClick = { navController.popBackStack() },
        onRequestPermission = requestPermission,
        onBatteryOptimizationClick = {
            runCatching {
                settingsContext.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    )
}

@Composable
internal fun CitySelectionRoute(navController: NavHostController) {
    val timesViewModel = sharedTimesViewModel(navController)
    val state by timesViewModel.uiState.collectAsStateWithLifecycle()

    CitySelectionScreen(
        currentCity = state.city,
        onCitySelected = { selectedCity ->
            timesViewModel.sendIntent(TimesIntent.ChangeCity(selectedCity))
            navController.popBackStack()
        },
        onUseLocation = {
            timesViewModel.sendIntent(TimesIntent.UseLocation)
            navController.popBackStack()
        },
        onBackClick = { navController.popBackStack() }
    )
}

internal fun currentYearMonth(): String {
    return DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now())
}

internal fun turkishMonthLabel(yearMonth: String): String {
    if (yearMonth.isBlank()) return "Aylık İmsakiye"
    return runCatching {
        YearMonth.parse(yearMonth)
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("tr-TR")))
            .replaceFirstChar { it.titlecase(Locale.forLanguageTag("tr-TR")) }
    }.getOrElse { yearMonth }
}
