package com.anttech.prayertimes.features.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.core.notifications.TestNotificationSender
import com.anttech.prayertimes.features.settings.domain.NotificationPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val preferences: NotificationPreferencesRepository,
    private val prayerAlarmScheduler: PrayerAlarmScheduler,
    private val testNotificationSender: TestNotificationSender
) : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<NotificationSettingsUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch(dispatcher) {
            preferences.notificationsEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch(dispatcher) {
            preferences.vibrationEnabled.collect { enabled ->
                _uiState.update { it.copy(vibrationEnabled = enabled) }
            }
        }
    }

    fun sendIntent(intent: NotificationSettingsIntent) {
        when (intent) {
            NotificationSettingsIntent.ToggleNotifications -> toggleNotifications()
            NotificationSettingsIntent.ToggleVibration -> toggleVibration()
            NotificationSettingsIntent.SendTestNotification -> sendTestNotification()
        }
    }

    private fun toggleNotifications() {
        val enabled = !_uiState.value.notificationsEnabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch(dispatcher) {
            preferences.setNotificationsEnabled(enabled)
            // Kapatınca da açınca da yeniden değerlendir: bildirim kapalı ama
            // ana ekranda widget varsa alarmlar widget tazelemesi için kalır.
            prayerAlarmScheduler.rescheduleSaved()
            _effect.send(
                NotificationSettingsUiEffect.ShowToast(
                    if (enabled) "Vakit bildirimleri açıldı" else "Vakit bildirimleri kapatıldı"
                )
            )
        }
    }

    private fun toggleVibration() {
        val enabled = !_uiState.value.vibrationEnabled
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        viewModelScope.launch(dispatcher) {
            preferences.setVibrationEnabled(enabled)
        }
    }

    private fun sendTestNotification() {
        testNotificationSender.send()
        viewModelScope.launch(dispatcher) {
            _effect.send(NotificationSettingsUiEffect.ShowToast("Test bildirimi gönderildi"))
        }
    }
}
