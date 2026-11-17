package com.anttech.prayertimes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.provider.Settings
import com.anttech.prayertimes.features.qibla.ui.QiblaIntent
import com.anttech.prayertimes.features.qibla.ui.QiblaScreen
import com.anttech.prayertimes.features.qibla.ui.QiblaUiState
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsIntent
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsScreen
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsUiState
import com.anttech.prayertimes.features.settings.ui.PermissionsRoute
import com.anttech.prayertimes.features.settings.ui.rememberNotificationPermission
import com.anttech.prayertimes.features.settings.ui.SoundSettingsIntent
import com.anttech.prayertimes.features.settings.ui.SoundSettingsScreen
import com.anttech.prayertimes.features.settings.ui.SoundSettingsUiState
import com.anttech.prayertimes.features.settings.ui.SettingsScreen
import com.anttech.prayertimes.features.times.ui.TimesIntent
import com.anttech.prayertimes.features.times.ui.TimesScreen
import com.anttech.prayertimes.features.times.ui.TimesUiState
import com.anttech.prayertimes.features.tracker.ui.TrackerIntent
import com.anttech.prayertimes.features.tracker.ui.TrackerScreen
import com.anttech.prayertimes.features.tracker.ui.TrackerUiState
import com.anttech.prayertimes.navigation.NavDestination
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    timesState: TimesUiState,
    onTimesIntent: (TimesIntent) -> Unit,
    imsakiyeState: com.anttech.prayertimes.features.calendar.ui.ImsakiyeUiState = com.anttech.prayertimes.features.calendar.ui.ImsakiyeUiState(isLoading = false),
    onImsakiyeIntent: (com.anttech.prayertimes.features.calendar.ui.ImsakiyeIntent) -> Unit = {},
    qiblaState: QiblaUiState,
    onQiblaIntent: (QiblaIntent) -> Unit,
    trackerState: TrackerUiState,
    onTrackerIntent: (TrackerIntent) -> Unit,
    soundState: SoundSettingsUiState,
    onSoundIntent: (SoundSettingsIntent) -> Unit,
    notificationState: NotificationSettingsUiState = NotificationSettingsUiState(),
    onNotificationIntent: (NotificationSettingsIntent) -> Unit = {},
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Times.route
    val scale = timesState.fontSizeMultiplier
    val isPrimaryTab = NavDestination.primaryTabs.any { it.route == currentRoute }

    // Kıble sensörü yalnızca Kıble sekmesi görünürken çalışsın (pil kuralı);
    // sekme değişince veya alt ekrana geçince dinleyici kapatılır.
    LaunchedEffect(currentRoute, timesState.city, timesState.latitude, timesState.longitude) {
        if (currentRoute == NavDestination.Qibla.route) {
            onQiblaIntent(
                QiblaIntent.StartCompass(
                    latitude = timesState.latitude,
                    longitude = timesState.longitude,
                    cityName = timesState.city
                )
            )
        } else {
            onQiblaIntent(QiblaIntent.StopCompass)
        }
    }

    val topBarTitle = when (currentRoute) {
        NavDestination.Imsakiye.route -> "${timesState.city} İmsakiyesi"
        NavDestination.Qibla.route -> "Kıble Pusulası"
        NavDestination.Tracker.route -> "Zikirmatik"
        else -> timesState.city
    }
    val topBarSubtitle = when (currentRoute) {
        NavDestination.Imsakiye.route -> turkishMonthLabel(imsakiyeState.yearMonth)
        NavDestination.Qibla.route -> "Kıble Yönü"
        NavDestination.Tracker.route -> "Zikir"
        else -> "Namaz Vakitleri"
    }

    Scaffold(
        topBar = {
            if (isPrimaryTab) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = topBarTitle,
                            fontSize = (20 * scale).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = topBarSubtitle,
                            fontSize = (12 * scale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (currentRoute == NavDestination.Times.route) {
                        IconButton(
                            onClick = { onTimesIntent(TimesIntent.ToggleSimpleMode) },
                            modifier = Modifier.testTag("toggle_simple_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = if (timesState.isSimpleMode) {
                                    "Standart Moda Geç"
                                } else {
                                    "Sade Moda Geç"
                                },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onTimesIntent(TimesIntent.UseLocation) },
                            modifier = Modifier.testTag("refresh_location_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Mevcut Konumu Bul",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
                TimesScreen(
                    state = timesState,
                    onIntent = onTimesIntent,
                    wrapInScaffold = false
                )
            }
            composable(NavDestination.Imsakiye.route) {
                com.anttech.prayertimes.features.calendar.ui.ImsakiyeScreen(
                    state = imsakiyeState,
                    onIntent = onImsakiyeIntent,
                    wrapInScaffold = false
                )
            }
            composable(NavDestination.Qibla.route) {
                QiblaScreen(
                    state = qiblaState,
                    onIntent = onQiblaIntent,
                    fontScale = scale,
                    wrapInScaffold = false
                )
            }
            composable(NavDestination.Tracker.route) {
                TrackerScreen(
                    state = trackerState,
                    onIntent = onTrackerIntent,
                    wrapInScaffold = false
                )
            }
            composable("sound_settings") {
                SoundSettingsScreen(
                    state = soundState,
                    onIntent = onSoundIntent,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    isSimpleMode = timesState.isSimpleMode,
                    onSimpleModeToggle = { onTimesIntent(TimesIntent.ToggleSimpleMode) },
                    onBackClick = { navController.popBackStack() },
                    onSoundSettingsClick = { navController.navigate("sound_settings") },
                    onNotificationsClick = { navController.navigate("notifications_settings") },
                    onCitySelectionClick = { navController.navigate("city_selection") },
                    onAboutClick = { navController.navigate("about") },
                    onPermissionsClick = { navController.navigate("permissions") }
                )
            }
            composable("notifications_settings") {
                val (hasPermission, requestPermission) = rememberNotificationPermission()
                val settingsContext = LocalContext.current
                NotificationSettingsScreen(
                    state = notificationState,
                    hasNotificationPermission = hasPermission,
                    onIntent = onNotificationIntent,
                    onBackClick = { navController.popBackStack() },
                    onRequestPermission = requestPermission,
                    onBatteryOptimizationClick = {
                        // Pil optimizasyonu listesini aç: kullanıcı Namazgâh'ı
                        // "Kısıtlanmamış" seçerse arka plan alarmları ve widget
                        // güncellemeleri OEM tarafından kesilmez.
                        runCatching {
                            settingsContext.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
            composable("permissions") {
                PermissionsRoute(
                    onBackClick = { navController.popBackStack() },
                    fontSizeMultiplier = scale
                )
            }
            composable("city_selection") {
                com.anttech.prayertimes.features.settings.ui.CitySelectionScreen(
                    currentCity = timesState.city,
                    onCitySelected = { city ->
                        onTimesIntent(TimesIntent.ChangeCity(city))
                        navController.popBackStack()
                    },
                    onUseLocation = {
                        onTimesIntent(TimesIntent.UseLocation)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("about") {
                com.anttech.prayertimes.features.settings.ui.AboutScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
private fun turkishMonthLabel(yearMonth: String): String {
    if (yearMonth.isBlank()) return "Aylık İmsakiye"
    return runCatching {
        YearMonth.parse(yearMonth)
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr", "TR")))
            .replaceFirstChar { it.titlecase(Locale("tr", "TR")) }
    }.getOrElse { yearMonth }
}
