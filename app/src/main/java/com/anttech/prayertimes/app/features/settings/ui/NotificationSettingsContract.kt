package com.anttech.prayertimes.features.settings.ui

data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val fontSizeMultiplier: Float = 1.0f
)

sealed interface NotificationSettingsIntent {
    data object ToggleNotifications : NotificationSettingsIntent
    data object ToggleVibration : NotificationSettingsIntent
    data object SendTestNotification : NotificationSettingsIntent
}

sealed interface NotificationSettingsUiEffect {
    data class ShowToast(val message: String) : NotificationSettingsUiEffect
}
