package com.anttech.prayertimes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeUiEffect
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeViewModel
import com.anttech.prayertimes.features.qibla.ui.QiblaViewModel
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsUiEffect
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsViewModel
import com.anttech.prayertimes.features.settings.ui.SoundSettingsUiEffect
import com.anttech.prayertimes.features.settings.ui.SoundSettingsViewModel
import com.anttech.prayertimes.features.times.data.SmartSyncScheduler
import com.anttech.prayertimes.features.times.ui.TimesIntent
import com.anttech.prayertimes.features.times.ui.TimesUiEffect
import com.anttech.prayertimes.features.times.ui.TimesViewModel
import com.anttech.prayertimes.features.tracker.ui.TrackerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val timesViewModel: TimesViewModel by viewModels()
    private val imsakiyeViewModel: ImsakiyeViewModel by viewModels()
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

        collectUiEffects()

        setContent {
            NamazVakitleriTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Konum yalnızca ilk açılışta bir kez çözülür.
                    LaunchedEffect(Unit) {
                        timesViewModel.sendIntent(TimesIntent.RefreshLocation)
                    }

                    // Saniyelik geri sayım yalnızca ekran görünürken çalışır.
                    LaunchedEffect(Unit) {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) {
                                delay(1_000L)
                                timesViewModel.sendIntent(TimesIntent.TickSecond)
                            }
                        }
                    }

                    // İmsakiye seçili şehri takip etsin (elle seçimde yeniden yüklensin).
                    // Yalnızca şehir değişimi dinlenir; saniyelik sayaç bu efekti tetiklemez.
                    val city by timesViewModel.uiState
                        .map { it.city }
                        .distinctUntilChanged()
                        .collectAsStateWithLifecycle(timesViewModel.uiState.value.city)
                    LaunchedEffect(city) {
                        val yearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM").format(java.time.LocalDate.now())
                        imsakiyeViewModel.sendIntent(com.anttech.prayertimes.features.calendar.ui.ImsakiyeIntent.LoadImsakiye(city, yearMonth))
                    }

                    MainAppScaffold(
                        timesStateFlow = timesViewModel.uiState,
                        onTimesIntent = timesViewModel::sendIntent,
                        imsakiyeStateFlow = imsakiyeViewModel.uiState,
                        onImsakiyeIntent = imsakiyeViewModel::sendIntent,
                        qiblaStateFlow = qiblaViewModel.uiState,
                        onQiblaIntent = qiblaViewModel::sendIntent,
                        trackerStateFlow = trackerViewModel.uiState,
                        onTrackerIntent = trackerViewModel::sendIntent,
                        soundStateFlow = soundSettingsViewModel.uiState,
                        onSoundIntent = soundSettingsViewModel::sendIntent,
                        notificationStateFlow = notificationSettingsViewModel.uiState,
                        onNotificationIntent = notificationSettingsViewModel::sendIntent
                    )
                }
            }
        }
    }

    /**
     * Tek seferlik olayları (toast, paylaşım) Activity yaşam döngüsüne bağlı olarak
     * toplar. Ekran görünür değilken biriken olaylar görünür olunca işlenir.
     */
    private fun collectUiEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    timesViewModel.effect.collect { effect ->
                        when (effect) {
                            is TimesUiEffect.ShowToast -> showToast(effect.message)
                        }
                    }
                }
                launch {
                    imsakiyeViewModel.effect.collect { effect ->
                        when (effect) {
                            is ImsakiyeUiEffect.ShowMessage -> showToast(effect.message)
                            is ImsakiyeUiEffect.ShareText -> shareText(effect.content)
                        }
                    }
                }
                launch {
                    soundSettingsViewModel.effect.collect { effect ->
                        when (effect) {
                            is SoundSettingsUiEffect.ShowToast -> showToast(effect.message)
                        }
                    }
                }
                launch {
                    notificationSettingsViewModel.effect.collect { effect ->
                        when (effect) {
                            is NotificationSettingsUiEffect.ShowToast -> showToast(effect.message)
                        }
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun shareText(content: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(sendIntent, "İmsakiyeyi paylaş"))
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            // Ses önizlemesi çalıyorsa durdur
            if (soundSettingsViewModel.uiState.value.currentlyPlayingSoundId != null) {
                soundSettingsViewModel.sendIntent(com.anttech.prayertimes.features.settings.ui.SoundSettingsIntent.StopPreview)
                return true
            }
            // Arka planda ezan çalıyorsa durdur
            val stopIntent = Intent(this, com.anttech.prayertimes.core.notifications.AdhanPlaybackService::class.java).apply {
                action = com.anttech.prayertimes.core.notifications.AdhanPlaybackService.ACTION_STOP
            }
            startService(stopIntent)
        }
        return super.onKeyDown(keyCode, event)
    }
}
