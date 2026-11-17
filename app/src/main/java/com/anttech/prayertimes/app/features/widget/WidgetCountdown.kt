package com.anttech.prayertimes.features.widget

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Büyük widget'taki geri sayımın hedefini hesaplar.
 *
 * Sıradaki vakit bugün geçtiyse hedef yarına taşınır (gece yatsıdan sonra
 * yarınki imsak gibi). Chronometer bu milisaniye ile kurulur; sistem saatiyle
 * aynı saniyede akar.
 */
object WidgetCountdown {

    fun remainingMillis(now: LocalDateTime, nextPrayerTime: LocalTime): Long {
        var target = now.toLocalDate().atTime(nextPrayerTime)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }
}
