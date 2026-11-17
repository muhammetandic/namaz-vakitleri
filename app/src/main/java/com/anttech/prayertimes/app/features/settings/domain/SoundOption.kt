package com.anttech.prayertimes.features.settings.domain

import com.anttech.prayertimes.features.times.domain.PrayerType

sealed interface SoundOption {
    val id: String
    val title: String

    data class BuiltIn(
        override val id: String,
        override val title: String,
        val resName: String
    ) : SoundOption

    data class CustomFile(
        override val id: String,
        override val title: String,
        val uriString: String
    ) : SoundOption

    object Silent : SoundOption {
        override val id: String = "silent"
        override val title: String = "Sessiz"
    }

    /** Ses çalınmaz, vakit girdiğinde yalnızca titreşimli metin bildirimi gelir. */
    object VibrateOnly : SoundOption {
        override val id: String = "vibrate_only"
        override val title: String = "Sessiz / Yalnızca Titreşim"
    }

    companion object {
        val EZAN_TURKIYE = BuiltIn("ezan_turkiye", "Ezan (Türkiye)", "ezan1")
        val EZAN_ANVER_DUMAN = BuiltIn("ezan_anver_duman", "Ezan - Anver Duman (Türkiye)", "ezan2")
        val EZAN_AHMED_HADDAD = BuiltIn("ezan_ahmed_haddad", "Ezan - Ahmed el-Haddad", "ezan3")
        val EZAN_MUSTAFA_ISMAIL = BuiltIn("ezan_mustafa_ismail", "Ezan - Mustafa İsmail (Mısır)", "ezan4")

        val DEFAULT_BUILT_IN_SOUNDS = listOf(
            EZAN_TURKIYE,
            EZAN_ANVER_DUMAN,
            EZAN_AHMED_HADDAD,
            EZAN_MUSTAFA_ISMAIL,
            BuiltIn("ney_taksim", "Ney Taksimi (Gerçek Ney)", "melody_ney"),
            BuiltIn("kanun_taksim", "Kanun Taksimi (Gerçek Kanun)", "melody_kanun_harp"),
            BuiltIn("nature_birds", "Sabah Kuşları & Uyanış", "melody_nature_birds"),
            BuiltIn("crystal_alarm", "Kristal Vakit Melodisi", "tone_crystal_alarm"),
            BuiltIn("bip_tonu", "Kısa Vakit Uyarısı (Bip)", "tone_beep"),
            Silent,
            VibrateOnly
        )

        fun defaultFor(prayerType: PrayerType): SoundOption = EZAN_ANVER_DUMAN
    }
}

/**
 * Alarm bildirimi için çalınacak ses URI'si. Sessiz seçeneklerde null döner.
 */
fun SoundOption.alarmSoundUri(packageName: String): String? = when (this) {
    is SoundOption.BuiltIn -> "android.resource://$packageName/raw/$resName"
    is SoundOption.CustomFile -> uriString
    is SoundOption.Silent, SoundOption.VibrateOnly -> null
}
