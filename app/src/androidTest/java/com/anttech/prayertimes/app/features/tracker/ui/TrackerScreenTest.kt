package com.anttech.prayertimes.features.tracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import org.junit.Rule
import org.junit.Test

class TrackerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dhikrTab_displaysPresetCountAndTapArea() {
        val state = TrackerUiState(
            dhikrCount = 12,
            dhikrLaps = 1
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TrackerScreen(state = state, onIntent = {})
            }
        }

        // Verify count
        composeTestRule.onNodeWithTag("dhikr_count_text", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("12").assertIsDisplayed()

        // Verify tap area
        composeTestRule.onNodeWithTag("dhikr_tap_area").assertIsDisplayed()
    }

    @Test
    fun dhikrTab_hero_showsSelectedDhikrLapAndProgress() {
        val state = TrackerUiState(dhikrCount = 12, dhikrLaps = 1)

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TrackerScreen(state = state, onIntent = {})
            }
        }

        composeTestRule.onNodeWithText("SEÇİLİ ZİKİR").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tur 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 / 33").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dhikr_arabic_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hedef: 33").assertIsDisplayed()
    }

    @Test
    fun dhikrTab_tappingArea_firesIncrementDhikrIntent() {
        var intentFired: TrackerIntent? = null
        val state = TrackerUiState()

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TrackerScreen(state = state, onIntent = { intentFired = it })
            }
        }

        composeTestRule.onNodeWithTag("dhikr_tap_area").performClick()
        assert(intentFired is TrackerIntent.IncrementDhikr)
    }

    @Test
    fun dhikrTab_showsTargetProgressAndLapCountInHero() {
        val state = TrackerUiState(dhikrCount = 12, dhikrLaps = 3)

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TrackerScreen(state = state, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag("dhikr_hero").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dhikr_progress_text", useUnmergedTree = true)
            .assertTextEquals("12 / 33")
        composeTestRule.onNodeWithTag("dhikr_laps_text", useUnmergedTree = true)
            .assertTextEquals("Tur 3")
    }

    @Test
    fun dhikrTab_resetButton_firesResetDhikrIntent() {
        var intentFired: TrackerIntent? = null
        val state = TrackerUiState()

        composeTestRule.setContent {
            NamazVakitleriTheme {
                TrackerScreen(state = state, onIntent = { intentFired = it })
            }
        }

        composeTestRule.onNodeWithTag("dhikr_reset_button").performClick()
        assert(intentFired is TrackerIntent.ResetDhikr)
    }
}
