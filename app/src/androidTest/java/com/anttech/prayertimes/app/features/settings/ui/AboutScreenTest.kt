package com.anttech.prayertimes.features.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import org.junit.Rule
import org.junit.Test

class AboutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsAppNameVersionAndDataSources() {
        composeTestRule.setContent {
            NamazVakitleriTheme {
                AboutScreen(onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText("Namazgâh").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sürüm 1.0.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Veri Kaynağı").assertIsDisplayed()
        composeTestRule.onNodeWithText("Küçük ve büyük ana ekran widget'ları, canlı geri sayım").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_version").assertIsDisplayed()
    }

    @Test
    fun showsVersionDetailsAndContactEmail() {
        composeTestRule.setContent {
            NamazVakitleriTheme {
                AboutScreen(onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText("Uygulama Bilgisi").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_build").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_package").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_contact_email").assertIsDisplayed()
        composeTestRule.onNodeWithText("muhammet.andic@gmail.com").assertIsDisplayed()
    }
}
