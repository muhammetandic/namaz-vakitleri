package com.anttech.prayertimes.core.notifications

import android.content.Context
import android.media.AudioManager
import com.anttech.prayertimes.features.settings.domain.SoundOption

/** Vakit uyarısının nasıl iletileceği. */
enum class PrayerAlertMode {
    /** Telefon sesli: yalnızca ses çalınır, metin bildirimi gönderilmez. */
    SOUND_ONLY,

    /** Telefon sessiz/titreşimde veya ses kapatılmış: yalnızca metin bildirimi. */
    TEXT_ONLY
}

/**
 * Zil moduna ve seçilen sese göre uyarı biçimini belirler.
 * - RINGER_MODE_NORMAL + ses seçili  -> SOUND_ONLY
 * - RINGER_MODE_SILENT / VIBRATE     -> TEXT_ONLY
 * - Ses "Sessiz" seçilmiş            -> TEXT_ONLY (her modda)
 */
class PrayerAlertPolicy(
    private val ringerModeProvider: () -> Int
) {

    constructor(context: Context) : this({
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    })

    fun modeFor(soundOption: SoundOption): PrayerAlertMode = when {
        soundOption is SoundOption.Silent || soundOption is SoundOption.VibrateOnly -> PrayerAlertMode.TEXT_ONLY
        ringerModeProvider() == AudioManager.RINGER_MODE_NORMAL -> PrayerAlertMode.SOUND_ONLY
        else -> PrayerAlertMode.TEXT_ONLY
    }

    /**
     * Metin bildiriminde titreşim olup olmayacağını belirler.
     * - "Sessiz / Yalnızca Titreşim" seçiliyse her zaman titreşir.
     * - "Sessiz" seçiliyse hiç titreşmez.
     * - Diğer seslerde Ayarlar > Bildirimler > Titreşim tercihi geçerlidir.
     */
    fun vibrationEnabledFor(soundOption: SoundOption, preferenceEnabled: Boolean): Boolean =
        when (soundOption) {
            is SoundOption.VibrateOnly -> true
            is SoundOption.Silent -> false
            else -> preferenceEnabled
        }
}
