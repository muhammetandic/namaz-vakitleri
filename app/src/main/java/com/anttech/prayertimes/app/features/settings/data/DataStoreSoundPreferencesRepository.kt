package com.anttech.prayertimes.features.settings.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anttech.prayertimes.features.settings.domain.SoundOption
import com.anttech.prayertimes.features.settings.domain.SoundPreferencesRepository
import com.anttech.prayertimes.features.times.domain.PrayerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

private val Context.soundDataStore by preferencesDataStore(
    name = "sound_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class DataStoreSoundPreferencesRepository(
    private val context: Context
) : SoundPreferencesRepository {

    private val safeData: Flow<Preferences> by lazy {
        context.soundDataStore.data.catch { throwable ->
            // Bozuk dosya: varsayılanlara düş, uygulamayı kilitleme.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
    }

    override fun getSoundForPrayer(prayerType: PrayerType): Flow<SoundOption> {
        val key = stringPreferencesKey("sound_${prayerType.name}")
        return safeData.map { preferences ->
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

    override val globalVolume: Flow<Float> = safeData.map { preferences ->
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
        return safeData.map { preferences ->
            preferences[key] ?: 0
        }
    }

    override suspend fun saveDelayMinutesForPrayer(prayerType: PrayerType, minutes: Int) {
        context.soundDataStore.edit { preferences ->
            preferences[intPreferencesKey("delay_${prayerType.name}")] = minutes.coerceAtLeast(0)
        }
    }

    override fun getSahurAlarmSound(): Flow<SoundOption?> = safeData.map { preferences ->
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

    internal fun serializeSound(soundOption: SoundOption): String {
        // Alanlar Base64 ile kodlanır; başlık ya da URI içindeki ayırıcılar
        // kaydı bozamaz. "v2" öneki eski kayıtlardan ayırt eder.
        return when (soundOption) {
            is SoundOption.BuiltIn -> listOf(
                "v2", "BUILTIN",
                encode(soundOption.id), encode(soundOption.title), encode(soundOption.resName)
            ).joinToString("|")
            is SoundOption.CustomFile -> listOf(
                "v2", "CUSTOM",
                encode(soundOption.id), encode(soundOption.title), encode(soundOption.uriString)
            ).joinToString("|")
            is SoundOption.Silent -> listOf(
                "v2", "SILENT", encode(soundOption.id), encode(soundOption.title)
            ).joinToString("|")
            is SoundOption.VibrateOnly -> listOf(
                "v2", "VIBRATE_ONLY", encode(soundOption.id), encode(soundOption.title)
            ).joinToString("|")
        }
    }

    internal fun deserializeSound(serialized: String): SoundOption {
        if (serialized.startsWith("v2|")) {
            val parts = serialized.split("|")
            return when (parts.getOrNull(1)) {
                "BUILTIN" -> if (parts.size >= 5) {
                    SoundOption.BuiltIn(decode(parts[2]), decode(parts[3]), decode(parts[4]))
                } else {
                    SoundOption.Silent
                }
                "CUSTOM" -> if (parts.size >= 5) {
                    SoundOption.CustomFile(decode(parts[2]), decode(parts[3]), decode(parts[4]))
                } else {
                    SoundOption.Silent
                }
                "SILENT" -> SoundOption.Silent
                "VIBRATE_ONLY" -> SoundOption.VibrateOnly
                else -> SoundOption.Silent
            }
        }
        // Eski (v1) kayıt biçimi: geriye dönük okunur.
        val parts = serialized.split("|")
        if (parts.size < 3) return SoundOption.Silent
        return when (parts[0]) {
            "BUILTIN" -> SoundOption.BuiltIn(parts[1], parts[2], parts.getOrElse(3) { "" })
            "CUSTOM" -> SoundOption.CustomFile(parts[1], parts[2], parts.getOrElse(3) { parts[1] })
            "VIBRATE_ONLY" -> SoundOption.VibrateOnly
            else -> SoundOption.Silent
        }
    }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(
        value.toByteArray(StandardCharsets.UTF_8)
    )

    private fun decode(value: String): String = String(
        Base64.getDecoder().decode(value),
        StandardCharsets.UTF_8
    )

    private fun defaultSoundFor(prayerType: PrayerType): SoundOption {
        return SoundOption.defaultFor(prayerType)
    }

    private companion object {
        val GLOBAL_VOLUME_KEY = intPreferencesKey("global_volume")
        val SAHUR_ALARM_SOUND_KEY = stringPreferencesKey("sahur_alarm_sound")
    }
}
