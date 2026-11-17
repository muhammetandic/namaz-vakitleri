package com.anttech.prayertimes.features.tracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.features.tracker.domain.DhikrPreset
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
class TrackerViewModel @Inject constructor() : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    private val _effect = Channel<TrackerUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun sendIntent(intent: TrackerIntent) {
        when (intent) {
            is TrackerIntent.IncrementDhikr -> incrementDhikr()
            is TrackerIntent.ResetDhikr -> resetDhikr()
            is TrackerIntent.SelectPreset -> selectPreset(intent.preset)
        }
    }

    private fun incrementDhikr() {
        val currentTarget = _uiState.value.selectedPreset.target
        val currentCount = _uiState.value.dhikrCount
        val currentLaps = _uiState.value.dhikrLaps

        if (currentTarget > 0 && currentCount + 1 >= currentTarget) {
            // Target reached (e.g. 33 or 100)
            _uiState.update {
                it.copy(
                    dhikrCount = 0,
                    dhikrLaps = currentLaps + 1
                )
            }
            viewModelScope.launch(dispatcher) {
                _effect.send(TrackerUiEffect.TriggerTargetVibration)
            }
        } else {
            _uiState.update { it.copy(dhikrCount = currentCount + 1) }
            viewModelScope.launch(dispatcher) {
                _effect.send(TrackerUiEffect.TriggerTapVibration)
            }
        }
    }

    private fun resetDhikr() {
        _uiState.update {
            it.copy(
                dhikrCount = 0,
                dhikrLaps = 0
            )
        }
    }

    private fun selectPreset(preset: DhikrPreset) {
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                dhikrCount = 0,
                dhikrLaps = 0
            )
        }
    }
}
