package com.anttech.prayertimes.features.calendar.ui

import com.anttech.prayertimes.features.calendar.domain.FastTrackingDay
import com.anttech.prayertimes.features.calendar.domain.ImsakiyeDay
import java.time.LocalDate

data class ImsakiyeUiState(
    val isLoading: Boolean = true,
    val city: String = "İstanbul",
    val yearMonth: String = "",
    val days: List<ImsakiyeDay> = emptyList(),
    val todayImsakiye: ImsakiyeDay? = null,
    val fontSizeMultiplier: Float = 1.0f,
    // Fasting tracking card data
    val nonFastedRamadanDays: List<FastTrackingDay> = emptyList(),
    val fastedNonRamadanDays: List<FastTrackingDay> = emptyList(),
    val forbiddenDays: List<FastTrackingDay> = emptyList()
)

sealed interface ImsakiyeIntent {
    data class LoadImsakiye(
        val city: String,
        val yearMonth: String,
        val force: Boolean = false
    ) : ImsakiyeIntent
    data class ToggleFastedDay(
        val date: LocalDate,
        val hijriDay: Int,
        val hijriMonth: Int,
        val hijriMonthName: String,
        val hijriYear: Int
    ) : ImsakiyeIntent
    object ShareTodayImsakiye : ImsakiyeIntent
}

sealed interface ImsakiyeUiEffect {
    data class ShareText(val content: String) : ImsakiyeUiEffect
    data class ShowMessage(val message: String) : ImsakiyeUiEffect
}
