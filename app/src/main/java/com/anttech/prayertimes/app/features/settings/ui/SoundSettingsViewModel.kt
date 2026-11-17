package com.anttech.prayertimes.features.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.core.notifications.TestNotificationSender
import com.anttech.prayertimes.features.settings.domain.AudioPlayerManager
import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerType
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
class SoundSettingsViewModel @Inject constructor(
    private val repository: SoundPreferencesRepository,
    private val fastingAlarmPreferences: FastingAlarmPreferencesRepository,
    private val alarmScheduler: PrayerAlarmScheduler,
    private val audioPlayerManager: AudioPlayerManager,
    private val testNotificationSender: TestNotificationSender
) : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main

    private val _uiState = MutableStateFlow(SoundSettingsUiState())
    val uiState: StateFlow<SoundSettingsUiState> = _uiState.asStateFlow()

    private val _effect = Channel<SoundSettingsUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadSoundsForPrayers()
        loadDelaysForPrayers()
        loadGlobalVolume()
        loadSahurAlarmPreferences()
        loadSahurAlarmSound()
    }

    private fun loadSahurAlarmPreferences() {
        viewModelScope.launch(dispatcher) {
            fastingAlarmPreferences.sahurAlarmEnabled.collect { enabled ->
                _uiState.update { it.copy(sahurAlarmEnabled = enabled) }
            }
        }
        viewModelScope.launch(dispatcher) {
            fastingAlarmPreferences.sahurAlarmMinutesBefore.collect { minutes ->
                _uiState.update { it.copy(sahurAlarmMinutesBefore = minutes) }
            }
        }
    }

    private fun loadSahurAlarmSound() {
        viewModelScope.launch(dispatcher) {
            repository.getSahurAlarmSound().collect { sound ->
                _uiState.update { it.copy(sahurAlarmSound = sound) }
            }
        }
    }

    private fun loadGlobalVolume() {
        viewModelScope.launch(dispatcher) {
            repository.globalVolume.collect { volume ->
                _uiState.update { it.copy(globalVolume = volume) }
            }
        }
    }

    private fun loadDelaysForPrayers() {
        PrayerType.entries.forEach { prayerType ->
            viewModelScope.launch(dispatcher) {
                repository.getDelayMinutesForPrayer(prayerType).collect { minutes ->
                    _uiState.update { current ->
                        current.copy(prayerDelays = current.prayerDelays + (prayerType to minutes))
                    }
                }
            }
        }
    }

    private fun loadSoundsForPrayers() {
        PrayerType.values().forEach { prayerType ->
            viewModelScope.launch(dispatcher) {
                repository.getSoundForPrayer(prayerType).collect { soundOption ->
                    _uiState.update { current ->
                        val updatedMap = current.prayerSounds.toMutableMap()
                        updatedMap[prayerType] = soundOption
                        current.copy(prayerSounds = updatedMap)
                    }
                }
            }
        }
    }

    fun sendIntent(intent: SoundSettingsIntent) {
        when (intent) {
            is SoundSettingsIntent.SelectPrayerTab -> {
                _uiState.update { it.copy(selectedPrayerType = intent.prayerType) }
            }
            is SoundSettingsIntent.SelectSound -> {
                selectSound(intent.prayerType, intent.soundOption)
            }
            is SoundSettingsIntent.ApplySoundToAll -> {
                applySoundToAll(intent.soundOption)
            }
            is SoundSettingsIntent.PlayPreview -> {
                playPreview(intent.soundOption, intent.targetId)
            }
            is SoundSettingsIntent.StopPreview -> {
                stopPreview()
            }
            is SoundSettingsIntent.CustomAudioPicked -> {
                handleCustomAudioPicked(intent.prayerType, intent.uriString, intent.displayName)
            }
            is SoundSettingsIntent.SetGlobalVolume -> setGlobalVolume(intent.volume)
            is SoundSettingsIntent.SelectDelay -> selectDelay(intent.prayerType, intent.minutes)
            is SoundSettingsIntent.ToggleSahurAlarm -> toggleSahurAlarm()
            is SoundSettingsIntent.SetSahurAlarmMinutes -> setSahurAlarmMinutes(intent.minutes)
            is SoundSettingsIntent.SelectSahurAlarmSound -> selectSahurAlarmSound(intent.soundOption)
        }
    }

    private fun selectSahurAlarmSound(soundOption: SoundOption) {
        _uiState.update { it.copy(sahurAlarmSound = soundOption) }
        viewModelScope.launch(dispatcher) {
            repository.saveSahurAlarmSound(soundOption)
        }
    }

    private fun toggleSahurAlarm() {
        val enabled = !_uiState.value.sahurAlarmEnabled
        _uiState.update { it.copy(sahurAlarmEnabled = enabled) }
        viewModelScope.launch(dispatcher) {
            fastingAlarmPreferences.setSahurAlarmEnabled(enabled)
            // Alarmları yeni tercihe göre yeniden kur.
            alarmScheduler.rescheduleSaved()
            _effect.send(
                SoundSettingsUiEffect.ShowToast(
                    if (enabled) {
                        "Sahur uyandırma alarmı açıldı"
                    } else {
                        "Sahur uyandırma alarmı kapatıldı"
                    }
                )
            )
        }
    }

    private fun setSahurAlarmMinutes(minutes: Int) {
        _uiState.update { it.copy(sahurAlarmMinutesBefore = minutes) }
        viewModelScope.launch(dispatcher) {
            fastingAlarmPreferences.setSahurAlarmMinutesBefore(minutes)
            alarmScheduler.rescheduleSaved()
        }
    }

    private fun setGlobalVolume(volume: Float) {
        val coerced = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(globalVolume = coerced) }
        // Önizleme çalıyorsa seviye anında uygulanır.
        audioPlayerManager.setVolume(coerced)
        viewModelScope.launch(dispatcher) {
            repository.setGlobalVolume(coerced)
        }
    }

    private fun selectDelay(prayerType: PrayerType?, minutes: Int) {
        val targets = prayerType?.let { listOf(it) } ?: PrayerType.entries.toList()
        _uiState.update { current ->
            current.copy(prayerDelays = current.prayerDelays + targets.associateWith { minutes })
        }
        viewModelScope.launch(dispatcher) {
            targets.forEach { repository.saveDelayMinutesForPrayer(it, minutes) }
        }
    }

    private fun selectSound(prayerType: PrayerType, soundOption: SoundOption) {
        viewModelScope.launch(dispatcher) {
            repository.saveSoundForPrayer(prayerType, soundOption)
            _uiState.update { current ->
                val updated = current.prayerSounds.toMutableMap()
                updated[prayerType] = soundOption
                current.copy(prayerSounds = updated)
            }
        }
    }

    private fun applySoundToAll(soundOption: SoundOption) {
        _uiState.update { current ->
            val updated = current.prayerSounds.toMutableMap()
            PrayerType.values().forEach { updated[it] = soundOption }
            current.copy(prayerSounds = updated)
        }
        viewModelScope.launch(dispatcher) {
            PrayerType.values().forEach { prayerType ->
                repository.saveSoundForPrayer(prayerType, soundOption)
            }
        }
    }

    private fun playPreview(soundOption: SoundOption, targetId: String) {
        _uiState.update {
            it.copy(
                currentlyPlayingSoundId = soundOption.id,
                currentlyPlayingTargetId = targetId
            )
        }
        audioPlayerManager.play(soundOption, _uiState.value.globalVolume) {
            // Çalma kendiliğinden bitince buton yeniden "oynat" simgesine döner.
            viewModelScope.launch(dispatcher) { clearNowPlaying(targetId) }
        }
    }

    private fun clearNowPlaying(targetId: String) {
        _uiState.update { current ->
            if (current.currentlyPlayingTargetId != targetId) {
                current
            } else {
                current.copy(currentlyPlayingSoundId = null, currentlyPlayingTargetId = null)
            }
        }
    }

    private fun stopPreview() {
        audioPlayerManager.stop()
        _uiState.update {
            it.copy(currentlyPlayingSoundId = null, currentlyPlayingTargetId = null)
        }
    }

    private fun handleCustomAudioPicked(prayerType: PrayerType?, uriString: String, displayName: String) {
        val customOption = SoundOption.CustomFile(
            id = uriString,
            title = displayName,
            uriString = uriString
        )
        if (prayerType == null) {
            applySoundToAll(customOption)
        } else {
            selectSound(prayerType, customOption)
        }
    }

    fun sendSahurTestNotification() {
        testNotificationSender.sendSahurTest()
    }

    override fun onCleared() {
        audioPlayerManager.stop()
    }
}
