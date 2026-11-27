package com.anttech.prayertimes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeUiState
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.ui.NotificationSettingsUiState
import com.anttech.prayertimes.features.settings.ui.SoundSettingsUiState
import com.anttech.prayertimes.features.times.domain.CountdownResult
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.features.times.ui.TimesUiState
import com.anttech.prayertimes.features.qibla.ui.QiblaIntent
import com.anttech.prayertimes.features.qibla.ui.QiblaUiState
import com.anttech.prayertimes.features.tracker.ui.TrackerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MainNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTimes = PrayerTimes(
        city = "İstanbul",
        date = LocalDate.of(2026, 11, 5),
        imsak = LocalTime.of(5, 45),
        gunes = LocalTime.of(7, 10),
        ogle = LocalTime.of(12, 50),
        ikindi = LocalTime.of(15, 40),
        aksam = LocalTime.of(18, 5),
        yatsi = LocalTime.of(19, 25)
    )

    private val sampleCountdown = CountdownResult(
        activePrayer = PrayerType.OGLE,
        nextPrayer = PrayerType.IKINDI,
        remainingSeconds = 3600L,
        hours = 1,
        minutes = 0,
        seconds = 0,
        formattedCountdown = "01:00:00",
        progress = 0.5f
    )

    @Test
    fun bottomNavigation_displaysAllTabsAndAllowsSwitching() {
        val timesState = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown
        )
        val qiblaState = QiblaUiState(isLoading = false, cityName = "İstanbul", qiblaBearing = 152f)
        val trackerState = TrackerUiState()
        val soundState = SoundSettingsUiState(
            prayerSounds = mapOf(PrayerType.IMSAK to SoundOption.BuiltIn("saba", "Saba", "saba"))
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                MainAppScaffold(
                    timesStateFlow = MutableStateFlow(timesState),
                    onTimesIntent = {},
                    imsakiyeStateFlow = MutableStateFlow(ImsakiyeUiState(isLoading = false)),
                    onImsakiyeIntent = {},
                    qiblaStateFlow = MutableStateFlow(qiblaState),
                    onQiblaIntent = {},
                    trackerStateFlow = MutableStateFlow(trackerState),
                    onTrackerIntent = {},
                    soundStateFlow = MutableStateFlow(soundState),
                    onSoundIntent = {},
                    notificationStateFlow = MutableStateFlow(NotificationSettingsUiState()),
                    onNotificationIntent = {}
                )
            }
        }

        // 1. Verify bottom navigation items are displayed
        composeTestRule.onNodeWithTag("nav_times").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_imsakiye").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_qibla").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tracker").assertIsDisplayed()

        // 2. Click on İmsakiye tab
        composeTestRule.onNodeWithTag("nav_imsakiye").performClick()
        composeTestRule.onNodeWithText("Aylık Vakit Çizelgesi").assertIsDisplayed()

        // 3. Click on Kıble tab
        composeTestRule.onNodeWithTag("nav_qibla").performClick()
        composeTestRule.onNodeWithTag("compass_dial").assertIsDisplayed()

        // 4. Click on Zikir tab
        composeTestRule.onNodeWithTag("nav_tracker").performClick()
        composeTestRule.onNodeWithTag("dhikr_tap_area").assertIsDisplayed()

        // 5. Click back on Vakitler tab
        composeTestRule.onNodeWithTag("nav_times").performClick()
        composeTestRule.onNodeWithTag("standard_countdown_text").assertIsDisplayed()
    }

    @Test
    fun leavingQiblaTab_stopsCompassSensor() {
        val timesState = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown
        )
        val qiblaState = QiblaUiState(isLoading = false, cityName = "İstanbul", qiblaBearing = 152f)
        val trackerState = TrackerUiState()
        val soundState = SoundSettingsUiState(
            prayerSounds = mapOf(PrayerType.IMSAK to SoundOption.BuiltIn("saba", "Saba", "saba"))
        )
        val qiblaIntents = mutableListOf<QiblaIntent>()

        composeTestRule.setContent {
            NamazVakitleriTheme {
                MainAppScaffold(
                    timesStateFlow = MutableStateFlow(timesState),
                    onTimesIntent = {},
                    imsakiyeStateFlow = MutableStateFlow(ImsakiyeUiState(isLoading = false)),
                    onImsakiyeIntent = {},
                    qiblaStateFlow = MutableStateFlow(qiblaState),
                    onQiblaIntent = { qiblaIntents.add(it) },
                    trackerStateFlow = MutableStateFlow(trackerState),
                    onTrackerIntent = {},
                    soundStateFlow = MutableStateFlow(soundState),
                    onSoundIntent = {},
                    notificationStateFlow = MutableStateFlow(NotificationSettingsUiState()),
                    onNotificationIntent = {}
                )
            }
        }

        // Kıble sekmesine gir: pusula sensörü başlamalı
        composeTestRule.onNodeWithTag("nav_qibla").performClick()
        composeTestRule.onNodeWithTag("compass_dial").assertIsDisplayed()
        composeTestRule.runOnIdle { qiblaIntents.clear() }

        // Vakitler sekmesine dön: sensör dinleyicisi kapatılmalı (pil kuralı)
        composeTestRule.onNodeWithTag("nav_times").performClick()
        composeTestRule.runOnIdle { }
        assertTrue(
            "Kıble sekmesi terk edilince StopCompass gönderilmeli. Alınan: $qiblaIntents",
            qiblaIntents.any { it is QiblaIntent.StopCompass }
        )
    }
}
