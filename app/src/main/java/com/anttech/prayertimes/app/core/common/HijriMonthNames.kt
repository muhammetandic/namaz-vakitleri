package com.anttech.prayertimes.core.common

/** Hicri ay numarasının (1..12) Türkçe adı. Bilinmeyen değerlerde boş dize döner. */
fun hijriMonthName(month: Int): String = when (month) {
    1 -> "Muharrem"
    2 -> "Safer"
    3 -> "Rebiülevvel"
    4 -> "Rebiülahir"
    5 -> "Cemaziyelevvel"
    6 -> "Cemaziyelahir"
    7 -> "Recep"
    8 -> "Şaban"
    9 -> "Ramazan"
    10 -> "Şevval"
    11 -> "Zilkade"
    12 -> "Zilhicce"
    else -> ""
}
