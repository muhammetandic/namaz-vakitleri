package com.anttech.prayertimes.features.qibla.ui

enum class QiblaDisplayMode {
    COMPASS,
    MAP
}

data class QiblaUiState(
    val isLoading: Boolean = true,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val cityName: String = "",
    val qiblaBearing: Float = 0f,
    val currentAzimuth: Float = 0f,
    val needleAngle: Float = 0f,
    val isAligned: Boolean = false,
    val isSensorAvailable: Boolean = true,
    val displayMode: QiblaDisplayMode = QiblaDisplayMode.COMPASS,
    val errorMessage: String? = null
)

sealed interface QiblaIntent {
    data class StartCompass(val latitude: Double, val longitude: Double, val cityName: String) : QiblaIntent
    object StopCompass : QiblaIntent
    data class OnAzimuthChanged(val azimuth: Float) : QiblaIntent
    data class SetDisplayMode(val mode: QiblaDisplayMode) : QiblaIntent
}
