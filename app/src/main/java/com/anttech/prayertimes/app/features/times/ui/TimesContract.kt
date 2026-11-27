package com.anttech.prayertimes.features.times.ui

import com.anttech.prayertimes.features.times.domain.CountdownResult
import com.anttech.prayertimes.features.times.domain.KerahaResult
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.PrayerType
import java.time.LocalDate

data class TimesUiState(
    val isLoading: Boolean = true,
    val city: String = "İstanbul",
    val latitude: Double = 41.0082,
    val longitude: Double = 28.9784,
    val date: LocalDate = LocalDate.now(),
    val prayerTimes: PrayerTimes? = null,
    val countdown: CountdownResult? = null,
    val keraha: KerahaResult? = null,
    val fontSizeMultiplier: Float = 1.0f,
    val isSimpleMode: Boolean = false,
    val errorMessage: String? = null
)

sealed interface TimesIntent {
    data class LoadTimes(val city: String) : TimesIntent
    data class ChangeCity(val city: String) : TimesIntent
    object UseLocation : TimesIntent
    object TickSecond : TimesIntent
    object ToggleSimpleMode : TimesIntent
    data class SetFontScale(val scale: Float) : TimesIntent
    object RefreshLocation : TimesIntent
}

sealed interface TimesUiEffect {
    data class ShowToast(val message: String) : TimesUiEffect
}
