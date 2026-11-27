package com.anttech.prayertimes.features.settings.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CitySelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listsCitiesAndReportsSelection() {
        var selectedCity: String? = null

        composeTestRule.setContent {
            NamazVakitleriTheme {
                CitySelectionScreen(
                    currentCity = "İstanbul",
                    onCitySelected = { selectedCity = it },
                    onUseLocation = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("city_search").assertIsDisplayed()
        composeTestRule.onNodeWithTag("city_item_Ankara").performClick()
        composeTestRule.onNodeWithTag("save_selection").performClick()

        assertTrue(selectedCity == "Ankara")
    }

    @Test
    fun searchFiltersCityList() {
        composeTestRule.setContent {
            NamazVakitleriTheme {
                CitySelectionScreen(
                    currentCity = "İstanbul",
                    onCitySelected = {},
                    onUseLocation = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("city_search").performTextInput("izmir")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("city_item_İzmir").assertIsDisplayed()
        composeTestRule.onNodeWithTag("city_item_Ankara").assertDoesNotExist()
    }

    @Test
    fun locationButtonFiresCallback() {
        var useLocationClicked = false

        composeTestRule.setContent {
            NamazVakitleriTheme {
                CitySelectionScreen(
                    currentCity = "İstanbul",
                    onCitySelected = {},
                    onUseLocation = { useLocationClicked = true },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("use_location_button").performClick()

        assertTrue(useLocationClicked)
    }

    @Test
    fun selectingDistrictIncludesItInSavedCity() {
        var selectedCity: String? = null

        composeTestRule.setContent {
            NamazVakitleriTheme {
                CitySelectionScreen(
                    currentCity = "İstanbul",
                    onCitySelected = { selectedCity = it },
                    onUseLocation = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("city_item_Ankara").performClick()
        composeTestRule.onNodeWithTag("district_item_Çankaya").performClick()
        composeTestRule.onNodeWithTag("save_selection").performClick()

        assertTrue(selectedCity == "Ankara, Çankaya")
    }

    @Test
    fun tappingDifferentCityResetsStaleDistrictFromCurrentCity() {
        var selectedCity: String? = null

        composeTestRule.setContent {
            NamazVakitleriTheme {
                CitySelectionScreen(
                    currentCity = "Antalya, Kepez",
                    onCitySelected = { selectedCity = it },
                    onUseLocation = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("city_search").performTextInput("artvin")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("city_item_Artvin").performClick()
        composeTestRule.onNodeWithTag("save_selection").performClick()

        assertTrue(selectedCity == "Artvin")
    }
}
