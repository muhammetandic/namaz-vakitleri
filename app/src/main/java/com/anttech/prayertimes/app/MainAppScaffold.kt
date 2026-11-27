package com.anttech.prayertimes

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.provider.Settings
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeIntent
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeScreen
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeUiState
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    timesStateFlow: StateFlow<TimesUiState>,
    onTimesIntent: (TimesIntent) -> Unit,
    imsakiyeStateFlow: StateFlow<ImsakiyeUiState>,
    onImsakiyeIntent: (ImsakiyeIntent) -> Unit,
    qiblaStateFlow: StateFlow<QiblaUiState>,
    onQiblaIntent: (QiblaIntent) -> Unit,
    trackerStateFlow: StateFlow<TrackerUiState>,
    onTrackerIntent: (TrackerIntent) -> Unit,
    soundStateFlow: StateFlow<SoundSettingsUiState>,
    onSoundIntent: (SoundSettingsIntent) -> Unit,
    notificationStateFlow: StateFlow<NotificationSettingsUiState>,
    onNotificationIntent: (NotificationSettingsIntent) -> Unit,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Times.route

    // Yalnızca üst/alt çubuk için gereken küçük alanlar toplanır; saniyelik
    // geri sayım güncellemeleri tüm iskeleti yeniden çizmez.
    val city by timesStateFlow
        .map { it.city }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(timesStateFlow.value.city)
    val scale by timesStateFlow
        .map { it.fontSizeMultiplier }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(timesStateFlow.value.fontSizeMultiplier)
    val isSimpleMode by timesStateFlow
        .map { it.isSimpleMode }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(timesStateFlow.value.isSimpleMode)
    val imsakiyeYearMonth by imsakiyeStateFlow
        .map { it.yearMonth }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(imsakiyeStateFlow.value.yearMonth)

    val isPrimaryTab = NavDestination.primaryTabs.any { it.route == currentRoute }

    val topBarTitle = when (currentRoute) {
        NavDestination.Imsakiye.route -> "$city İmsakiyesi"
        NavDestination.Qibla.route -> "Kıble Pusulası"
        NavDestination.Tracker.route -> "Zikirmatik"
        else -> city
    }
    val topBarSubtitle = when (currentRoute) {
        NavDestination.Imsakiye.route -> turkishMonthLabel(imsakiyeYearMonth)
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
                                contentDescription = if (isSimpleMode) {
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
                    if (currentRoute == NavDestination.Imsakiye.route) {
                        IconButton(
                            onClick = { onImsakiyeIntent(ImsakiyeIntent.ShareTodayImsakiye) },
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
                                onImsakiyeIntent(
                                    ImsakiyeIntent.LoadImsakiye(
                                        city = timesStateFlow.value.city,
                                        yearMonth = imsakiyeStateFlow.value.yearMonth
                                    )
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
                val state by timesStateFlow.collectAsStateWithLifecycle()
                TimesScreen(
                    state = state,
                    onIntent = onTimesIntent,
                    wrapInScaffold = false
                )
            }
            composable(NavDestination.Imsakiye.route) {
                val state by imsakiyeStateFlow.collectAsStateWithLifecycle()
                ImsakiyeScreen(
                    state = state.copy(fontSizeMultiplier = scale),
                    onIntent = onImsakiyeIntent,
                    wrapInScaffold = false
                )
            }
            composable(NavDestination.Qibla.route) {
                val state by qiblaStateFlow.collectAsStateWithLifecycle()
                val timesState by timesStateFlow.collectAsStateWithLifecycle()

                // Sensör yalnızca Kıble sekmesi görünürken çalışır; sekmeden
                // çıkınca dinleyici kapatılır (pil kuralı).
                LaunchedEffect(timesState.city, timesState.latitude, timesState.longitude) {
                    onQiblaIntent(
                        QiblaIntent.StartCompass(
                            latitude = timesState.latitude,
                            longitude = timesState.longitude,
                            cityName = timesState.city
                        )
                    )
                }
                DisposableEffect(Unit) {
                    onDispose { onQiblaIntent(QiblaIntent.StopCompass) }
                }

                QiblaScreen(
                    state = state,
                    onIntent = onQiblaIntent,
                    fontScale = scale,
                    wrapInScaffold = false
                )
            }
            composable(NavDestination.Tracker.route) {
                val state by trackerStateFlow.collectAsStateWithLifecycle()
                TrackerScreen(
                    state = state.copy(fontSizeMultiplier = scale),
                    onIntent = onTrackerIntent,
                    wrapInScaffold = false
                )
            }
            composable("sound_settings") {
                val state by soundStateFlow.collectAsStateWithLifecycle()
                SoundSettingsScreen(
                    state = state.copy(fontSizeMultiplier = scale),
                    onIntent = onSoundIntent,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    isSimpleMode = isSimpleMode,
                    fontScale = scale,
                    onSimpleModeToggle = { onTimesIntent(TimesIntent.ToggleSimpleMode) },
                    onFontScaleChange = { onTimesIntent(TimesIntent.SetFontScale(it)) },
                    onBackClick = { navController.popBackStack() },
                    onSoundSettingsClick = { navController.navigate("sound_settings") },
                    onNotificationsClick = { navController.navigate("notifications_settings") },
                    onCitySelectionClick = { navController.navigate("city_selection") },
                    onAboutClick = { navController.navigate("about") },
                    onPermissionsClick = { navController.navigate("permissions") }
                )
            }
            composable("notifications_settings") {
                val state by notificationStateFlow.collectAsStateWithLifecycle()
                val (hasPermission, requestPermission) = rememberNotificationPermission()
                val settingsContext = LocalContext.current
                NotificationSettingsScreen(
                    state = state.copy(fontSizeMultiplier = scale),
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
                    currentCity = city,
                    onCitySelected = { selectedCity ->
                        onTimesIntent(TimesIntent.ChangeCity(selectedCity))
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
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("tr-TR")))
            .replaceFirstChar { it.titlecase(Locale.forLanguageTag("tr-TR")) }
    }.getOrElse { yearMonth }
}
