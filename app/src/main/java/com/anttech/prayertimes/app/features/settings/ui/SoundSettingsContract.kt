package com.anttech.prayertimes.features.settings.ui

import com.anttech.prayertimes.features.settings.domain.FastingAlarmPreferencesRepository
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerType

data class SoundSettingsUiState(
    val isLoading: Boolean = false,
    val selectedPrayerType: PrayerType = PrayerType.IMSAK,
    val prayerSounds: Map<PrayerType, SoundOption> = emptyMap(),
    val prayerDelays: Map<PrayerType, Int> = emptyMap(),
    val globalVolume: Float = SoundPreferencesRepository.DEFAULT_VOLUME,
    val delayOptionsMinutes: List<Int> = SoundPreferencesRepository.DELAY_OPTIONS_MINUTES,
    val availableBuiltInSounds: List<SoundOption> = SoundOption.DEFAULT_BUILT_IN_SOUNDS,
    val currentlyPlayingSoundId: String? = null,
    val currentlyPlayingTargetId: String? = null,
    val sahurAlarmEnabled: Boolean = FastingAlarmPreferencesRepository.DEFAULT_ENABLED,
    val sahurAlarmMinutesBefore: Int = FastingAlarmPreferencesRepository.DEFAULT_MINUTES_BEFORE,
    val sahurAlarmMinuteOptions: List<Int> = FastingAlarmPreferencesRepository.MINUTE_OPTIONS,
    val sahurAlarmSound: SoundOption? = null,
    val fontSizeMultiplier: Float = 1.0f
)

sealed interface SoundSettingsIntent {
    data class SelectPrayerTab(val prayerType: PrayerType) : SoundSettingsIntent
    data class SelectSound(val prayerType: PrayerType, val soundOption: SoundOption) : SoundSettingsIntent
    data class ApplySoundToAll(val soundOption: SoundOption) : SoundSettingsIntent
    data class PlayPreview(val soundOption: SoundOption, val targetId: String = soundOption.id) : SoundSettingsIntent
    object StopPreview : SoundSettingsIntent
    data class CustomAudioPicked(
        val prayerType: PrayerType?,
        val uriString: String,
        val displayName: String
    ) : SoundSettingsIntent

    /** Uygulama içi genel ses seviyesi (0f..1f); sistem sesinden bağımsızdır. */
    data class SetGlobalVolume(val volume: Float) : SoundSettingsIntent

    /** Vakit sesi gecikmesi. [prayerType] null ise tüm vakitlere uygulanır. */
    data class SelectDelay(val prayerType: PrayerType?, val minutes: Int) : SoundSettingsIntent

    /** Sahur uyandırma alarmını aç/kapat. */
    data object ToggleSahurAlarm : SoundSettingsIntent

    /** Sahur alarmının imsaktan kaç dakika önce kurulacağı. */
    data class SetSahurAlarmMinutes(val minutes: Int) : SoundSettingsIntent

    /** Sahur alarmı için ses seçimi. */
    data class SelectSahurAlarmSound(val soundOption: SoundOption) : SoundSettingsIntent
}

sealed interface SoundSettingsUiEffect {
    data class ShowToast(val message: String) : SoundSettingsUiEffect
}
