package com.anttech.prayertimes.core.notifications

import com.anttech.prayertimes.R

/**
 * Yerleşik ses adlarını derleme zamanında çözülen `R.raw` kimliklerine eşler.
 *
 * Ad → kaynak eşlemesi açıkça yazıldığı için R8 sonrası da çalışır; yansıma
 * (`Resources.getIdentifier`) kullanılmaz. Bilinmeyen adlar için 0 döner.
 */
fun rawSoundResId(resName: String): Int = when (resName) {
    "ezan1" -> R.raw.ezan1
    "ezan2" -> R.raw.ezan2
    "ezan3" -> R.raw.ezan3
    "ezan4" -> R.raw.ezan4
    "melody_kanun_harp" -> R.raw.melody_kanun_harp
    "melody_nature_birds" -> R.raw.melody_nature_birds
    "melody_ney" -> R.raw.melody_ney
    "tone_beep" -> R.raw.tone_beep
    "tone_crystal_alarm" -> R.raw.tone_crystal_alarm
    else -> 0
}
