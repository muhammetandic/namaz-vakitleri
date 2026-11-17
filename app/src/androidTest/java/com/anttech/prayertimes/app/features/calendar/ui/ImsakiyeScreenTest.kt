package com.anttech.prayertimes.features.calendar.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import com.anttech.prayertimes.features.calendar.domain.ImsakiyeDay
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ImsakiyeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleDay = ImsakiyeDay(
        date = LocalDate.of(2026, 11, 5),
        imsakFormatted = "05:45",
        gunesFormatted = "07:10",
        ogleFormatted = "12:50",
        ikindiFormatted = "15:40",
        aksamFormatted = "18:05",
        yatsiFormatted = "19:25",
        isToday = true,
        hijriDay = 15,
        hijriMonth = 3,
        hijriMonthName = "Rebiülevvel",
        hijriYear = 1448
    )

    @Test
    fun displaysTodayImsakiyeCardAndIftarPrayer() {
        val state = ImsakiyeUiState(
            isLoading = false,
            city = "İstanbul",
            todayImsakiye = sampleDay,
            days = listOf(sampleDay)
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                ImsakiyeScreen(state = state, onIntent = {})
            }
        }

        // Verify city title
        composeTestRule.onNodeWithText("İstanbul İmsakiyesi").assertIsDisplayed()

        // Verify Today card
        composeTestRule.onNodeWithTag("today_imsakiye_card").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("05:45").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("18:05").onFirst().assertIsDisplayed()

        // Verify Iftar prayer text
        composeTestRule.onNodeWithText("İftar Duası").assertIsDisplayed()

        // Verify Hijri date is shown in today card
        composeTestRule.onNodeWithText("Hicri: 15 Rebiülevvel 1448").assertIsDisplayed()
    }

    @Test
    fun clickingTodayCardToggle_firesToggleFastedDayIntentWithHijriData() {
        var intentFired: ImsakiyeIntent? = null
        val state = ImsakiyeUiState(
            isLoading = false,
            city = "İstanbul",
            todayImsakiye = sampleDay,
            days = listOf(sampleDay)
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                ImsakiyeScreen(state = state, onIntent = { intentFired = it })
            }
        }

        composeTestRule.onNodeWithTag("today_fasted_toggle").assertHasClickAction().performClick()
        val intent = intentFired
        assert(intent is ImsakiyeIntent.ToggleFastedDay)
        assert((intent as ImsakiyeIntent.ToggleFastedDay).date == sampleDay.date)
        assert(intent.hijriDay == 15)
        assert(intent.hijriMonth == 3)
        assert(intent.hijriMonthName == "Rebiülevvel")
        assert(intent.hijriYear == 1448)
    }

    @Test
    fun imsakiyeRows_removed_noMonthlyTable() {
        val state = ImsakiyeUiState(
            isLoading = false,
            city = "İstanbul",
            todayImsakiye = sampleDay,
            days = listOf(sampleDay)
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                ImsakiyeScreen(state = state, onIntent = {})
            }
        }

        // Monthly table rows should not exist anymore
        composeTestRule.onNodeWithTag("imsakiye_row_5").assertDoesNotExist()
        composeTestRule.onNodeWithText("Aylık Vakit Çizelgesi").assertDoesNotExist()
    }

    @Test
    fun clickingShareButton_firesShareTodayImsakiyeIntent() {
        var intentFired: ImsakiyeIntent? = null
        val state = ImsakiyeUiState(
            isLoading = false,
            city = "İstanbul",
            todayImsakiye = sampleDay
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                ImsakiyeScreen(state = state, onIntent = { intentFired = it })
            }
        }

        composeTestRule.onNodeWithTag("share_imsakiye_button").performClick()
        assert(intentFired is ImsakiyeIntent.ShareTodayImsakiye)
    }
}
