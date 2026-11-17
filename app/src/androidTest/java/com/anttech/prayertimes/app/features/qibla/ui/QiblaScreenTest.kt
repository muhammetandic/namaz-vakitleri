package com.anttech.prayertimes.features.qibla.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import org.junit.Rule
import org.junit.Test

class QiblaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCityNameAndQiblaBearing() {
        val state = QiblaUiState(
            isLoading = false,
            cityName = "İstanbul",
            qiblaBearing = 152f,
            currentAzimuth = 90f,
            needleAngle = 62f,
            isAligned = false
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                QiblaScreen(state = state, onIntent = {})
            }
        }

        // Verify city name and bearing
        composeTestRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeTestRule.onNodeWithTag("qibla_bearing_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kıble Açısı: 152°").assertIsDisplayed()

        // Verify compass dial
        composeTestRule.onNodeWithTag("compass_dial").assertIsDisplayed()
    }

    @Test
    fun whenAlignedWithQibla_showsAlignedIndicator() {
        val state = QiblaUiState(
            isLoading = false,
            cityName = "İstanbul",
            qiblaBearing = 152f,
            currentAzimuth = 152f,
            needleAngle = 0f,
            isAligned = true
        )

        composeTestRule.setContent {
            NamazVakitleriTheme {
                QiblaScreen(state = state, onIntent = {})
            }
        }

        // Verify aligned indicator
        composeTestRule.onNodeWithTag("qibla_aligned_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kıbleye Yöneldiniz").assertIsDisplayed()
    }
}
