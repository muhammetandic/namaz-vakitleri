package com.anttech.prayertimes.features.times.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.core.common.IoDispatcher
import com.anttech.prayertimes.core.location.GeocoderHelper
import com.anttech.prayertimes.core.location.LocationTracker
import com.anttech.prayertimes.core.location.SelectedCityStore
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.features.times.domain.CalculateKerahaStatusUseCase
import com.anttech.prayertimes.features.times.domain.CalculateNextPrayerUseCase
import com.anttech.prayertimes.features.times.domain.PrayerTimes
import com.anttech.prayertimes.features.settings.domain.DisplayPreferencesRepository
import com.anttech.prayertimes.features.times.domain.TimesRepository
import com.anttech.prayertimes.features.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TimesViewModel @Inject constructor(
    private val repository: TimesRepository,
    private val calculateNextPrayerUseCase: CalculateNextPrayerUseCase,
    private val calculateKerahaStatusUseCase: CalculateKerahaStatusUseCase,
    private val locationTracker: LocationTracker,
    private val prayerAlarmScheduler: PrayerAlarmScheduler,
    private val selectedCityStore: SelectedCityStore,
    private val geocoderHelper: GeocoderHelper,
    private val widgetUpdater: WidgetUpdater,
    private val displayPreferences: DisplayPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimesUiState())
    val uiState: StateFlow<TimesUiState> = _uiState.asStateFlow()

    private val _effect = Channel<TimesUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        // Görünüm tercihleri kalıcıdır; ilk değer olarak yayınlanır.
        viewModelScope.launch {
            displayPreferences.fontSizeMultiplier.collect { multiplier ->
                _uiState.update { state ->
                    if (state.fontSizeMultiplier == multiplier) {
                        state
                    } else {
                        state.copy(fontSizeMultiplier = multiplier)
                    }
                }
            }
        }
        viewModelScope.launch {
            displayPreferences.isSimpleMode.collect { enabled ->
                _uiState.update { state ->
                    if (state.isSimpleMode == enabled) state else state.copy(isSimpleMode = enabled)
                }
            }
        }
    }

    fun sendIntent(intent: TimesIntent) {
        when (intent) {
            is TimesIntent.LoadTimes -> loadPrayerTimes(intent.city)
            is TimesIntent.ChangeCity -> changeCity(intent.city)
            is TimesIntent.UseLocation -> useLocation()
            is TimesIntent.TickSecond -> updateCountdownAndKeraha()
            is TimesIntent.ToggleSimpleMode -> toggleSimpleMode()
            is TimesIntent.SetFontScale -> setFontScale(intent.scale)
            is TimesIntent.RefreshLocation -> refreshLocation()
        }
    }

    private fun changeCity(city: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val (cityName, district) = parseCityString(city)
            selectedCityStore.saveCity(cityName, district)
            updateCoordinatesForCity(cityName, district)
            loadPrayerTimesInternal(city)
        }
    }

    /** İleri geocode ile şehrin koordinatlarını bulur; bulamazsa mevcut konumu korur. */
    private suspend fun updateCoordinatesForCity(cityName: String, district: String?) {
        val coordinates = withContext(ioDispatcher) {
            geocoderHelper.getCoordinatesForCity(cityName, district)
        } ?: return
        _uiState.update {
            it.copy(latitude = coordinates.first, longitude = coordinates.second)
        }
    }

    private fun parseCityString(cityString: String): Pair<String, String?> {
        val parts = cityString.split(",", limit = 2)
        val city = parts[0].trim()
        val district = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        return city to district
    }

    private fun useLocation() {
        viewModelScope.launch {
            selectedCityStore.clearCity()
            refreshLocation()
        }
    }

    private fun refreshLocation() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val savedCity = selectedCityStore.getCity()
            if (savedCity != null) {
                // Kayıtlı elle seçilmiş şehir: GPS'e gitmeden vakitleri ve koordinatları yükle.
                val (cityName, district) = parseCityString(savedCity)
                updateCoordinatesForCity(cityName, district)
                loadPrayerTimesInternal(savedCity)
                return@launch
            }

            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                locationTracker.getCurrentLocation()
            }
            if (location != null) {
                _uiState.update {
                    it.copy(
                        city = location.displayName,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
                loadPrayerTimesInternal(location.displayName)
            } else {
                _effect.send(TimesUiEffect.ShowToast("Konum alınamadı. Varsayılan şehir kullanılıyor."))
                if (_uiState.value.prayerTimes == null) {
                    loadPrayerTimesInternal(DEFAULT_CITY)
                }
            }
        }
    }

    private fun loadPrayerTimes(city: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadPrayerTimesInternal(city)
        }
    }

    /**
     * Offline-first yükleme: önce Room'daki önbellek gösterilir, ağ senkronizasyonu
     * arka planda yapılır. Ağ yoksa mevcut önbellek ekranda kalır.
     */
    private suspend fun loadPrayerTimesInternal(city: String) {
        val today = LocalDate.now()
        if (_uiState.value.city != city) {
            // Farklı şehre geçildi: eski şehrin vakitlerini göstermeye devam etme.
            _uiState.update {
                it.copy(prayerTimes = null, countdown = null, keraha = null)
            }
        }
        _uiState.update {
            it.copy(
                city = city,
                isLoading = it.prayerTimes == null,
                errorMessage = null
            )
        }

        val cached = withTimeoutOrNull(DB_TIMEOUT_MS) {
            repository.getPrayerTimesForDate(city, today).firstOrNull()
        }
        if (cached != null) {
            applyPrayerTimes(cached)
        }

        withContext(ioDispatcher) {
            repository.syncMonthlyPrayerTimes(city)
        }

        val fresh = withTimeoutOrNull(DB_TIMEOUT_MS) {
            repository.getPrayerTimesForDate(city, today).firstOrNull()
        }
        if (fresh != null) {
            applyPrayerTimes(fresh)
            prayerAlarmScheduler.scheduleUpcoming(city)
            widgetUpdater.updateAll()
        } else if (cached == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Vakitler bulunamadı. Lütfen internetinizi kontrol edin."
                )
            }
        }
    }

    private fun applyPrayerTimes(times: PrayerTimes) {
        val now = LocalDateTime.now()
        val countdown = calculateNextPrayerUseCase(times, now)
        val keraha = calculateKerahaStatusUseCase(times, now)
        _uiState.update {
            it.copy(
                isLoading = false,
                prayerTimes = times,
                countdown = countdown,
                keraha = keraha,
                errorMessage = null
            )
        }
    }

    private fun updateCountdownAndKeraha() {
        val currentTimes = _uiState.value.prayerTimes ?: return
        val now = LocalDateTime.now()
        val countdown = calculateNextPrayerUseCase(currentTimes, now)
        val keraha = calculateKerahaStatusUseCase(currentTimes, now)

        _uiState.update {
            it.copy(
                countdown = countdown,
                keraha = keraha
            )
        }
    }

    private fun toggleSimpleMode() {
        val enabled = !_uiState.value.isSimpleMode
        _uiState.update { it.copy(isSimpleMode = enabled) }
        viewModelScope.launch(ioDispatcher) {
            displayPreferences.setSimpleMode(enabled)
        }
    }

    private fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(
            DisplayPreferencesRepository.MIN_FONT_SCALE,
            DisplayPreferencesRepository.MAX_FONT_SCALE
        )
        _uiState.update { it.copy(fontSizeMultiplier = clamped) }
        viewModelScope.launch(ioDispatcher) {
            displayPreferences.setFontSizeMultiplier(clamped)
        }
    }

    private companion object {
        const val DEFAULT_CITY = "İstanbul"
        const val DB_TIMEOUT_MS = 5_000L
        const val LOCATION_TIMEOUT_MS = 15_000L
    }
}
