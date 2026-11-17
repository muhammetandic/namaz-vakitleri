package com.anttech.prayertimes.features.times.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anttech.prayertimes.core.location.GeocoderHelper
import com.anttech.prayertimes.core.location.LocationTracker
import com.anttech.prayertimes.core.location.SelectedCityStore
import com.anttech.prayertimes.core.notifications.PrayerAlarmScheduler
import com.anttech.prayertimes.features.times.domain.CalculateKerahaStatusUseCase
import com.anttech.prayertimes.features.times.domain.CalculateNextPrayerUseCase
import com.anttech.prayertimes.features.times.domain.TimesRepository
import com.anttech.prayertimes.features.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main

    private val _uiState = MutableStateFlow(TimesUiState())
    val uiState: StateFlow<TimesUiState> = _uiState.asStateFlow()

    private val _effect = Channel<TimesUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun sendIntent(intent: TimesIntent) {
        when (intent) {
            is TimesIntent.LoadTimes -> loadPrayerTimes(intent.city)
            is TimesIntent.ChangeCity -> changeCity(intent.city)
            is TimesIntent.UseLocation -> useLocation()
            is TimesIntent.TickSecond -> updateCountdownAndKeraha()
            is TimesIntent.ToggleSimpleMode -> toggleSimpleMode()
            is TimesIntent.RefreshLocation -> refreshLocation()
        }
    }

    private fun changeCity(city: String) {
        viewModelScope.launch(dispatcher) {
            // Extract city name and district from "City, District" format
            val (cityName, district) = parseCityString(city)
            selectedCityStore.saveCity(cityName, district)
            // Elle seçilen şehir için kıble koordinatlarını güncelle
            updateCoordinatesForCity(cityName, district)
            // Use full city string (may include district) for API call
            loadPrayerTimes(city)
        }
    }

    /** İleri geocode ile şehrin koordinatlarını bulur; bulamazsa mevcut konumu korur. */
    private fun updateCoordinatesForCity(cityName: String, district: String?) {
        val coordinates = geocoderHelper.getCoordinatesForCity(cityName, district) ?: return
        _uiState.update {
            it.copy(latitude = coordinates.first, longitude = coordinates.second)
        }
    }

    private fun parseCityString(cityString: String): Pair<String, String?> {
        return if (cityString.contains(",")) {
            val parts = cityString.split(",")
            val city = parts[0].trim()
            val district = if (parts.size > 1) parts[1].trim() else null
            city to district
        } else {
            cityString to null
        }
    }

    private fun useLocation() {
        viewModelScope.launch(dispatcher) {
            selectedCityStore.clearCity()
            refreshLocation()
        }
    }

    private fun refreshLocation() {
        viewModelScope.launch(dispatcher) {
            val savedCity = selectedCityStore.getCity()
            if (savedCity != null) {
                // Kayıtlı elle seçilmiş şehir: GPS'e gitmeden vakitleri ve koordinatları yükle
                val (cityName, district) = parseCityString(savedCity)
                updateCoordinatesForCity(cityName, district)
                loadPrayerTimes(savedCity)
                return@launch
            }
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                _uiState.update {
                    it.copy(
                        city = location.displayName,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
                // Sync using full city string (may include district)
                repository.syncMonthlyPrayerTimes(location.displayName)
                loadPrayerTimes(location.displayName)
            } else {
                _effect.send(TimesUiEffect.ShowToast("Konum alınamadı. Varsayılan şehir kullanılıyor."))
                if (_uiState.value.prayerTimes == null) {
                    loadPrayerTimes("İstanbul")
                }
            }
        }
    }

    private fun loadPrayerTimes(city: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoading = true, city = city) }
            val today = LocalDate.now()
            // Use full city string (may include district) for API call
            val queryCity = city
            
            // First try to sync from API with full city string
            repository.syncMonthlyPrayerTimes(queryCity)
            
            // Then load from DB with timeout
            val times: com.anttech.prayertimes.features.times.domain.PrayerTimes? = withTimeoutOrNull(5000L) {
                repository.getPrayerTimesForDate(city, today).firstOrNull()
                    ?: repository.getPrayerTimesForDate(city, today).firstOrNull()
            }
            
            if (times != null) {
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
                prayerAlarmScheduler.scheduleUpcoming(queryCity)
                // Ana ekran widget'ları da yeni veriyle güncellensin.
                widgetUpdater.updateAll()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Vakitler bulunamadı. Lütfen internetinizi kontrol edin."
                    )
                }
            }
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
        _uiState.update { it.copy(isSimpleMode = !it.isSimpleMode) }
    }
}
