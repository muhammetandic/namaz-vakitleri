package com.anttech.prayertimes.core.location

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Kullanıcının elle seçtiği şehir ve ilçe. Boşsa uygulama konumu otomatik kullanır.
 * Format: "Şehir, İlçe" (örn: "Ankara, Çankaya")
 */
interface SelectedCityStore {
    suspend fun saveCity(city: String, district: String? = null)
    suspend fun getCity(): String?
    suspend fun clearCity()
}

private val Context.cityDataStore by preferencesDataStore(name = "selected_city_preferences")

class DataStoreSelectedCityStore(
    private val context: Context
) : SelectedCityStore {

    override suspend fun saveCity(city: String, district: String?) {
        val cityWithDistrict = if (!district.isNullOrBlank() && district != city) {
            "$city, $district"
        } else {
            city
        }
        context.cityDataStore.edit { preferences ->
            preferences[CITY_KEY] = cityWithDistrict
        }
    }

    override suspend fun getCity(): String? {
        return context.cityDataStore.data.first()[CITY_KEY]
    }

    override suspend fun clearCity() {
        context.cityDataStore.edit { preferences ->
            preferences.remove(CITY_KEY)
        }
    }

    private companion object {
        val CITY_KEY = stringPreferencesKey("selected_city")
    }
}
