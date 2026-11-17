package com.anttech.prayertimes.features.settings.domain

import com.anttech.prayertimes.features.times.domain.PrayerType
import kotlinx.coroutines.flow.Flow

interface SoundPreferencesRepository {
    fun getSoundForPrayer(prayerType: PrayerType): Flow<SoundOption>
    suspend fun saveSoundForPrayer(prayerType: PrayerType, soundOption: SoundOption)

    /** Uygulama içi genel ses seviyesi (0f..1f). Sistem ses seviyesinden bağımsızdır. */
    val globalVolume: Flow<Float>
    suspend fun setGlobalVolume(volume: Float)

    /** Vakit girdikten kaç dakika sonra ses çalınsın. 0 = hemen. */
    fun getDelayMinutesForPrayer(prayerType: PrayerType): Flow<Int>
    suspend fun saveDelayMinutesForPrayer(prayerType: PrayerType, minutes: Int)

    /**
     * Sahur uyandırma alarmı için seçilen ses.
     * null ise imsak vakti sesi kullanılır.
     */
    fun getSahurAlarmSound(): Flow<SoundOption?>
    suspend fun saveSahurAlarmSound(soundOption: SoundOption?)

    companion object {
        const val DEFAULT_VOLUME = 0.8f
        val DELAY_OPTIONS_MINUTES = listOf(0, 1, 2, 5, 10, 15, 30)
    }
}

interface AudioPlayerManager {
    /**
     * Sesi [volume] seviyesiyle çalar.
     * [onFinished] çalma bittiğinde veya hata verdiğinde main thread'de çağrılır;
     * [stop] ile kullanıcı durdurursa çağrılmaz.
     */
    fun play(
        soundOption: SoundOption,
        volume: Float = SoundPreferencesRepository.DEFAULT_VOLUME,
        onFinished: () -> Unit = {}
    )

    /** Çalan sesin seviyesini anında günceller (önizleme sırasında kaydırıcı). */
    fun setVolume(volume: Float)

    fun stop()
    fun isPlaying(): Boolean
}
