package com.anttech.prayertimes.features.times.domain

data class CountdownResult(
    val activePrayer: PrayerType,
    val nextPrayer: PrayerType,
    val remainingSeconds: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val formattedCountdown: String,
    val progress: Float
)
