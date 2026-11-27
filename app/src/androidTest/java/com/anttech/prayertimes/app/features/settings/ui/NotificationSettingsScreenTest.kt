package com.anttech.prayertimes.features.settings.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        state: NotificationSettingsUiState = NotificationSettingsUiState(),
        hasPermission: Boolean = true,
        onIntent: (NotificationSettingsIntent) -> Unit = {},
        onRequestPermission: () -> Unit = {},
        onBatteryOptimizationClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            NamazVakitleriTheme {
                NotificationSettingsScreen(
                    state = state,
                    hasNotificationPermission = hasPermission,
                    onIntent = onIntent,
                    onBackClick = {},
                    onRequestPermission = onRequestPermission,
                    onBatteryOptimizationClick = onBatteryOptimizationClick
                )
            }
        }
    }

    @Test
    fun switches_reflectSavedPreferences() {
        setScreen(state = NotificationSettingsUiState(notificationsEnabled = true, vibrationEnabled = false))

        composeTestRule.onNodeWithTag("notification_enabled_switch").assertIsOn()
        composeTestRule.onNodeWithTag("notification_vibration_switch").assertIsOff()
    }

    @Test
    fun taps_onRows_fireToggles() {
        val intents = mutableListOf<NotificationSettingsIntent>()
        setScreen(onIntent = { intents += it })

        composeTestRule.onNodeWithTag("notification_enabled_row").performClick()
        composeTestRule.onNodeWithTag("notification_vibration_row").performClick()

        assertTrue(intents[0] == NotificationSettingsIntent.ToggleNotifications)
        assertTrue(intents[1] == NotificationSettingsIntent.ToggleVibration)
    }

    @Test
    fun permissionBanner_shownOnlyWhenPermissionMissing() {
        var requested = false
        setScreen(hasPermission = false, onRequestPermission = { requested = true })

        composeTestRule.onNodeWithTag("notification_permission_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("notification_permission_button").performClick()
        assertTrue(requested)

        composeTestRule.onNodeWithTag("notification_test_button").assertIsNotEnabled()
    }

    @Test
    fun noPermissionBanner_whenPermissionGranted() {
        setScreen(hasPermission = true)

        composeTestRule.onNodeWithTag("notification_permission_banner").assertDoesNotExist()
        composeTestRule.onNodeWithTag("notification_test_button").assertIsEnabled()
    }

    @Test
    fun testNotificationButton_firesIntent() {
        val intents = mutableListOf<NotificationSettingsIntent>()
        setScreen(onIntent = { intents += it })

        composeTestRule.onNodeWithTag("notification_test_button").performClick()

        assertTrue(intents.single() == NotificationSettingsIntent.SendTestNotification)
    }

    // @Test
// fun soundRow_opensSoundSettings() {
//     var opened = false
//     setScreen(onBatteryOptimizationClick = { opened = true })
//
//     composeTestRule.onNodeWithTag("notification_sound_row").performClick()
//
//     assertTrue(opened)
// }
}
