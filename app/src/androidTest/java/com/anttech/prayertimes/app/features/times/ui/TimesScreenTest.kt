package com.anttech.prayertimes.features.times.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.times.domain.CountdownResult
import com.anttech.prayertimes.features.times.domain.KerahaResult
import com.anttech.prayertimes.features.times.domain.KerahaType
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimesScreenTest {

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
        remainingSeconds = 7200L,
        hours = 2,
        minutes = 0,
        seconds = 0,
        formattedCountdown = "02:00:00",
        progress = 0.35f
    )

    @Test
    fun standardMode_displaysAllPrayerNamesAndCountdown() {
        val state = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown,
            isSimpleMode = false
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TimesScreen(state = state, onIntent = {})
            }
        }

        // Verify city
        composeTestRule.onNodeWithText("İstanbul").assertIsDisplayed()

        // Verify countdown in standard mode
        composeTestRule.onNodeWithTag("standard_countdown_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("02:00:00").assertIsDisplayed()

        // Verify all 6 Turkish prayer names
        composeTestRule.onNodeWithText("İmsak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Güneş").assertIsDisplayed()
        composeTestRule.onNodeWithText("Öğle").assertIsDisplayed()
        composeTestRule.onNodeWithText("İkindi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Akşam").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yatsı").assertIsDisplayed()
    }

    /**
     * Regression: inside the app shell the Times screen only gets ~560dp of height.
     * All six prayer times must fit there, even while the kerahat warning is active.
     */
    @Test
    fun standardMode_allSixPrayerTimesFitInsideShellHeight() {
        val state = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown,
            keraha = KerahaResult(
                isKeraha = true,
                type = KerahaType.MIDDAY,
                minutesRemaining = 17
            ),
            isSimpleMode = false
        )

        val shellHeight = 560.dp

        composeTestRule.setContent {
            NamazVakitleriTheme {
                Box(modifier = Modifier.height(shellHeight)) {
                    TimesScreen(state = state, onIntent = {}, wrapInScaffold = false)
                }
            }
        }

        // Kerahat lives in the hero, with its remaining-minutes counter
        composeTestRule.onNodeWithTag("kerahat_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Öğle kerahati · 17 dk").assertIsDisplayed()

        val bottomLimit = with(composeTestRule.density) { shellHeight.toPx() }
        listOf("İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı").forEach { name ->
            val bounds = composeTestRule.onNodeWithText(name).fetchSemanticsNode().boundsInRoot
            assertTrue(
                "$name is clipped: bottom=${bounds.bottom} > $bottomLimit",
                bounds.bottom <= bottomLimit
            )
        }
    }

    @Test
    fun standardMode_hidesKerahatBadgeWhenNoKeraha() {
        val state = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown,
            keraha = KerahaResult(
                isKeraha = false,
                type = KerahaType.NONE
            ),
            isSimpleMode = false
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TimesScreen(state = state, onIntent = {}, wrapInScaffold = false)
            }
        }

        composeTestRule.onNodeWithTag("kerahat_badge").assertDoesNotExist()
    }

    @Test
    fun simpleMode_displaysGiantCountdownAndSimpleList() {        val state = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown,
            isSimpleMode = true // Simple mode for seniors
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TimesScreen(state = state, onIntent = {})
            }
        }

        // Verify giant countdown tag is displayed
        composeTestRule.onNodeWithTag("simple_countdown_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("İkindi Vaktine Kalan:").assertIsDisplayed()
    }

    @Test
    fun clickingToggleSimpleModeButton_firesToggleIntent() {
        var intentFired: TimesIntent? = null
        val state = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown,
            isSimpleMode = false
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TimesScreen(state = state, onIntent = { intentFired = it })
            }
        }

        composeTestRule.onNodeWithTag("toggle_simple_mode_button").performClick()
        assert(intentFired is TimesIntent.ToggleSimpleMode)
    }

    @Test
    fun clickingGpsButton_firesRefreshLocationIntent() {
        var intentFired: TimesIntent? = null
        val state = TimesUiState(
            isLoading = false,
            city = "İstanbul",
            prayerTimes = sampleTimes,
            countdown = sampleCountdown
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TimesScreen(state = state, onIntent = { intentFired = it })
            }
        }

        composeTestRule.onNodeWithTag("refresh_location_button").performClick()
        assert(intentFired is TimesIntent.RefreshLocation)
    }
}
