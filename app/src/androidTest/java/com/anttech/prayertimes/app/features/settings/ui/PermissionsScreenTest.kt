package com.anttech.prayertimes.features.settings.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.anttech.prayertimes.core.ui.theme.NamazVakitleriTheme
import org.junit.Rule
import org.junit.Test

class PermissionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        notificationsGranted: Boolean = true,
        locationGranted: Boolean = true,
        exactAlarmsGranted: Boolean = true,
        onRequestNotifications: () -> Unit = {},
        onRequestLocation: () -> Unit = {},
        onOpenExactAlarmSettings: () -> Unit = {},
        onOpenAppSettings: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            NamazVakitleriTheme {
                PermissionsScreen(
                    notificationsGranted = notificationsGranted,
                    locationGranted = locationGranted,
                    exactAlarmsGranted = exactAlarmsGranted,
                    onRequestNotifications = onRequestNotifications,
                    onRequestLocation = onRequestLocation,
                    onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onBackClick = {}
                )
            }
        }
    }

    @Test
    fun rows_showGrantedAndMissingStatuses() {
        setScreen(notificationsGranted = true, locationGranted = false, exactAlarmsGranted = false)

        composeTestRule.onNodeWithTag("permission_notifications_status", useUnmergedTree = true)
            .assertTextEquals("Açık")
        composeTestRule.onNodeWithTag("permission_location_status", useUnmergedTree = true)
            .assertTextEquals("Gerekli")
        composeTestRule.onNodeWithTag("permission_exact_alarm_status", useUnmergedTree = true)
            .assertTextEquals("Gerekli")
    }

    @Test
    fun rows_areAllVisible() {
        setScreen()

        composeTestRule.onNodeWithTag("permission_notifications_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("permission_location_row").assertIsDisplayed()
        composeTestRule.onNodeWithTag("permission_exact_alarm_row").assertIsDisplayed()
    }

    @Test
    fun notificationAction_requestsPermission() {
        var requested = false
        setScreen(notificationsGranted = false, onRequestNotifications = { requested = true })

        composeTestRule.onNodeWithTag("permission_notifications_row_action").performClick()

        assert(requested)
    }

    @Test
    fun locationAction_requestsPermission() {
        var requested = false
        setScreen(locationGranted = false, onRequestLocation = { requested = true })

        composeTestRule.onNodeWithTag("permission_location_row_action").performClick()

        assert(requested)
    }

    @Test
    fun exactAlarmAction_opensSystemSettings() {
        var opened = false
        setScreen(exactAlarmsGranted = false, onOpenExactAlarmSettings = { opened = true })

        composeTestRule.onNodeWithTag("permission_exact_alarm_row_action").performClick()

        assert(opened)
    }

    @Test
    fun appSettingsRow_opensAppSettings() {
        var opened = false
        setScreen(onOpenAppSettings = { opened = true })

        composeTestRule.onNodeWithTag("permission_open_app_settings").performClick()

        assert(opened)
    }
}
