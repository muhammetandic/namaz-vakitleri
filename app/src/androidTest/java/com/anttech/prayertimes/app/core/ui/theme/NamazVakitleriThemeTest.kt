package com.anttech.prayertimes.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Yönerge: tüm ekranlar açık ve koyu temayı desteklemeli.
 * Bu test, temanın darkTheme parametresinin gerçekten şema değiştirdiğini korur.
 */
class NamazVakitleriThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun darkThemeParameter_appliesDarkScheme() {
        var background: Color? = null
        composeTestRule.setContent {
            NamazVakitleriTheme(darkTheme = true) {
                background = MaterialTheme.colorScheme.background
            }
        }
        assertEquals(Color(0xFF161110), background) // BackgroundDark
    }

    @Test
    fun lightThemeParameter_appliesLightScheme() {
        var background: Color? = null
        composeTestRule.setContent {
            NamazVakitleriTheme(darkTheme = false) {
                background = MaterialTheme.colorScheme.background
            }
        }
        assertEquals(Color(0xFFFFF8F5), background) // BackgroundLight
    }
}
