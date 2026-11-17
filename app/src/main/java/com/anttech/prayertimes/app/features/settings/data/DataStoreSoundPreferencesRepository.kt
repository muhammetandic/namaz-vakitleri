package com.anttech.prayertimes.features.settings.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.soundDataStore by preferencesDataStore(name = "sound_preferences")

class DataStoreSoundPreferencesRepository(
    private val context: Context
) : SoundPreferencesRepository {

    override fun getSoundForPrayer(prayerType: PrayerType): Flow<SoundOption> {
        val key = stringPreferencesKey("sound_${prayerType.name}")
        return context.soundDataStore.data.map { preferences ->
            val serialized = preferences[key]
            if (serialized != null) {
                deserializeSound(serialized)
            } else {
                defaultSoundFor(prayerType)
            }
        }
    }

    override suspend fun saveSoundForPrayer(prayerType: PrayerType, soundOption: SoundOption) {
        val key = stringPreferencesKey("sound_${prayerType.name}")
        val serialized = serializeSound(soundOption)
        context.soundDataStore.edit { preferences ->
            preferences[key] = serialized
        }
    }

    override val globalVolume: Flow<Float> = context.soundDataStore.data.map { preferences ->
        val percent = preferences[GLOBAL_VOLUME_KEY] ?: (SoundPreferencesRepository.DEFAULT_VOLUME * 100).toInt()
        (percent / 100f).coerceIn(0f, 1f)
    }

    override suspend fun setGlobalVolume(volume: Float) {
        context.soundDataStore.edit { preferences ->
            preferences[GLOBAL_VOLUME_KEY] = (volume.coerceIn(0f, 1f) * 100).toInt()
        }
    }

    override fun getDelayMinutesForPrayer(prayerType: PrayerType): Flow<Int> {
        val key = intPreferencesKey("delay_${prayerType.name}")
        return context.soundDataStore.data.map { preferences ->
            preferences[key] ?: 0
        }
    }

    override suspend fun saveDelayMinutesForPrayer(prayerType: PrayerType, minutes: Int) {
        context.soundDataStore.edit { preferences ->
            preferences[intPreferencesKey("delay_${prayerType.name}")] = minutes.coerceAtLeast(0)
        }
    }

    override fun getSahurAlarmSound(): Flow<SoundOption?> = context.soundDataStore.data.map { preferences ->
        preferences[SAHUR_ALARM_SOUND_KEY]?.let { deserializeSound(it) }
    }

    override suspend fun saveSahurAlarmSound(soundOption: SoundOption?) {
        context.soundDataStore.edit { preferences ->
            if (soundOption == null) {
                preferences.remove(SAHUR_ALARM_SOUND_KEY)
            } else {
                preferences[SAHUR_ALARM_SOUND_KEY] = serializeSound(soundOption)
            }
        }
    }

    private fun serializeSound(soundOption: SoundOption): String {
        return when (soundOption) {
            is SoundOption.BuiltIn -> "BUILTIN|${soundOption.id}|${soundOption.title}|${soundOption.resName}"
            is SoundOption.CustomFile -> "CUSTOM|${soundOption.id}|${soundOption.title}|${soundOption.uriString}"
            is SoundOption.Silent -> "SILENT|silent|Sessiz"
            is SoundOption.VibrateOnly -> "VIBRATE_ONLY|vibrate_only|Sessiz / Yalnızca Titreşim"
        }
    }

    internal fun deserializeSound(serialized: String): SoundOption {
        val parts = serialized.split("|")
        if (parts.size < 3) return SoundOption.Silent
        return when (parts[0]) {
            "BUILTIN" -> SoundOption.BuiltIn(parts[1], parts[2], parts.getOrElse(3) { "" })
            "CUSTOM" -> SoundOption.CustomFile(parts[1], parts[2], parts.getOrElse(3) { parts[1] })
            "VIBRATE_ONLY" -> SoundOption.VibrateOnly
            else -> SoundOption.Silent
        }
    }

    private fun defaultSoundFor(prayerType: PrayerType): SoundOption {
        return SoundOption.defaultFor(prayerType)
    }

    private companion object {
        val GLOBAL_VOLUME_KEY = intPreferencesKey("global_volume")
        val SAHUR_ALARM_SOUND_KEY = stringPreferencesKey("sahur_alarm_sound")
    }
}
