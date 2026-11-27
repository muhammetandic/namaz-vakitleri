package com.anttech.prayertimes.features.settings.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.times.domain.PrayerType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SoundSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overviewListsAllTimesAndEveryPrayerWithoutScopeSwitch() {
        val state = SoundSettingsUiState(
            prayerSounds = PrayerType.entries.associateWith { SoundOption.EZAN_ANVER_DUMAN }
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                SoundSettingsScreen(state = state, onIntent = {})
            }
        }

        composeTestRule.onNodeWithTag("sound_target_ALL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tüm Vakitler").assertIsDisplayed()
        PrayerType.entries.forEach { prayerType ->
            composeTestRule.onNodeWithTag("sound_target_${prayerType.name}").assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("sound_scope_switch").assertDoesNotExist()
        composeTestRule.onNodeWithTag("sound_scope_info").assertDoesNotExist()
    }

    @Test
    fun selectingSoundForAllTimes_firesApplySoundToAllIntent() {
        val intents = mutableListOf<SoundSettingsIntent>()
        val state = SoundSettingsUiState(
            prayerSounds = PrayerType.entries.associateWith { SoundOption.EZAN_TURKIYE }
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                SoundSettingsScreen(state = state, onIntent = { intents += it })
            }
        }

        composeTestRule.onNodeWithTag("select_sound_ALL").performClick()
        composeTestRule.onNodeWithText("Seçilen ses tüm vakitlere uygulanır.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sound_item_${SoundOption.EZAN_ANVER_DUMAN.id}").performClick()

        composeTestRule.waitForIdle()
        assertTrue(intents.any { it is SoundSettingsIntent.ApplySoundToAll })
    }

    @Test
    fun selectingSoundForSinglePrayer_firesSelectSoundIntent() {
        val intents = mutableListOf<SoundSettingsIntent>()
        val state = SoundSettingsUiState(
            prayerSounds = PrayerType.entries.associateWith { SoundOption.EZAN_TURKIYE }
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                SoundSettingsScreen(state = state, onIntent = { intents += it })
            }
        }

        composeTestRule.onNodeWithTag("select_sound_OGLE").performClick()
        composeTestRule.onNodeWithText("Öğle vakti için ses seçin.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sound_item_${SoundOption.EZAN_ANVER_DUMAN.id}").performClick()

        composeTestRule.waitForIdle()
        assertTrue(
            intents.any {
                it is SoundSettingsIntent.SelectSound && it.prayerType == PrayerType.OGLE
            }
        )
    }

    @Test
    fun openingSoundPicker_stopsPlayingPreview() {
        val intents = mutableListOf<SoundSettingsIntent>()
        val state = SoundSettingsUiState(
            prayerSounds = PrayerType.entries.associateWith { SoundOption.EZAN_ANVER_DUMAN }
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                SoundSettingsScreen(state = state, onIntent = { intents += it })
            }
        }

        composeTestRule.onAllNodesWithContentDescription("Sesi Dinle")[0].performClick()
        composeTestRule.waitForIdle()
        assertTrue(intents.any { it is SoundSettingsIntent.PlayPreview })

        composeTestRule.onNodeWithTag("select_sound_OGLE").performClick()
        composeTestRule.waitForIdle()

        assertTrue(intents.contains(SoundSettingsIntent.StopPreview))
    }

    @Test
    fun leavingScreen_firesStopPreviewIntent() {
        val intents = mutableListOf<SoundSettingsIntent>()
        val isScreenVisible = mutableStateOf(true)
        val state = SoundSettingsUiState(
            prayerSounds = PrayerType.entries.associateWith { SoundOption.EZAN_ANVER_DUMAN }
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                if (isScreenVisible.value) {
                    SoundSettingsScreen(state = state, onIntent = { intents += it })
                }
            }
        }

        composeTestRule.runOnIdle { isScreenVisible.value = false }
        composeTestRule.waitForIdle()

        assertTrue(intents.contains(SoundSettingsIntent.StopPreview))
    }
}
