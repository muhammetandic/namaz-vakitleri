package com.anttech.prayertimes.features.tracker.ui

import com.anttech.prayertimes.features.tracker.domain.DhikrPreset

data class TrackerUiState(
    // Dhikr State
    val dhikrCount: Int = 0,
    val dhikrLaps: Int = 0,
    val selectedPreset: DhikrPreset = DhikrPreset.DEFAULT_PRESETS[0],
    val availablePresets: List<DhikrPreset> = DhikrPreset.DEFAULT_PRESETS,
    val fontSizeMultiplier: Float = 1.0f
)

sealed interface TrackerIntent {
    // Dhikr Intents
    object IncrementDhikr : TrackerIntent
    object ResetDhikr : TrackerIntent
    data class SelectPreset(val preset: DhikrPreset) : TrackerIntent
}

sealed interface TrackerUiEffect {
    object TriggerTapVibration : TrackerUiEffect
    object TriggerTargetVibration : TrackerUiEffect
    data class ShowToast(val message: String) : TrackerUiEffect
}
