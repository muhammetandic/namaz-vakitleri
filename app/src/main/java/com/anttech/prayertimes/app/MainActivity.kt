package com.anttech.prayertimes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.anttech.prayertimes.core.notifications.AdhanPlaybackService
import com.anttech.prayertimes.core.ui.AppUiCommand
import com.anttech.prayertimes.core.ui.AppUiEffect
import com.anttech.prayertimes.core.ui.UiEffectsCoordinator
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.settings.domain.AudioPlayerManager
import com.anttech.prayertimes.features.times.data.SmartSyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var smartSyncScheduler: SmartSyncScheduler

    @Inject
    lateinit var uiEffectsCoordinator: UiEffectsCoordinator

    @Inject
    lateinit var audioPlayerManager: AudioPlayerManager

    private val locationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted - location tracker will work
        // Permissions denied - location tracker returns null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        volumeControlStream = android.media.AudioManager.STREAM_ALARM

        smartSyncScheduler.scheduleDailyCheck()

        locationPermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

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
                    MainAppScaffold()
                }
            }
        }
    }

    /** Tek seferlik olaylar Activity yaşam döngüsüne bağlı olarak toplanır. */
    private fun collectUiEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiEffectsCoordinator.effects.collect { effect ->
                    when (effect) {
                        is AppUiEffect.Toast -> showToast(effect.message)
                        is AppUiEffect.ShareText -> shareText(effect.content)
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
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP
        ) {
            if (uiEffectsCoordinator.previewActive.value || audioPlayerManager.isPlaying()) {
                audioPlayerManager.stop()
                uiEffectsCoordinator.send(AppUiCommand.StopPreview)
                return true
            }
            val stopIntent = Intent(this, AdhanPlaybackService::class.java).apply {
                action = AdhanPlaybackService.ACTION_STOP
            }
            startService(stopIntent)
        }
        return super.onKeyDown(keyCode, event)
    }
}
