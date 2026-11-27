package com.anttech.prayertimes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeScreen
import com.anttech.prayertimes.features.calendar.ui.ImsakiyeUiState
import com.anttech.prayertimes.features.qibla.ui.QiblaIntent
import com.anttech.prayertimes.features.qibla.ui.QiblaScreen
import com.anttech.prayertimes.features.qibla.ui.QiblaUiState
import com.anttech.prayertimes.features.times.domain.CountdownResult
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import com.anttech.prayertimes.features.times.ui.TimesScreen
import com.anttech.prayertimes.features.times.ui.TimesUiState
import com.anttech.prayertimes.features.tracker.ui.TrackerScreen
import com.anttech.prayertimes.features.tracker.ui.TrackerUiState
import com.anttech.prayertimes.navigation.CompassLifecycle
import com.anttech.prayertimes.navigation.NavDestination
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

        composeTestRule.setContent {
            NamazVakitleriTheme {
                MainAppScaffold(
                    routeContent = { route, _, _ ->
                        FakeRoute(route, timesState, qiblaState, trackerState, mutableListOf())
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("nav_times").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_imsakiye").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_qibla").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tracker").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_imsakiye").performClick()
        composeTestRule.onNodeWithText("Aylık Vakit Çizelgesi").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_qibla").performClick()
        composeTestRule.onNodeWithTag("compass_dial").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tracker").performClick()
        composeTestRule.onNodeWithTag("dhikr_tap_area").assertIsDisplayed()

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
        val qiblaIntents = mutableListOf<QiblaIntent>()

        composeTestRule.setContent {
            NamazVakitleriTheme {
                MainAppScaffold(
                    routeContent = { route, _, _ ->
                        FakeRoute(route, timesState, qiblaState, trackerState, qiblaIntents)
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("nav_qibla").performClick()
        composeTestRule.onNodeWithTag("compass_dial").assertIsDisplayed()
        composeTestRule.runOnIdle { qiblaIntents.clear() }

        composeTestRule.onNodeWithTag("nav_times").performClick()
        composeTestRule.runOnIdle { }
        assertTrue(
            "Kıble sekmesi terk edilince StopCompass gönderilmeli. Alınan: $qiblaIntents",
            qiblaIntents.any { it is QiblaIntent.StopCompass }
        )
    }

    @Composable
    private fun FakeRoute(
        route: String,
        timesState: TimesUiState,
        qiblaState: QiblaUiState,
        trackerState: TrackerUiState,
        qiblaIntents: MutableList<QiblaIntent>
    ) {
        when (route) {
            NavDestination.Times.route -> TimesScreen(
                state = timesState,
                onIntent = {},
                wrapInScaffold = false
            )

            NavDestination.Imsakiye.route -> ImsakiyeScreen(
                state = ImsakiyeUiState(isLoading = false),
                onIntent = {},
                wrapInScaffold = false
            )

            NavDestination.Qibla.route -> {
                CompassLifecycle(
                    latitude = 41.0,
                    longitude = 29.0,
                    cityName = "İstanbul",
                    onIntent = { qiblaIntents.add(it) }
                )
                QiblaScreen(
                    state = qiblaState,
                    onIntent = {},
                    wrapInScaffold = false
                )
            }

            NavDestination.Tracker.route -> TrackerScreen(
                state = trackerState,
                onIntent = {},
                wrapInScaffold = false
            )

            else -> Text(route)
        }
    }
}
