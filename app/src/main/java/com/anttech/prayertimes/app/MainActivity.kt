package com.anttech.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.qibla.ui.QiblaIntent
import com.anttech.prayertimes.features.qibla.ui.QiblaViewModel
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsViewModel
import com.anttech.prayertimes.features.settings.ui.SoundSettingsViewModel
import com.anttech.prayertimes.features.times.ui.TimesIntent
import com.anttech.prayertimes.features.times.data.SmartSyncScheduler
import com.anttech.prayertimes.features.times.ui.TimesViewModel
import javax.inject.Inject
import com.anttech.prayertimes.features.tracker.ui.TrackerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val timesViewModel: TimesViewModel by viewModels()
    private val imsakiyeViewModel: com.anttech.prayertimes.features.calendar.ui.ImsakiyeViewModel by viewModels()
    private val qiblaViewModel: QiblaViewModel by viewModels()
    private val trackerViewModel: TrackerViewModel by viewModels()
    private val soundSettingsViewModel: SoundSettingsViewModel by viewModels()
    private val notificationSettingsViewModel: NotificationSettingsViewModel by viewModels()

    @Inject
    lateinit var smartSyncScheduler: SmartSyncScheduler

    // Location permissions launcher
    private val locationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted - location tracker will work
        // Permissions denied - location tracker returns null
    }

    // Android 13+ bildirim izni (vakit alarmları için zorunlu)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sistem temasına uyumlu edge-to-edge: açık temada açık durum çubuğu
        val isSystemDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val barStyle = if (isSystemDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle
        )

        // Ses butonları alarm sesini yönetsin (ezan ve önizlemelerle senkron)
        volumeControlStream = android.media.AudioManager.STREAM_ALARM

        // Akıllı aylık senkronizasyon: günlük WorkManager kontrolü planla
        smartSyncScheduler.scheduleDailyCheck()

        // Request location permissions at startup
        locationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        // Request notification permission for prayer alarms (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            NamazVakitleriTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val timesState by timesViewModel.uiState.collectAsState()
                    val imsakiyeState by imsakiyeViewModel.uiState.collectAsState()
                    val qiblaState by qiblaViewModel.uiState.collectAsState()
                    val trackerState by trackerViewModel.uiState.collectAsState()
                    val soundState by soundSettingsViewModel.uiState.collectAsState()
                    val notificationState by notificationSettingsViewModel.uiState.collectAsState()

                    // Saniye saniye geri sayım yenileme döngüsü ve konum başlatma
                    LaunchedEffect(Unit) {
                        timesViewModel.sendIntent(TimesIntent.RefreshLocation)
                        while (true) {
                            delay(1000L)
                            timesViewModel.sendIntent(TimesIntent.TickSecond)
                        }
                    }

                    // İmsakiye seçili şehri takip etsin (elle seçimde yeniden yüklensin)
                    LaunchedEffect(timesState.city) {
                        val yearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM").format(java.time.LocalDate.now())
                        imsakiyeViewModel.sendIntent(com.anttech.prayertimes.features.calendar.ui.ImsakiyeIntent.LoadImsakiye(timesState.city, yearMonth))
                    }

                    MainAppScaffold(
                        timesState = timesState,
                        onTimesIntent = timesViewModel::sendIntent,
                        imsakiyeState = imsakiyeState,
                        onImsakiyeIntent = imsakiyeViewModel::sendIntent,
                        qiblaState = qiblaState,
                        onQiblaIntent = qiblaViewModel::sendIntent,
                        trackerState = trackerState,
                        onTrackerIntent = trackerViewModel::sendIntent,
                        soundState = soundState,
                        onSoundIntent = soundSettingsViewModel::sendIntent,
                        notificationState = notificationState,
                        onNotificationIntent = notificationSettingsViewModel::sendIntent
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            // Ses önizlemesi çalıyorsa durdur
            if (soundSettingsViewModel.uiState.value.currentlyPlayingSoundId != null) {
                soundSettingsViewModel.sendIntent(com.anttech.prayertimes.features.settings.ui.SoundSettingsIntent.StopPreview)
                return true
            }
            // Arka planda ezan çalıyorsa durdur
            val stopIntent = android.content.Intent(this, com.anttech.prayertimes.core.notifications.AdhanPlaybackService::class.java).apply {
                action = com.anttech.prayertimes.core.notifications.AdhanPlaybackService.ACTION_STOP
            }
            startService(stopIntent)
        }
        return super.onKeyDown(keyCode, event)
    }
}
