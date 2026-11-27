package com.anttech.prayertimes.features.qibla.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.core.sensor.CompassSensorManager
import com.anttech.prayertimes.features.qibla.domain.CalculateQiblaAngleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QiblaViewModel @Inject constructor(
    private val compassSensorManager: CompassSensorManager,
    private val calculateQiblaAngleUseCase: CalculateQiblaAngleUseCase
) : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    private var sensorJob: Job? = null

    fun sendIntent(intent: QiblaIntent) {
        when (intent) {
            is QiblaIntent.StartCompass -> startCompass(intent.latitude, intent.longitude, intent.cityName)
            is QiblaIntent.StopCompass -> stopCompass()
            is QiblaIntent.OnAzimuthChanged -> updateAzimuth(intent.azimuth)
            is QiblaIntent.SetDisplayMode -> setDisplayMode(intent.mode)
        }
    }

    private fun setDisplayMode(mode: QiblaDisplayMode) {
        _uiState.update { it.copy(displayMode = mode) }
    }

    private fun startCompass(latitude: Double, longitude: Double, cityName: String) {
        val qiblaBearing = calculateQiblaAngleUseCase.calculateQiblaBearing(latitude, longitude)
        val sensorAvailable = compassSensorManager.isSensorAvailable()
        val needleAngle = if (!sensorAvailable) qiblaBearing else calculateQiblaAngleUseCase.calculateNeedleAngle(qiblaBearing, 0f)
        val initialMode = if (!sensorAvailable) QiblaDisplayMode.MAP else _uiState.value.displayMode

        _uiState.update {
            it.copy(
                isLoading = false,
                userLatitude = latitude,
                userLongitude = longitude,
                cityName = cityName,
                qiblaBearing = qiblaBearing,
                needleAngle = needleAngle,
                isSensorAvailable = sensorAvailable,
                displayMode = initialMode
            )
        }

        if (!sensorAvailable) return

        sensorJob?.cancel()
        sensorJob = viewModelScope.launch(dispatcher) {
            compassSensorManager.getAzimuthFlow().collect { azimuth ->
                updateAzimuth(azimuth)
            }
        }
    }

    private fun updateAzimuth(azimuth: Float) {
        val qiblaBearing = _uiState.value.qiblaBearing
        val needleAngle = calculateQiblaAngleUseCase.calculateNeedleAngle(qiblaBearing, azimuth)
        val isAligned = calculateQiblaAngleUseCase.isAlignedWithQibla(qiblaBearing, azimuth)

        _uiState.update {
            it.copy(
                currentAzimuth = azimuth,
                needleAngle = needleAngle,
                isAligned = isAligned
            )
        }
    }

    private fun stopCompass() {
        sensorJob?.cancel()
        sensorJob = null
    }

    override fun onCleared() {
        stopCompass()
    }
}
