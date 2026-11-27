package com.anttech.prayertimes.features.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.core.ui.AppUiEffect
import com.anttech.prayertimes.core.ui.UiEffectsCoordinator
import com.anttech.prayertimes.features.calendar.domain.FastTrackingDay
import com.anttech.prayertimes.features.calendar.domain.FastTrackingRepository
import com.anttech.prayertimes.features.calendar.domain.ImsakiyeDay
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.times.domain.TimesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ImsakiyeViewModel @Inject constructor(
    private val repository: TimesRepository,
    private val fastTrackingRepository: FastTrackingRepository,
    private val uiEffects: UiEffectsCoordinator = UiEffectsCoordinator()
) : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main

    private val _uiState = MutableStateFlow(ImsakiyeUiState())
    val uiState: StateFlow<ImsakiyeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ImsakiyeUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private fun sendEffect(effect: ImsakiyeUiEffect) {
        _effect.trySend(effect)
        val appEffect = when (effect) {
            is ImsakiyeUiEffect.ShowMessage -> AppUiEffect.Toast(effect.message)
            is ImsakiyeUiEffect.ShareText -> AppUiEffect.ShareText(effect.content)
        }
        uiEffects.emit(appEffect)
    }

    private var loadJob: Job? = null
    private var trackingJob: Job? = null

    fun sendIntent(intent: ImsakiyeIntent) {
        when (intent) {
            is ImsakiyeIntent.LoadImsakiye -> loadImsakiye(intent.city, intent.yearMonth, intent.force)
            is ImsakiyeIntent.ToggleFastedDay -> toggleFastedDay(
                intent.date,
                intent.hijriDay,
                intent.hijriMonth,
                intent.hijriMonthName,
                intent.hijriYear
            )
            is ImsakiyeIntent.ShareTodayImsakiye -> shareTodayImsakiye()
        }
    }

    private var lastLoadedKey: Pair<String, String>? = null

    private fun loadImsakiye(city: String, yearMonth: String, force: Boolean = false) {
        val key = city to yearMonth
        if (!force && lastLoadedKey == key) return
        lastLoadedKey = key
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, city = city, yearMonth = yearMonth)

        // Load fasting tracking card data (all-time, not just current month)
        loadFastingTrackingData()

        loadJob = viewModelScope.launch(dispatcher) {
            val today = LocalDate.now()

            // Combine prayer times with fast tracking data
            val timesFlow = repository.getMonthlyPrayerTimes(city, yearMonth)
            val trackingFlow = fastTrackingRepository.getMonthlyTracking(city, yearMonth)

            combine(timesFlow, trackingFlow) { timesList, trackingList ->
                val trackingMap = trackingList.associateBy { it.date }
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                timesList.map { pt ->
                    val tracking = trackingMap[pt.date]
                    // Determine forbidden days
                    val (isForbidden, forbiddenReason) = when {
                        pt.hijriMonth == 10 && pt.hijriDay == 1 -> Pair(true, "Ramazan Bayramı (1. Şevval)")
                        pt.hijriMonth == 12 && pt.hijriDay == 10 -> Pair(true, "Kurban Bayramı (10. Zilhicce)")
                        pt.hijriMonth == 12 && pt.hijriDay in 11..13 -> Pair(true, "Teşrik Günleri (${pt.hijriDay}. Zilhicce)")
                        else -> Pair(false, "")
                    }
                    ImsakiyeDay(
                        date = pt.date,
                        imsakFormatted = pt.imsak.format(timeFormatter),
                        gunesFormatted = pt.gunes.format(timeFormatter),
                        ogleFormatted = pt.ogle.format(timeFormatter),
                        ikindiFormatted = pt.ikindi.format(timeFormatter),
                        aksamFormatted = pt.aksam.format(timeFormatter),
                        yatsiFormatted = pt.yatsi.format(timeFormatter),
                        isToday = pt.date == today,
                        isFasted = tracking?.isMarked == true,
                        hijriDay = pt.hijriDay,
                        hijriMonth = pt.hijriMonth,
                        hijriMonthName = pt.hijriMonthName,
                        hijriYear = pt.hijriYear,
                        isForbidden = isForbidden,
                        forbiddenReason = forbiddenReason
                    )
                }
            }.collect { mappedDays ->
                val todayItem = mappedDays.find { it.isToday } ?: mappedDays.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    days = mappedDays,
                    todayImsakiye = todayItem
                )
            }
        }
    }

    private fun loadFastingTrackingData() {
        val city = _uiState.value.city
        if (city.isBlank()) return

        trackingJob?.cancel()
        trackingJob = viewModelScope.launch(dispatcher) {
            val nonFastedRamadan = fastTrackingRepository.getNonFastedRamadanDays(city)
            val fastedNonRamadan = fastTrackingRepository.getFastedNonRamadanDays(city)
            val forbiddenDays = fastTrackingRepository.getForbiddenDays(city)

            combine(nonFastedRamadan, fastedNonRamadan, forbiddenDays) { ramadan, nonRamadan, forbidden ->
                _uiState.value = _uiState.value.copy(
                    nonFastedRamadanDays = ramadan,
                    fastedNonRamadanDays = nonRamadan,
                    forbiddenDays = forbidden
                )
            }.collect { }
        }
    }

    private fun toggleFastedDay(
        date: LocalDate,
        hijriDay: Int,
        hijriMonth: Int,
        hijriMonthName: String,
        hijriYear: Int
    ) {
        viewModelScope.launch(dispatcher) {
            val newMarked = fastTrackingRepository.toggleFastedDay(
                city = _uiState.value.city,
                date = date,
                hijriDay = hijriDay,
                hijriMonth = hijriMonth,
                hijriMonthName = hijriMonthName,
                hijriYear = hijriYear
            )

            val actionText = if (hijriMonth == 9) {
                if (newMarked) "Ramazan orucu tutulmadı olarak işaretlendi" else "Ramazan orucu işareti kaldırıldı"
            } else {
                if (newMarked) "Nafile oruç işaretlendi" else "Nafile oruç işareti kaldırıldı"
            }

            sendEffect(ImsakiyeUiEffect.ShowMessage(actionText))

            // Refresh the tracking card data (Room flow also refreshes todayImsakiye.isFasted)
            loadFastingTrackingData()
        }
    }

    private fun shareTodayImsakiye() {
        val today = _uiState.value.todayImsakiye ?: return
        val text = buildString {
            append("🕌 ${_uiState.value.city} İmsakiye Vakitleri\n")
            append("📅 Tarih: ${today.dayNumber} ${today.monthShortTurkish} ${today.hijriDay} ${today.hijriMonthName} ${today.hijriYear}\n")
            append("🌙 Sahur (İmsak): ${today.imsakFormatted}\n")
            append("☀️ Güneş: ${today.gunesFormatted}\n")
            append("🕐 Öğle: ${today.ogleFormatted}\n")
            append("🕒 İkindi: ${today.ikindiFormatted}\n")
            append("✨ İftar (Akşam): ${today.aksamFormatted}\n")
            append("🌟 Yatsı: ${today.yatsiFormatted}\n\n")
            append("Hayırlı ve bereketli günler dileriz.")
        }

        viewModelScope.launch(dispatcher) {
            sendEffect(ImsakiyeUiEffect.ShareText(text))
        }
    }
}
