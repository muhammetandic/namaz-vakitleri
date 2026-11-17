package com.anttech.prayertimes.core.notifications

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.anttech.prayertimes.features.settings.ui.SoundSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Test activity to trigger sahur alarm notification from ADB.
 * Kullanım: adb shell am start -n com.anttech.prayertimes/com.anttech.prayertimes.core.notifications.TestSahurActivity
 */
@AndroidEntryPoint
class TestSahurActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[SoundSettingsViewModel::class.java]
        viewModel.sendSahurTestNotification()
        finish()
    }
}